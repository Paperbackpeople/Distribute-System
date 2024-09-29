package Server;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import Client.GamerNode;
import Interface.GameNodeInterface;
import Message.GameState;
import Message.GossipMessage;
import Player.PlayerInfo;



public class PrimaryServer extends GamerNode {
    // 崩溃的玩家列表
    private List<PlayerInfo> crashedPlayers = new ArrayList<>();
    // 更新的玩家列表
    private List<PlayerInfo> updatedPlayers = new ArrayList<>();
    private int version = 0; // 版本号
    private PlayerInfo primaryNode;
    private GameState gameState;

    // 主节点的玩家列表
    private List<PlayerInfo> players = new ArrayList<>();

    public PrimaryServer(String trackerIp, int trackerPort, String playerId) {
        super(trackerIp, trackerPort, playerId);
    }
    private void sendReplyToSender(PlayerInfo senderInfo) {
        try {
            Registry registry = LocateRegistry.getRegistry(senderInfo.getIpAddress(), senderInfo.getPort());
            GameNodeInterface senderNode = (GameNodeInterface) registry.lookup("GameNode_" + senderInfo.getPlayerId());
            // 调用发送者节点的 receiveReplyMessage 方法（需要在 GameNodeInterface 中定义）
            senderNode.receiveReplyMessage(new GossipMessage(primaryNode, version, updatedPlayers, crashedPlayers, primaryNode, null));

            System.out.println("Sent reply message to " + senderInfo.getPlayerId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void receiveGossipMessage(GossipMessage message) throws RemoteException {
        List<PlayerInfo> updatedPlayers = message.getUpdatedPlayers();
        PlayerInfo senderInfo = message.getSenderInfo();
        List<PlayerInfo> newPlayers = new ArrayList<>();
        boolean updated = false;
        for(int i = updatedPlayers.size() - 1; i >= 0; i--){
            PlayerInfo player = updatedPlayers.get(i);
            if(players.stream().noneMatch(p -> p.getPlayerId().equals(player.getPlayerId()))){
                players.add(player);
                newPlayers.add(player);
                this.updatedPlayers.add(player);
                updated = true;
            }else{
                break;
            }
        }

        List<PlayerInfo> crashedPlayers = message.getCrashedPlayers();
        for(int i = crashedPlayers.size() - 1; i >= 0; i--){
            PlayerInfo player = crashedPlayers.get(i);
            if(players.stream().anyMatch(p -> p.getPlayerId().equals(player.getPlayerId()))){
                players.removeIf(p -> p.getPlayerId().equals(player.getPlayerId()));
                this.crashedPlayers.add(player);
                updated = true;
            }else{
                break;
            }
        }
        if (updated) {
            version++;
            super.sendGossipMessage(senderInfo);
        }

        // 处理崩溃的玩家信息
//        if (message.getCrashedPlayers() != null && !message.getCrashedPlayers().isEmpty()) {
//            synchronized (players) {
//                for (PlayerInfo crashedPlayer : message.getCrashedPlayers()) {
//                    String crashedPlayerId = crashedPlayer.getPlayerId();
//                    players.removeIf(player -> player.getPlayerId().equals(crashedPlayerId));
//                    System.out.println("Removed crashed player from gossip: " + crashedPlayer.getPlayerId());
//
//                    if (crashedPlayerId.equals(backupNode.getPlayerId())) {
//                        System.out.println("Backup node has crashed (via gossip). Selecting new backup...");
////                        selectNewBackup();
//                    }
//                }
//            }
            // 更新游戏状态
//            updateGameStateAfterCrashGossip(message.getCrashedPlayers());
        }
    }
