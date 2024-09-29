package Client;
import Interface.GameNodeInterface;
import Message.ElectionMessage;
import Message.GameState;
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
    private PlayerInfo primaryNode;
    private PlayerInfo backupNode;
    private int version = 0;
    private List<PlayerInfo> updatedPlayers;
    List<PlayerInfo> players;
    private List<PlayerInfo> crashedPlayers = new ArrayList<>();
    private boolean isPrimary = false;
    private boolean isBackup = false;
    private GameState gameState;
    private PlayerInfo playerInfo;
    boolean electionInProgress = false;
    int electionVersion = 0;

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
            requestPrimaryNodeInfoAndNotifyJoin();
            //sleep 1s to prepare the initialization
            Thread.sleep(1000);
            startGossip();
            startPinging();
            if (isPrimary) {
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
                try {
                    PlayerInfo randomPlayer = getPlayerInfo();
                    if (randomPlayer != null) {
                        sendGossipMessage(randomPlayer);
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
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

    /*这部分放到gui处理更合适？
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
    */

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
            players = message.getUpdatedPlayers();
        }

        if (message.getUpdatedPlayers() != null && !message.getUpdatedPlayers().isEmpty()) {
            synchronized (players) {
                for (PlayerInfo player : message.getUpdatedPlayers()) {
                    if (players.stream().noneMatch(p -> p.getPlayerId().equals(player.getPlayerId()))) {
                        players.add(player);
                        this.updatedPlayers.add(player);
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
                        initiateElection();
                    } else if (crashedPlayerId.equals(backupNode.getPlayerId())) {
                        System.out.println("Backup node has crashed (via gossip). Selecting new backup...");
                        selectNewBackup();
                    }
                }
            }
            // 更新游戏状态
//            updateGameStateAfterCrashGossip(message.getCrashedPlayers());
        }
        if(message.getUpdatedPlayers().size() == 1 && message.getCrashedPlayers().isEmpty()){
            sendGossipMessage(message.getSenderInfo());
        }
    }

    @Override
    public void receiveReplyMessage(GossipMessage reply) throws RemoteException {
        this.primaryNode = reply.getPrimaryNode();
        this.version = reply.getVersion();
        this.updatedPlayers = reply.getUpdatedPlayers();
        this.crashedPlayers = reply.getCrashedPlayers();
        this.gameState = reply.getGameState();
    }

    public void sendGossipMessage(PlayerInfo targetNode) {
        try {
            Registry registry = LocateRegistry.getRegistry(targetNode.getIpAddress(), targetNode.getPort());
            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + targetNode.getPlayerId());
            GossipMessage message = new GossipMessage(primaryNode, version, updatedPlayers, crashedPlayers, this.playerInfo, null);
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

    @Override
    public void receiveElectionMessage(ElectionMessage message) throws RemoteException {
        synchronized (this) {
            if (message.getElectionVersion() < electionVersion) {
                // 接收到旧的选举消息，忽略
                return;
            } else if (message.getElectionVersion() > electionVersion) {
                // 接收到新的选举消息，更新选举信息
                electionVersion = message.getElectionVersion();
                electionInProgress = true;
            }

            // 更新候选者列表
            List<PlayerInfo> candidate = message.getCandidate();
            if (!candidate.contains(this.playerInfo)) {
                candidate.add(this.playerInfo);
                message = new ElectionMessage(electionVersion, candidate);
                // 继续传播选举消息
                broadcastElectionMessage(message);
            } else {
                // 所有节点都已参与选举，选举结束
                concludeElection(candidate);
            }
        }
    }

    private void initiateElection() {
        synchronized (this) {
            if (electionInProgress) {
                return; // 已有选举在进行中
            }
            electionInProgress = true;
            electionVersion = generateElectionVersion(); // 生成新的选举版本号

            List<String> candidateIds = new ArrayList<>();
            candidateIds.add(this.playerId); // 将自己加入候选者列表

            ElectionMessage electionMessage = new ElectionMessage(electionVersion, candidateIds);

            // 通过 Gossip 传播选举消息
            broadcastElectionMessage(electionMessage);
        }
    }

    private void selectNewBackup() {

    }

    private void initiateElection() {
        synchronized (this) {
            if (electionInProgress) {
                return; // 已有选举在进行中
            }
            electionInProgress = true;
            electionVersion = generateElectionVersion(); // 生成新的选举版本号

            List<PlayerInfo> candidate = new ArrayList<>();
            candidate.add(this.playerInfo); // 将自己加入候选者列表

            ElectionMessage electionMessage = new ElectionMessage(electionVersion, candidate);

            // 通过 Gossip 传播选举消息
            broadcastElectionMessage(electionMessage);
        }
    }

    private int generateElectionVersion() {
        // 可以使用当前时间戳或其他方式生成唯一的版本号
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    private void broadcastElectionMessage(ElectionMessage message) {
        synchronized (players) {
            for (PlayerInfo player : players) {
                if (player.getPlayerId().equals(this.playerId)) {
                    continue; // 跳过自己
                }
                try {
                    Registry registry = LocateRegistry.getRegistry(player.getIpAddress(), player.getPort());
                    GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + player.getPlayerId());
                    node.receiveElectionMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
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
                    if (target != null && target != primaryNode) {
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
                    if (target == primaryNode) {
                        try {
                            Registry registry = LocateRegistry.getRegistry(primaryNode.getIpAddress(), primaryNode.getPort());
                            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + primaryNode.getPlayerId());
                            node.ping();
                            node.receiveGossipMessage(new GossipMessage(primaryNode, version, updatedPlayers, crashedPlayers, this.playerInfo, null));
                        } catch (Exception e) {
                            // 如果无法连接到目标玩家，认为其已崩溃
                            handlePlayerCrash(primaryNode);
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
                crashedPlayers.add(crashedPlayer);
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
        if (players.isEmpty()) {
            isPrimary = true;
            return;
        }
        // 选择若干节点进行交互
        int nodesToContact = Math.min(3, players.size()); // 最多联系3个节点
        Collections.shuffle(players);

        List<PlayerInfo> nodesToInteract = players.subList(0, nodesToContact);

        for (PlayerInfo player : nodesToInteract) {
            try {
                Registry registry = LocateRegistry.getRegistry(player.getIpAddress(), player.getPort());
                GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + player.getPlayerId());
                this.updatedPlayers = new ArrayList<>(players);

                sendGossipMessage(player);

                if (response != null) {
                    // 更新本地的主节点信息和其他状态
                    this.primaryNode = response.getPrimaryNode();
                    this.version = response.getVersion();
                    this.updatedPlayers = response.getUpdatedPlayers();
                    this.crashedPlayers = response.getCrashedPlayers();
                    this.gameState = response.getGameState();
                    //向主节点发送自己的信息
                    try {
                        sendGossipMessage(primaryNode);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    break; // 已成功交互，退出循环
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}