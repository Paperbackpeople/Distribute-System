import Interface.GameNodeInterface;
import Message.GossipMessage;
import Player.PlayerInfo;
import Server.Tracker;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Random;

public class Game implements GameNodeInterface {
    private String playerId;
    private Tracker tracker;
    private PlayerInfo playerInfo;
    private String primaryNodeIp;
    private int primaryNodePort;
    private int version;

    List<PlayerInfo> players;

    public Game(String trackerIp, int trackerPort, String playerId) {
        this.playerId = playerId;
        try {
            // 1. 连接到 Tracker
            Registry trackerRegistry = LocateRegistry.getRegistry(trackerIp, trackerPort);
            tracker = (Tracker) trackerRegistry.lookup("Tracker");

            // 2. 注册玩家
            tracker.registerPlayer(playerId, "Player's IP Address"); // 替换为实际玩家的 IP 地址

            // 3. 获取游戏状态
            players = tracker.getPlayerList();
            initializeGameState(players);

            // 4. 绑定 RMI 服务
            GameNodeInterface stub = (GameNodeInterface) UnicastRemoteObject.exportObject(this, 0);
            Registry localRegistry = LocateRegistry.getRegistry(); // 获取本地 RMI 注册表
            localRegistry.bind("GameNode_" + playerId, stub); // 使用唯一名称绑定对象，避免命名冲突

            // 5. 启动 Gossip 协议
            startGossip();
            requestPrimaryNodeInfo();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGossip() {
        // 启动Gossip协议
        new Thread(() -> {
            while (true) {
                try{
                    PlayerInfo randomPlayer = getPlayerInfo();
                    if (randomPlayer != null) {
                        sendGossipMessage(randomPlayer);
                    }
                    Thread.sleep(1000);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    public PlayerInfo getPlayerInfo() {
        players.removeIf(player -> player.getPlayerId().equals(this.playerId)); // 排除自己
        if (players.isEmpty()) {
            return null;
        }
        int index = new Random().nextInt(players.size());
        return players.get(index);
    }

    private void initializeGameState(List<PlayerInfo> players) {
        // 初始化游戏状态，如设置迷宫大小、玩家位置等
        System.out.println("Initializing game state for player: " + playerId);
    }

    private int getPlayerMove() {
        // 获取玩家的移动输入，如0-4表示不同方向，9表示退出
        return 0;
    }
    private void handleMove(int move) {
        try {
            if (move >= 0 && move <= 4) {
                // 移动玩家
                System.out.println("Moving player " + playerId + " to direction " + move);
            } else if (move == 9) {
                exitGame();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshGameState() {
        // 从主服务器获取最新的游戏状态
    }

    private void exitGame() {
        try {
            tracker.removePlayer(playerId);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void receiveGossipMessage(GossipMessage message) throws RemoteException {
        if (isNewerPrimaryNode(message)) {
            this.primaryNodeIp = message.getPrimaryNodeIp();
            this.primaryNodePort = message.getPrimaryNodePort();
            this.version = message.getVersion();
            System.out.println("Updated primary node info from gossip.");
        }
    }
    private void sendGossipMessage(PlayerInfo targetNode) {
        try {
            Registry registry = LocateRegistry.getRegistry(targetNode.getIpAddress(), targetNode.getPort());
            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + targetNode.getPlayerId());
            GossipMessage message = new GossipMessage(primaryNodeIp, primaryNodePort , version);
            node.receiveGossipMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean isNewerPrimaryNode(GossipMessage message) {
        // 实现逻辑来判断新的主节点信息是否更优先
        return message.getVersion() > this.version;
    }

    // 主动请求主节点信息的方法
    private void requestPrimaryNodeInfo() {
        players.removeIf(player -> player.getPlayerId().equals(this.playerId)); // 排除自己

        for (PlayerInfo player : players) {
            try {
                Registry registry = LocateRegistry.getRegistry(player.getIpAddress(), player.getPort());
                GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + player.getPlayerId());
                GossipMessage message = node.getPrimaryNodeInfo();
                if (message != null) {
                    this.primaryNodeIp = message.getPrimaryNodeIp();
                    this.primaryNodePort = message.getPrimaryNodePort();
                    this.version = message.getVersion();
                    System.out.println("Obtained primary node info from existing nodes.");
                    break; // 已获取主节点信息，退出循环
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 获取主节点信息的实现
    @Override
    public GossipMessage getPrimaryNodeInfo() {
        return new GossipMessage(primaryNodeIp, primaryNodePort, version);
    }

    // 简单的 ping 方法
    @Override
    public void ping() {
        // 简单回应，不需要具体实现
    }
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Game [IP-address] [port-number] [player-id]");
            return;
        }

        String trackerIp = args[0];
        int trackerPort = Integer.parseInt(args[1]);
        String playerId = args[2];

        Game game = new Game(trackerIp, trackerPort, playerId);
    }


}