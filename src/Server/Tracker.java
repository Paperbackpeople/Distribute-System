package Server;

import Interface.TrackerInterface;
import Message.TrackerMessage;
import Player.PlayerInfo;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class Tracker extends UnicastRemoteObject implements TrackerInterface {
    private int N; // 迷宫的大小
    private int K; // 宝藏的数量
    private List<PlayerInfo> players; // 当前在线的玩家列表
    private int version; // 版本号，用于状态同步
    public final String trackerId = "01";
    public final int trackerPort = 1099;


    protected Tracker(int N, int K) throws RemoteException {
        super();
        this.N = N;
        this.K = K;
        this.players = new ArrayList<>();
        this.version = 0;
    }

    /**
     * 处理来自 GamerNode 的 TrackerMessage
     */
    @Override
    public synchronized TrackerMessage handleTrackerMessage(TrackerMessage message) throws RemoteException {
        String type = message.getMessageType();
        PlayerInfo sender = message.getSenderInfo();

        switch (type) {
            case "JOIN":
                handleJoin(message, sender);
                break;
            case "UPDATE":
                handleUpdate(message);
                break;
            default:
                System.out.println("Unknown TrackerMessage type: " + type);
                break;
        }

        // 构建响应消息
        TrackerMessage response = new TrackerMessage(
                type + "_RESPONSE",
                new ArrayList<>(players),
                N,
                K,
                new PlayerInfo(trackerId)
        );

        return response;
    }


    /**
     * 处理 "JOIN" 类型的消息
     */
    private void handleJoin(TrackerMessage message, PlayerInfo sender) {
        // Tracker 不管理玩家列表，仅记录 JOIN 请求
        System.out.println("Received JOIN request from " + sender.getPlayerId());
    }

    /**
     * 处理 "UPDATE" 类型的消息
     */
    private void handleUpdate(TrackerMessage message) {
        List<PlayerInfo> updatedPlayers = message.getPlayerList();
        this.players = updatedPlayers;
        version = message.getVersion();
        System.out.println("Player list updated by primary node." +  "Current player list: " + players);
    }


    /**
     * 启动 Tracker 服务器的方法
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Tracker [port-number] [N] [K]");
            return;
        }

        int portNumber = Integer.parseInt(args[0]);
        int N = Integer.parseInt(args[1]);
        int K = Integer.parseInt(args[2]);

        try {
            // 创建并启动 Tracker
            Tracker tracker = new Tracker(N, K);

            // 启动 RMI 注册表
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(portNumber);
                System.out.println("RMI Registry created on port " + portNumber);
            } catch (RemoteException e) {
                // 如果注册表已存在，则获取现有的注册表
                registry = LocateRegistry.getRegistry(portNumber);
                System.out.println("RMI Registry already exists on port " + portNumber);
            }

            // 绑定 Tracker 到 RMI 注册表
            registry.rebind("Tracker", tracker);
            System.out.println("Tracker is running on port " + portNumber + " with maze size " + N + "x" + N + " and " + K + " treasures.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}