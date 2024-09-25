package Client;
import Interface.GameNodeInterface;
import Message.GossipMessage;
import Player.PlayerInfo;
import Server.Tracker;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GamerNode implements GameNodeInterface {
    private String playerId;
    private Tracker tracker;
    private PlayerInfo playerInfo;
    private PlayerInfo primaryNode;
    private PlayerInfo backupNode;
    private int version;
    private List<PlayerInfo> updatedPlayers;
    List<PlayerInfo> players;
    private List<> crashedPlayers = new ArrayList<>();
    private boolean isPrimary = false;
    private boolean isBackup = false;

    public GamerNode(String trackerIp, int trackerPort, String playerId) {
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
            requestPrimaryNodeInfoAndNotifyJoin();
            if(isPrimary){
                initializeGameState(players);
            }
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
        if (message.getVersion() > this.version) {
            this.primaryNode = message.getPrimaryNode();
            this.version = message.getVersion();
            this.updatedPlayers = message.getUpdatedPlayers();
            this.crashedPlayers = message.getCrashedPlayers();
        }

        if (message.getUpdatedPlayers() != null && !message.getUpdatedPlayers().isEmpty()) {
            synchronized (players){
                for (PlayerInfo player : message.getUpdatedPlayers()) {
                    if (players.stream().noneMatch(p -> p.getPlayerId().equals(player.getPlayerId()))) {
                        players.add(player);
                    }
                }

            }
        }

        // 处理崩溃的玩家信息
        if (message.getCrashedPlayers() != null && !message.getCrashedPlayers().isEmpty()) {
            synchronized (players) {
                for (PlayerInfo crashedPlayer : message.getCrashedPlayers()) {
                    String crashedPlayerId = crashedPlayer.getPlayerId();
                    players.removeIf(player -> player.getPlayerId().equals(crashedPlayerId));
                    System.out.println("Removed crashed player from gossip: " + crashedPlayer.getPlayerId());

                    // 如果崩溃的节点是主服务器或备份服务器，启动相应的处理
                    if (crashedPlayerId.equals(primaryNode.getPlayerId())) {
                        System.out.println("Primary node has crashed (via gossip). Initiating election...");
//                        initiateElection();
                    } else if (crashedPlayerId.equals(backupNode.getPlayerId())) {
                        System.out.println("Backup node has crashed (via gossip). Selecting new backup...");
//                        selectNewBackup();
                    }
                }
            }
            // 更新游戏状态
//            updateGameStateAfterCrashGossip(message.getCrashedPlayers());
    }
}

    private void sendGossipMessage(PlayerInfo targetNode) {
        try {
            Registry registry = LocateRegistry.getRegistry(targetNode.getIpAddress(), targetNode.getPort());
            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + targetNode.getPlayerId());
            List<PlayerInfo> crashedPlayersCopy = new ArrayList<>(this.crashedPlayers);
            GossipMessage message = new GossipMessage(primaryNode, version, updatedPlayers, crashedPlayersCopy);
            this.crashedPlayers.clear();
            node.receiveGossipMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 简单的 ping 方法
    @Override
    public void ping() {
        // 简单回应，不需要具体实现
    }

    private void startPinging() {
        new Thread(() -> {
            Random random = new Random();
            while (true) {
                try {
                    PlayerInfo target = null;
                    synchronized (players) {
                        if (players.size() > 1) {
                            // 构建一个可用的目标节点列表，排除自己
                            List<PlayerInfo> availablePlayers = new ArrayList<>(players);
                            availablePlayers.removeIf(player -> player.getPlayerId().equals(this.playerId));
                            if (!availablePlayers.isEmpty()) {
                                int index = random.nextInt(availablePlayers.size());
                                target = availablePlayers.get(index);
                            }
                        }
                    }
                    if (target != null) {
                        try {
                            Registry registry = LocateRegistry.getRegistry(target.getIpAddress(), target.getPort());
                            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + target.getPlayerId());
                            node.ping(); // 调用目标玩家的 ping 方法
                            System.out.println("Pinged player " + target.getPlayerId());
                        } catch (Exception e) {
                            // 如果无法连接到目标玩家，认为其已崩溃
                            handlePlayerCrash(target);
                        }
                    }
                    Thread.sleep(500); // 每 0.5 秒 ping 一次
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private void handlePlayerCrash(PlayerInfo crashedPlayer) {
        System.out.println("Player " + crashedPlayer.getPlayerId() + " has crashed.");

        // 从本地的 players 列表中移除崩溃的节点
        synchronized (players) {
            players.removeIf(player -> player.getPlayerId().equals(crashedPlayer.getPlayerId()));
        }

        // 将崩溃的节点添加到 crashedPlayers 列表中，以便通过 Gossip 传播
        synchronized (crashedPlayers) {
            if (!crashedPlayers.contains(crashedPlayer.getPlayerId())) {
                crashedPlayers.add(crashedPlayer.getPlayerId());
            }
        }

        // 更新游戏状态，移除崩溃玩家的角色
//        updateGameStateAfterCrash(crashedPlayer.getPlayerId());

        // 如果崩溃的节点是主服务器或备份服务器
        if (crashedPlayer.getPlayerId().equals(primaryNode.getPlayerId())) {
            System.out.println("Primary node has crashed. Initiating election...");
//            initiateElection();
        } else if (crashedPlayer.getPlayerId().equals(backupNode.getPlayerId())) {
            System.out.println("Backup node has crashed. Selecting new backup...");
//            selectNewBackup();
        }
    }
//    private void updateGameStateAfterCrash(String crashedPlayerId) {
//        synchronized (gameState) {
//            gameState.removePlayer(crashedPlayerId);
//            System.out.println("Removed crashed player's character from the game.");
//        }
//        // 可选地，将更新后的游戏状态传播给其他节点
//        propagateGameState();
//    }
//
//    private void updateGameStateAfterCrashGossip(List<String> crashedPlayerIds) {
//        synchronized (gameState) {
//            for (String playerId : crashedPlayerIds) {
//                gameState.removePlayer(playerId);
//            }
//            System.out.println("Updated game state after receiving crashed players from gossip.");
//        }
//        // 可选地，将更新后的游戏状态传播给其他节点
//        propagateGameState();
//    }
    private void requestPrimaryNodeInfoAndNotifyJoin() {
        players.removeIf(player -> player.getPlayerId().equals(this.playerId)); // 排除自己
        if(players.isEmpty()){
        }
        // 选择若干节点进行交互
        int nodesToContact = Math.min(3, players.size()); // 最多联系3个节点
        Collections.shuffle(players);

        List<PlayerInfo> nodesToInteract = players.subList(0, nodesToContact);

        for (PlayerInfo player : nodesToInteract) {
            try {
                Registry registry = LocateRegistry.getRegistry(player.getIpAddress(), player.getPort());
                GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + player.getPlayerId());

                // 构建包含自己信息的 GossipMessage，并请求主节点信息
                List<PlayerInfo> newPlayers = new ArrayList<>();
                newPlayers.add(this.playerInfo);

                GossipMessage response = node.joinAndProvideInfo(newPlayers);
                if (response != null) {
                    // 更新本地的主节点信息和其他状态
                    this.primaryNode = response.getPrimaryNode();
                    this.version = response.getVersion();

                    // 更新玩家列表
                    if (response.getUpdatedPlayers() != null) {
                        synchronized (players) {
                            for (PlayerInfo newPlayer : response.getUpdatedPlayers()) {
                                if (!players.contains(newPlayer) && !newPlayer.getPlayerId().equals(this.playerId)) {
                                    players.add(newPlayer);
                                    System.out.println("Added new player from response: " + newPlayer.getPlayerId());
                                }
                            }
                        }
                    }

                    System.out.println("Obtained primary node info and notified others about join.");
                    break; // 已成功交互，退出循环
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public GossipMessage joinAndProvideInfo(List<PlayerInfo> newPlayers) throws RemoteException {
        // 接收新节点的加入信息
        boolean version_flag = false;
        if (newPlayers != null && !newPlayers.isEmpty()) {
            synchronized (players) {
                for (PlayerInfo newPlayer : newPlayers) {
                    if (!players.contains(newPlayer) && !newPlayer.getPlayerId().equals(this.playerId)) {
                        players.add(newPlayer);
                        version_flag = true;
                        System.out.println("Added new player from join request: " + newPlayer.getPlayerId());
                    }
                }
            }
        }

        // 构建回复的 GossipMessage，包含主节点信息和玩家列表更新
        synchronized (players) {
            List<PlayerInfo> updatedPlayers = new ArrayList<>(players);
            if(!updatedPlayers.contains(this.playerInfo)){
                updatedPlayers.add(this.playerInfo);
        }
        if(version_flag){
            this.version++;
        }
        return new GossipMessage(primaryNode, this.version, updatedPlayers, crashedPlayers);
    }
}

//    public static void main(String[] args) {
//        if (args.length < 3) {
//            System.out.println("Usage: java Game [IP-address] [port-number] [player-id]");
//            return;
//        }
//
//        String trackerIp = args[0];
//        int trackerPort = Integer.parseInt(args[1]);
//        String playerId = args[2];
//
//        Game game = new Game(trackerIp, trackerPort, playerId);
//    }


}