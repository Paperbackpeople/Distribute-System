package Client;
import Interface.GameNodeInterface;
import Interface.TrackerCommunicationInterface;
import Interface.TrackerInterface;
import Message.ElectionMessage;
import Message.GameState;
import Message.GossipMessage;
import Message.TrackerMessage;
import Player.PlayerInfo;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

public class GamerNode implements GameNodeInterface, TrackerCommunicationInterface {
    private static final Logger logger = Logger.getLogger(GamerNode.class.getName());
    private String playerId;
    private TrackerInterface tracker;
    private PlayerInfo primaryNode;
    private PlayerInfo backupNode;
    private int version = 0;
    private List<PlayerInfo> updatedPlayers = new ArrayList<>();
    List<PlayerInfo> players;
    private List<PlayerInfo> crashedPlayers = new ArrayList<>();
    private boolean isPrimary = false;
    private boolean isBackup = false;
    private GameState gameState = new GameState();
    private PlayerInfo playerInfo;
    boolean electionInProgress = false;
    int electionVersion = 0;


    public GamerNode(String trackerIp, int trackerPort, String playerId) {
        this.playerId = playerId;
        try {
            // 1. 连接到 Tracker
            Registry trackerRegistry = LocateRegistry.getRegistry(trackerIp, trackerPort);
            tracker = (TrackerInterface) trackerRegistry.lookup("Tracker");
            this.playerInfo = new PlayerInfo(playerId);
            sendJoinToTracker();
            // 4. 绑定 RMI 服务
            GameNodeInterface stub = (GameNodeInterface) UnicastRemoteObject.exportObject(this, 0);
            Registry localRegistry = LocateRegistry.getRegistry(); // 获取本地 RMI 注册表
            localRegistry.bind("GameNode_" + playerId, stub); // 使用唯一名称绑定对象，避免命名冲突
            requestPrimaryNodeInfoAndNotifyJoin();
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

    public void startPrimaryGossip() {
        System.out.println(" 启动 Primary Gossip 协议");
        // 启动Gossip协议
        new Thread(() -> {
            while (true) {
                try {
                    sendTrackerMessage(new TrackerMessage("UPDATE", updatedPlayers, gameState.getN(), gameState.getK(), version, this.playerInfo));
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }
    public void startNormalGossip() {
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
    public void startGossip() {
        if(isPrimary){
            startPrimaryGossip();
        } else{
            startNormalGossip();
        }

    }


    public PlayerInfo getPlayerInfo() {
        List<PlayerInfo> players = new ArrayList<>(this.players);
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
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void receiveGossipMessagePrimary(GossipMessage message){
        boolean changed = false; // 标记是否有更新或移除操作
        // 1. 处理更新的玩家信息
        if (message.getUpdatedPlayers() != null && !message.getUpdatedPlayers().isEmpty()) {
            synchronized (players) {
                for (PlayerInfo player : message.getUpdatedPlayers()) {
                    if(player.getPlayerId().equals(this.playerId)){
                        continue;
                    }
                    boolean playerExists = players.stream()
                            .anyMatch(p -> p.getPlayerId().equals(player.getPlayerId()));
                    if (!playerExists) {
                        players.add(player);
                        this.updatedPlayers.add(player);
                        changed = true;
                        System.out.println("Added new player from gossip: " + player.getPlayerId());
                    }
                }
            }
        }

        if(message.getBackupNode() != null) {
            if (this.backupNode == null || crashedPlayers.contains(this.backupNode)) {
                logger.info("Updated backup node from gossip: " + message.getBackupNode().getPlayerId());
                this.backupNode = message.getBackupNode();
                changed = true;
            }
        }
        // 处理崩溃的玩家信息
        if (message.getCrashedPlayers() != null && !message.getCrashedPlayers().isEmpty()) {
            synchronized (players) {
                for (PlayerInfo crashedPlayer : message.getCrashedPlayers()) {
                    String crashedPlayerId = crashedPlayer.getPlayerId();
                    boolean removed = players.removeIf(player -> player.getPlayerId().equals(crashedPlayerId));
                    if (removed) {
                        System.out.println("Removed crashed player from gossip: " + crashedPlayerId);
                        changed = true;
                    }
                    if (crashedPlayerId.equals(backupNode.getPlayerId())) {
                        System.out.println("Backup node has crashed (via gossip). Selecting new backup...");
                        selectNewBackup();
                    }
                }
            }
            // 更新游戏状态
//            updateGameStateAfterCrashGossip(message.getCrashedPlayers());
        }
        // 3. 如果有任何更改，且消息中的版本号高于当前版本，更新版本号
        if (changed) {
            this.version++;
            System.out.println("Updated version to: " + version);
        }
        sendGossipMessagePrimary(message.getSenderInfo());
    }

    public void receiveGossipMessageNormal(GossipMessage message){
        logger.info("Received gossip message from " + message.getSenderInfo().getPlayerId());
        logger.info("version: " + version);
        logger.info("message version: " + message.getVersion());
        if (message.getVersion() >= this.version) {
            this.primaryNode = message.getPrimaryNode();
            this.backupNode = message.getBackupNode();
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
    public void receiveGossipMessage(GossipMessage message) throws RemoteException {
        if (isPrimary) {
            receiveGossipMessagePrimary(message);
        } else {
            receiveGossipMessageNormal(message);
        }
    }

    public void sendGossipMessage(PlayerInfo targetNode) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + targetNode.getPlayerId());
            GossipMessage message = new GossipMessage(primaryNode, backupNode, version, updatedPlayers, crashedPlayers, this.playerInfo, null);
            System.out.println("我的信息"+message);
            node.receiveGossipMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendGossipMessagePrimary(PlayerInfo targetNode) {
        logger.info("Sending gossip message to " + targetNode.getPlayerId());
        logger.info("backupNode: " + backupNode);
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + targetNode.getPlayerId());
            GossipMessage message = new GossipMessage(primaryNode, backupNode,version, updatedPlayers, crashedPlayers, this.playerInfo, gameState);
            System.out.println("我的信息"+message);
            node.receiveGossipMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 简单的 ping 方法
    @Override
    public void ping() {
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
            int participantCount = message.getParticipantCount();

            if (!candidate.contains(this.playerInfo)) {
                candidate.add(this.playerInfo);
                participantCount++;
                message = new ElectionMessage(electionVersion, candidate, participantCount);
                // 继续传播选举消息
                broadcastElectionMessage(message);
            }

            // 检查选举是否可以结束
            int knownNodes = getKnownNodesCount(); // 获取已知的存活节点数，包括自己
            if (participantCount >= knownNodes) {
                // 所有节点都已参与选举，选举结束
                concludeElection(candidate);
            }
        }
    }
    private int getKnownNodesCount() {
        synchronized (players) {
            return players.size() + 1; // players 列表不包含自己，所以加1
        }
    }
    private void concludeElection(List<PlayerInfo> candidateIds) {
        // 根据规则选出新的主节点，例如选择最小的 ID
        String newPrimaryId = candidateIds.stream().map(PlayerInfo::getPlayerId).min(String::compareTo).orElse(null);

        assert newPrimaryId != null;
        if (newPrimaryId.equals(this.playerId)) {
            // 自己成为新的主节点
            becomePrimary();
        } else {
            // 更新主节点信息
            PlayerInfo newPrimary = findPlayerById(newPrimaryId);
            if (newPrimary != null) {
                primaryNode = newPrimary;
                isPrimary = false;
                System.out.println("New primary node is " + primaryNode.getPlayerId());
            }
        }

        // 选举结束，重置选举状态
        electionInProgress = false;

        // 通知其他节点新的主节点信息
        broadcastPrimaryNodeInfo();
    }
    private PlayerInfo findPlayerById(String playerId) {
        synchronized (players) {
            for (PlayerInfo player : players) {
                if (player.getPlayerId().equals(playerId)) {
                    return player;
                }
            }
        }
        return null;
    }
    private void becomePrimary() {
        isPrimary = true;
        primaryNode = this.playerInfo;
        isBackup = false;
        System.out.println("This node has become the primary server.");
        // 增加版本号
        version++;
        // 初始化或更新游戏状态
//        initializeGameState();
        // 广播新的主节点信息
        broadcastPrimaryNodeInfo();
    }
    private void becomeBackup() {
        isBackup = true;
        backupNode = this.playerInfo;
        isPrimary = false;
        System.out.println("This node has become the backup server.");
    }
    private void initiateElection() {
        synchronized (this) {
            if (electionInProgress) {
                return; // 已有选举在进行中
            }
            electionInProgress = true;
            electionVersion = generateElectionVersion(); // 生成新的选举版本号

            List<PlayerInfo> candidateIds = new ArrayList<>();
            candidateIds.add(this.playerInfo); // 将自己加入候选者列表

            ElectionMessage electionMessage = new ElectionMessage(electionVersion, candidateIds, 1);

            // 通过 Gossip 传播选举消息
            broadcastElectionMessage(electionMessage);
        }
    }

    private void selectNewBackup() {

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
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + player.getPlayerId());
                    node.receiveElectionMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    private void broadcastPrimaryNodeInfo() {
        synchronized (players) {
            for (PlayerInfo player : players) {
                if (player.getPlayerId().equals(this.playerId)) {
                    continue; // 跳过自己
                }
                try {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + player.getPlayerId());
                    // 构建 GossipMessage，包含新的主节点信息
                    GossipMessage message = new GossipMessage(primaryNode, backupNode,version, updatedPlayers, crashedPlayers, this.playerInfo, null);
                    node.receiveGossipMessage(message);
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
                            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
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
                            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + primaryNode.getPlayerId());
                            node.ping();
                            System.out.println("Pinged primary node " + primaryNode.getPlayerId());
                            node.receiveGossipMessage(new GossipMessage(primaryNode, backupNode,version, updatedPlayers, crashedPlayers, this.playerInfo, null));
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
            updatedPlayers.add(this.playerInfo);
            becomePrimary();
            System.out.println("No existing nodes found. This node becomes the primary node.");
            return;
        }
        // 选择若干节点进行交互
        int nodesToContact = Math.min(3, players.size()); // 最多联系3个节点
        Collections.shuffle(players);
        List<PlayerInfo> nodesToInteract = players.subList(0, nodesToContact);
        for (PlayerInfo player : nodesToInteract) {
            try {
                this.updatedPlayers.add(this.playerInfo);
                sendGossipMessage(player);
                while ((primaryNode == null) && !isPrimary) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                sendGossipMessage(primaryNode);
                if (backupNode == null) {
                    becomeBackup();
                    sendGossipMessage(primaryNode);
                }
                break; // 已成功获取主节点信息，退出循环


            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // 方法：发送 JOIN 消息到 Tracker
    private void sendJoinToTracker() {
        try {
            // 创建 JOIN 类型的 TrackerMessage
            TrackerMessage joinMessage = new TrackerMessage(
                    "JOIN",
                    List.of(this.playerInfo),
                    0,
                    0,
                    version,
                    this.playerInfo
            );

            // 发送 TrackerMessage 并接收响应
            TrackerMessage response = sendTrackerMessage(joinMessage);

            if ("JOIN_RESPONSE".equals(response.getMessageType())) {
                // 处理 JOIN_RESPONSE
                this.players = response.getPlayerList();
                this.gameState.setK(response.getK());
                this.gameState.setN(response.getMazeSize());
                this.version = response.getVersion();
                System.out.println("Received JOIN_RESPONSE from Tracker. Player list : " + players);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public TrackerMessage sendTrackerMessage(TrackerMessage message) throws RemoteException {
        try {
            TrackerMessage response = tracker.handleTrackerMessage(message);
            return response;
        } catch (RemoteException e) {
            e.printStackTrace();
            throw e;
        }
    }

}