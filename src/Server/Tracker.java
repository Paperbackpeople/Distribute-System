package Server;
import Player.PlayerInfo;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class Tracker extends UnicastRemoteObject {
    private int N; // 迷宫的大小
    private int K; // 宝藏的数量
    private List<PlayerInfo> players; // 当前在线的玩家列表

    public Tracker(int N, int K) throws RemoteException {
        super();
        this.N = N;
        this.K = K;
        this.players = new ArrayList<>();
    }

    // 玩家首次加入游戏时调用此方法
    public synchronized void registerPlayer(String playerId, String playerAddress) throws RemoteException {
        players.add(new PlayerInfo(playerId, playerAddress));
        System.out.println("Player " + playerId + " joined the game.");
    }

    // 当玩家崩溃时调用此方法，返回当前玩家列表，以帮助重新生成主服务器或备份服务器
    public synchronized List<PlayerInfo> getPlayerList() throws RemoteException {
        return new ArrayList<>(players);
    }

    // 在玩家崩溃后，通知Tracker移除该玩家
    public synchronized void removePlayer(String playerId) throws RemoteException {
        players.removeIf(player -> player.getPlayerId().equals(playerId));
        System.out.println("Player " + playerId + " has been removed from the game.");
    }

    // 打印当前在线玩家
    public void printPlayerList() {
        System.out.println("Current players: ");
        for (PlayerInfo player : players) {
            System.out.println(player.getPlayerId() + " at " + player.getPlayerAddress());
        }
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Tracker [port-number] [N] [K]");
            return;
        }

        int portNumber = Integer.parseInt(args[0]);
        int N = Integer.parseInt(args[1]);
        int K = Integer.parseInt(args[2]);
        try {
            // 创建并启动Tracker
            Tracker tracker = new Tracker(N, K);
            // 注册RMI服务（如果使用RMI的话）
            java.rmi.registry.LocateRegistry.createRegistry(portNumber);
            java.rmi.Naming.rebind("rmi://localhost:" + portNumber + "/Tracker", tracker);
            System.out.println("Tracker is running on port " + portNumber + " with maze size " + N + "x" + N + " and " + K + " treasures.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}