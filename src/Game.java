import Interface.GameNodeInterface;
import Message.GossipMessage;
import Player.PlayerInfo;
import Server.Tracker;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Random;

public class Game implements GameNodeInterface {
    private String playerId;
    private Tracker tracker;
    private PlayerInfo playerInfo;
    private String primaryNodeIp;
    private int primaryNodePort;

    List<PlayerInfo> players;

    public Game(String trackerIp, int trackerPort, String playerId) {
        this.playerId = playerId;
        try {
            // 连接Tracker
            Registry registry = LocateRegistry.getRegistry(trackerIp, trackerPort);
            tracker = (Tracker) registry.lookup("Tracker");

            // 注册玩家
            tracker.registerPlayer(playerId, "Player's IP Address");

            // 获取游戏状态
            players = tracker.getPlayerList();
            initializeGameState(players);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startGossip() {
        // 启动Gossip协议
        new Thread(() -> {
            while (true) {

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
    private GameServer getPrimaryServer() {
// 获取主服务器
        return null;
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

    @Override
    public void receiveGossipMessage(GossipMessage message) throws RemoteException {

    }
}