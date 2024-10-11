package Client;
import GUI.GameGUI;
import Interface.GameNodeInterface;
import Interface.TrackerCommunicationInterface;
import Interface.TrackerInterface;
import Position.Position;
import Message.ElectionMessage;
import Message.GameState;
import Message.GossipMessage;
import Message.TrackerMessage;
import Player.PlayerInfo;
import Treasure.TreasureInfo;

import javax.sound.midi.Soundbank;
import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLOutput;
import java.time.LocalTime;
import java.util.*;
import java.util.logging.Logger;

public class GamerNode implements GameNodeInterface, TrackerCommunicationInterface {
    private Thread normalGossipThread;
    private final Object gameStateLock = new Object();
    private final Object playersLock = new Object();
    private final Object crashedPlayersLock = new Object();
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
    private GameState gameState;
    private PlayerInfo playerInfo;
    private Scanner scanner;
    private boolean running;
    private GameGUI gameGUI;
    // 添加状态标志，指示是否已经处理过主节点崩溃
    private volatile boolean primaryCrashHandled = false;

    private volatile boolean primaryGossipStarted = false;

    public GamerNode(String trackerIp, int trackerPort, String playerId, Scanner scanner) {
        this.playerId = playerId;
        this.scanner = scanner;
        this.running = true;
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
            System.out.println("GamerNode " + playerId + " is ready to gossip!");
            if (isPrimary) {
                gameState = new GameState(15, 10);
                gameState.setPlayers(updatedPlayers);
                gameState.initializeGameState();
                gameState.setPrimaryNode(playerInfo.getPlayerId());
                //localtime
                gameState.setStartTime(LocalTime.now());
                System.out.println(gameState);
                // 如果节点是主节点，启动主节点的 Gossip 线程
                startPrimaryGossip();
            } else {
                // 如果不是主节点，检查是否已经启动了主节点的 Gossip 线程
                if (!primaryGossipStarted) {
                    startGossip();
                }
            }
            startPinging();
            Thread.sleep(200);
            // Initialize the GUI after setting up the game state
            gameGUI = new GameGUI(playerId, gameState);
            SwingUtilities.invokeLater(() -> gameGUI.setVisible(true));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startPrimaryGossip() {
        if (primaryGossipStarted) {
            // 如果已经启动过，不再重复启动
            return;
        }
        primaryGossipStarted = true;
        System.out.println("启动 Primary Gossip 协议");
        // 启动 Gossip 协议线程
        new Thread(() -> {
            while (running) {
                try {
                    if (gameState != null) {
                        sendTrackerMessage(new TrackerMessage("UPDATE", updatedPlayers, gameState.getN(), gameState.getK(), this.playerInfo));
                    }
                    if (backupNode != null) {
                        try {
                            sendGossipMessagePrimary(backupNode, true);
                        } catch (Exception e) {
                            System.out.println("无法向备份节点发送 Gossip 消息，备份节点可能已崩溃：" + backupNode.getPlayerId());
                            handleBackupNodeCrash();
                        }
                    } else {
//                        System.out.println("没有备份节点可发送 Gossip 消息。尝试选择新的备份节点...");
                        selectNewBackup();
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println("主节点发生异常：" + e.getMessage());
                    // 根据需要处理其他异常
                }
            }
        }).start();
    }
    private void handleBackupNodeCrash() {
        if (backupNode == null) {
            // 已经没有备份节点，无需处理
            return;
        }
        System.out.println("处理备份节点崩溃：" + backupNode.getPlayerId());
        // 先保存备份节点的信息
        String backupNodeId = backupNode.getPlayerId();
        int backupNodeX = backupNode.getX();
        int backupNodeY = backupNode.getY();

        // 从 players 列表中移除崩溃的备份节点
        synchronized (playersLock) {
            players.removeIf(player -> player.getPlayerId().equals(backupNodeId));
        }
        // 从 updatedPlayers 列表中移除
        synchronized (updatedPlayers) {
            updatedPlayers.removeIf(player -> player.getPlayerId().equals(backupNodeId));
        }
        // 将备份节点添加到 crashedPlayers 列表中
        synchronized (crashedPlayersLock) {
            boolean alreadyCrashed = crashedPlayers.stream()
                    .anyMatch(player -> player.getPlayerId().equals(backupNodeId));
            if (!alreadyCrashed) {
                crashedPlayers.add(backupNode);
            }
        }
        // 更新 gameState
        synchronized (gameStateLock) {
            gameState.getPlayerPositions().remove(backupNodeId);
            gameState.getPlayerScores().remove(backupNodeId);
            gameState.getMaze()[backupNodeX][backupNodeY] = " ";
        }
        backupNode = null;
        // 尝试选择新的备份节点
        selectNewBackup();
    }
    public void startNormalGossip() {
        normalGossipThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (isPrimary || isBackup) {
                        // 如果节点变为主节点或备份节点，退出线程
                        break;
                    }
                    PlayerInfo randomPlayer = getPlayerInfo();
                    if (randomPlayer != null) {
                        sendGossipMessage(randomPlayer);
                    }
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // 线程被中断，退出循环
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        normalGossipThread.start();
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

    // 启动游戏
    public void start() {
        // 启动输入线程
        Thread inputThread = new Thread(new InputHandler());
        inputThread.start();

    }

    // 处理用户输入的内部类
    private class InputHandler implements Runnable {
        @Override
        public void run() {
            while (running) {
                System.out.print("请输入您的移动 (0: refresh, 1: left, 2: down, 3: right, 4: up, 9: exit): ");
                String input = scanner.nextLine();
                try {
                    int move = Integer.parseInt(input);
                    handleMove(move);
                } catch (NumberFormatException e) {
                    System.out.println("无效的输入，请输入数字。");
                }
            }
        }
    }


    private synchronized void handleMove(int move) {
        try {
            if (move >= 0 && move <= 4) {
                movePlayer(move);
                if (isPrimary){
                    synchronized (gameState) {
                        gameState.setPlayers(updatedPlayers);
                        Position curplayerPosition = gameState.getPlayerPositions().getOrDefault(playerId, new Position(-1, -1));
                        if(curplayerPosition.getX() != -1 && curplayerPosition.getY() != -1){
                            if(Objects.equals(gameState.getMaze()[curplayerPosition.getX()][curplayerPosition.getY()], "*")){
                                gameState.getPlayerScores().put(playerId, gameState.getPlayerScores().get(playerId) + 1);
                                gameState.getTreasures().remove(new Position(curplayerPosition.getX(), curplayerPosition.getY()));
                                gameState.generateNewTreasure();
                            }else{
                                gameState.getMaze()[curplayerPosition.getX()][curplayerPosition.getY()] = playerId;
                            }
                            gameState.getPlayerPositions().put(playerId, curplayerPosition);
                            gameState.getMaze()[curplayerPosition.getX()][curplayerPosition.getY()] = playerId;
                        }
                    }
                    System.out.println(gameState);
                }else {
                    sendGossipMessageMove(primaryNode);
                }
                } else if (move == 9) {
                exitGame();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> gameGUI.updateGameState(this.gameState));
    }


    private void exitGame() {
        try {
            // 首先关闭 GUI 界面
            if (gameGUI != null) {
                SwingUtilities.invokeLater(() -> gameGUI.exitGame());
            }
            // 然后退出程序
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void movePlayer(int move) {
        int X = -1;
        int Y = -1;
        if(isPrimary){
            X = gameState.getPlayerPositions().get(playerId).getX();
            Y = gameState.getPlayerPositions().get(playerId).getY();
            gameState.getMaze()[X][Y] = " ";
        }else{
            X = playerInfo.getX();
            Y = playerInfo.getY();
        }
        switch (move) {
            case 0: break;
            case 1: Y = Y-1; break; // Move left
            case 2: X = X+1; break; // Move down
            case 3: Y = Y+1; break; // Move right
            case 4: X = X-1; break; // Move up
            default: return;
        }
        // Check if new position is within bounds
        if (X >= 0 && X < gameState.getMazeSize() && Y >= 0 && Y < gameState.getMazeSize()) {
            if(isPrimary) {
                gameState.getPlayerPositions().put(playerId, new Position(X, Y));
                System.out.println("Player " + playerId + " moved to position (" + X + ", " + Y + ")");
            }else{
                playerInfo.setPosition(X, Y);
                System.out.println("Player " + playerId + " moved to position (" + X + ", " + Y + ")");
            }
        }else {
            System.out.println("Invalid move: out of bounds");
        }
    }

    public void receiveGossipMessagePrimary(GossipMessage message){
        boolean changed = false; // 标记是否有更新或移除操作
        boolean moved = (message.getGameState() != null);
        if (message.getMessageType() == GossipMessage.MessageType.BACKUP_SYNC) {
            handleBackupSyncMessage(message);
            return;
        }
        if(message.getMessageType() == GossipMessage.MessageType.JOIN){
            moved = true;
        }
        synchronized (players) {
            for (PlayerInfo player : message.getUpdatedPlayers()) {
                boolean playerExists = players.stream()
                        .anyMatch(p -> p.getPlayerId().equals(player.getPlayerId()));
                boolean updated = updatedPlayers.stream()
                        .anyMatch(p -> p.getPlayerId().equals(player.getPlayerId()));
                if (!playerExists && !player.getPlayerId().equals(this.playerId) && !player.getPlayerId().isEmpty()) {
                    players.add(player);
                    changed = true;
                }
                if(!updatedPlayers.contains(player) && !updated && !crashedPlayers.contains(player)){
                    updatedPlayers.add(player);
                    moved = true;
                    initiateNormalPlayerPosition(player);
                    changed = true;

                }
            }
        }


        if(message.getBackupNode() != null) {
            if (this.backupNode == null || crashedPlayers.contains(this.backupNode)) {
                this.backupNode = message.getBackupNode();
                gameState.setBackupNode(backupNode.getPlayerId());
                changed = true;
            }
        }
        // 处理崩溃的玩家信息
        if (message.getCrashedPlayers() != null && !message.getCrashedPlayers().isEmpty()) {
            synchronized (players) {
                for (PlayerInfo crashedPlayer : message.getCrashedPlayers()) {
                    String crashedPlayerId = crashedPlayer.getPlayerId();
                    boolean removed = players.removeIf(player -> player.getPlayerId().equals(crashedPlayerId));
                    boolean removedUpdated = updatedPlayers.removeIf(player -> player.getPlayerId().equals(crashedPlayerId));
                    boolean alreadyCrashed = crashedPlayers.stream()
                            .anyMatch(player -> player.getPlayerId().equals(crashedPlayerId));
                    if (!alreadyCrashed) {
                        synchronized (crashedPlayers) {
                            crashedPlayers.add(crashedPlayer);
                            updateGameStateAfterCrash(crashedPlayerId);
                        }
                    }
                    if (removed) {
                        changed = true;
                    }
                    if (removedUpdated) {
                        changed = true;
                    }
                    if (crashedPlayerId.equals(backupNode.getPlayerId())) {
                        System.out.println("Backup node has crashed (via gossip). Selecting new backup...");
                        selectNewBackup();
                    }

                }
            }
            {

            }        }
        // 3. 如果有任何更改，且消息中的版本号高于当前版本，更新版本号
        if (changed) {
            this.version++;
            System.out.println("Updated version to: " + version);
        }
        if (moved){
            synchronized (gameStateLock) {
                String senderId = message.getSenderInfo().getPlayerId();
                System.out.println("updatePlayer" + updatedPlayers);
                gameState.setPlayers(updatedPlayers);
                if(backupNode != null){
                    gameState.setBackupNode(backupNode.getPlayerId());
                }
                Position lastplayerPosition = gameState.getPlayerPositions().get(senderId);
                Position curplayerPosition = new Position(message.getSenderInfo().getX(), message.getSenderInfo().getY());
                if(curplayerPosition.getX() != -1 && curplayerPosition.getY() != -1){
                    if(Objects.equals(gameState.getMaze()[curplayerPosition.getX()][curplayerPosition.getY()], "*")){
                        gameState.getPlayerScores().put(senderId, gameState.getPlayerScores().get(senderId) + 1);
                        gameState.getTreasures().remove(new Position(curplayerPosition.getX(), curplayerPosition.getY()));
                        gameState.getMaze()[lastplayerPosition.getX()][lastplayerPosition.getY()] = " ";
                        gameState.generateNewTreasure();
                    }else {
                        gameState.getMaze()[lastplayerPosition.getX()][lastplayerPosition.getY()] = " ";
                    }
                    gameState.getPlayerPositions().put(senderId, curplayerPosition);
                    gameState.getPlayers().forEach(player -> {
                        if(player.getPlayerId().equals(senderId)){
                            player.setPosition(curplayerPosition.getX(), curplayerPosition.getY());
                        }
                    });
                    gameState.getMaze()[curplayerPosition.getX()][curplayerPosition.getY()] = senderId;
                }
            }
        }
        sendGossipMessagePrimary(message.getSenderInfo(), moved);
    }
    private void handleBackupSyncMessage(GossipMessage message) {
        synchronized (gameStateLock) {
            // 更新游戏状态为备份节点发送过来的状态
            this.gameState = new GameState(message.getGameState());
            this.primaryNode = this.playerInfo;
            this.backupNode = message.getBackupNode();
            this.version = message.getVersion();
            // 更新本地玩家信息
            this.playerInfo.setPosition(
                    gameState.getPlayerPositions().get(playerId).getX(),
                    gameState.getPlayerPositions().get(playerId).getY()
            );
            System.out.println("Primary node updated game state from backup node's GossipMessage.");
        }
        synchronized (playersLock) {
            // 更新玩家列表
            for (PlayerInfo player : message.getUpdatedPlayers()) {
                if (!players.contains(player) && !player.getPlayerId().equals(this.playerId)) {
                    players.add(player);
                }
            }
            this.updatedPlayers = new ArrayList<>(message.getUpdatedPlayers());
        }
        synchronized (crashedPlayersLock) {
            for (PlayerInfo crashedPlayer : message.getCrashedPlayers()) {
                if (!crashedPlayers.contains(crashedPlayer)) {
                    crashedPlayers.add(crashedPlayer);
                    updateGameStateAfterCrash(crashedPlayer.getPlayerId());
                }
            }        }
        version++;
    }

    private void updateGameStateAfterCrash(String crashedPlayerId) {
        synchronized (gameStateLock) {
            // 获取崩溃玩家的位置
            Position crashedPosition = gameState.getPlayerPositions().get(crashedPlayerId);

            if (crashedPosition != null) {
                int x = crashedPosition.getX();
                int y = crashedPosition.getY();

                // 移除崩溃玩家的位置信息和得分
                gameState.getPlayerPositions().remove(crashedPlayerId);
                gameState.getPlayerScores().remove(crashedPlayerId);

                // 检查是否有其他玩家在同一位置
                String otherPlayerId = " ";
                for (Map.Entry<String, Position> entry : gameState.getPlayerPositions().entrySet()) {
                    if (entry.getValue().getX() == x && entry.getValue().getY() == y) {
                        otherPlayerId = entry.getKey();
                        break; // 找到第一个玩家即可
                    }
                }
                System.out.println("Player " + crashedPlayerId + " has crashed. Removing from position (" + x + ", " + y + ")");
                // 更新迷宫中的位置
                gameState.getMaze()[x][y] = otherPlayerId;
            }
        }
    }

    public void initiateNormalPlayerPosition(PlayerInfo player){
        Random random = new Random();
        int x, y;
        // 随机生成位置，直到找到空白位置
        do {
            x = random.nextInt(gameState.getMazeSize());
            y = random.nextInt(gameState.getMazeSize());
        } while (!Objects.equals(gameState.getMaze()[x][y], " "));
        if(player.getX() == -1 && player.getY() == -1){
            gameState.getPlayerPositions().put(player.getPlayerId(), new Position(x, y));
            gameState.getPlayerScores().put(player.getPlayerId(), 0);
            player.setPosition(x, y);
        }
        gameState.getMaze()[x][y] = player.getPlayerId();
    }
    public void receiveGossipMessageNormal(GossipMessage message){
        if (message.getVersion() > this.version) {
            this.primaryNode = message.getPrimaryNode();
            this.backupNode = message.getBackupNode();
            this.version = message.getVersion();
            this.updatedPlayers = message.getUpdatedPlayers();
            this.crashedPlayers = message.getCrashedPlayers();
            players = message.getUpdatedPlayers();
        }
        if(message.getGameState() != null){
            synchronized (gameStateLock) {
                gameState = new GameState(message.getGameState());

                playerInfo.setPosition(gameState.getPlayerPositions().get(playerId).getX(), gameState.getPlayerPositions().get(playerId).getY());
            }
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
                    // 如果崩溃的节点是主服务器或备份服务器，启动相应的处理
                    if (crashedPlayerId.equals(primaryNode.getPlayerId())) {
                        updatedPlayers.removeIf(player -> player.getPlayerId().equals(primaryNode.getPlayerId()));
                        players.removeIf(player -> player.getPlayerId().equals(primaryNode.getPlayerId()));
                        primaryNode = null;

                    }
                }
            }
        }
        if(message.getUpdatedPlayers().size() == 1 && message.getCrashedPlayers().isEmpty()){
            sendGossipMessagetoNew(message.getSenderInfo());
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
            node.receiveGossipMessage(message);
        } catch (Exception e) {
            System.out.println("Failed to send GossipMessage to node " + targetNode.getPlayerId() );
        }
    }
    public void sendGossipMessageMove(PlayerInfo targetNode) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + targetNode.getPlayerId());
            GossipMessage message = new GossipMessage(primaryNode, backupNode, version, updatedPlayers, crashedPlayers, this.playerInfo, gameState);
            node.receiveGossipMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendGossipMessagetoNew(PlayerInfo targetNode) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + targetNode.getPlayerId());
            GossipMessage message = new GossipMessage(primaryNode, backupNode, 0, updatedPlayers, crashedPlayers, this.playerInfo, null);
            System.out.println("我的信息"+message + "发送给" + targetNode.getPlayerId());
            node.receiveGossipMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void sendGossipMessagePrimary(PlayerInfo targetNode, boolean moved) {
        if (targetNode == null) {
            System.out.println("备份节点为 null，无法发送 Gossip 消息。");
            return;
        }
        try {
            GossipMessage message;
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + targetNode.getPlayerId());
            if (moved) {
                message = new GossipMessage(primaryNode, backupNode, version, updatedPlayers, crashedPlayers, this.playerInfo, gameState);
            } else {
                message = new GossipMessage(primaryNode, backupNode, version, updatedPlayers, crashedPlayers, this.playerInfo, null);
            }
            node.receiveGossipMessage(message);
        } catch (Exception e) {
            System.out.println("无法向备份节点发送 Gossip 消息，备份节点可能已崩溃：" + targetNode.getPlayerId());
            handleBackupNodeCrash();
        }
    }

    // 简单的 ping 方法
    @Override
    public void ping() {
    }
    @Override
    public void becomePrimary() throws RemoteException {
        isPrimary = true;
        primaryNode = this.playerInfo;
        isBackup = false;
        System.out.println("此节点已成为主服务器。");

        // 中断 normal gossip 线程
        if (normalGossipThread != null && normalGossipThread.isAlive()) {
            normalGossipThread.interrupt();
        }
        // 启动主节点特定的任务
        startPrimaryGossip();
    }
    @Override
    public void becomeBackup() throws RemoteException {
        becomeBackupLocal();
    }

    private void becomeBackupLocal() {
        isBackup = true;
        backupNode = this.playerInfo;
        isPrimary = false;
        System.out.println("此节点已成为备份服务器。");
    }

    private void selectNewBackup() {
        synchronized (gameStateLock) {
            // 收集备份候选者：排除主节点、当前备份节点和自己
            List<PlayerInfo> candidates = new ArrayList<>();
            for (PlayerInfo player : players) {
                if (!player.getPlayerId().equals(primaryNode.getPlayerId()) &&
                        (backupNode == null || !player.getPlayerId().equals(backupNode.getPlayerId())) &&
                        !player.getPlayerId().equals(this.playerId)) { // 排除主节点、当前备份和自己
                    candidates.add(player);
                }
            }

            if (candidates.isEmpty()) {
//                System.out.println("没有可用的备份候选者。");
                backupNode = null;
                return;
            }

            // 随机打乱候选者列表
            Collections.shuffle(candidates);

            for (PlayerInfo candidate : candidates) {
                try {
                    // 假设所有节点都在 localhost 且使用相同的 RMI 端口 1099
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    GameNodeInterface node = (GameNodeInterface) registry.lookup("GameNode_" + candidate.getPlayerId());
                    node.ping(); // 发送 ping 请求

                    // 如果 ping 成功，设置为新的备份节点
                    backupNode = candidate;
                    System.out.println("新的备份节点已选择: " + candidate.getPlayerId());

                    // 通知选定的节点成为备份
                    node.becomeBackup();
                    System.out.println("已通知 " + candidate.getPlayerId() + " 成为新的备份节点。");

                    // 备份节点已选择，退出方法
                    return;

                } catch (Exception e) {
                    System.out.println("无法连接或 ping 备份候选者 " + candidate.getPlayerId() + ": " + e.getMessage());
                    // 尝试下一个候选者
                }
            }

            System.out.println("没有存活的备份候选者可用。");
            backupNode = null;
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

    private void handlePlayerCrash(PlayerInfo crashedPlayer) throws RemoteException {

        System.out.println("Player " + crashedPlayer.getPlayerId() + " has crashed.");

        // 从 players 列表中移除崩溃的节点
        synchronized (playersLock) {
            players.removeIf(player -> player.getPlayerId().equals(crashedPlayer.getPlayerId()));
        }

        // 将崩溃的节点添加到 crashedPlayers 列表中
        synchronized (crashedPlayersLock) {
            boolean alreadyCrashed = crashedPlayers.stream()
                    .anyMatch(player -> player.getPlayerId().equals(crashedPlayer.getPlayerId()));
            if (!alreadyCrashed) {
                crashedPlayers.add(crashedPlayer);
            }
        }
        updateGameStateAfterCrash(crashedPlayer.getPlayerId());

        System.out.println("Crashed players: " + crashedPlayers);
        if (primaryNode != null && crashedPlayer.getPlayerId().equals(primaryNode.getPlayerId())) {
            System.out.println("主节点已崩溃。通知备份节点...");
            if (!isPrimaryNodeAlive()) {
                System.out.println("主节点确实不可达，通知备份节点...");
                informBackupNodePrimaryCrashed();
            } else {
                System.out.println("主节点仍然存活，忽略错误的崩溃检测。");
            }
        } else if (backupNode != null && crashedPlayer.getPlayerId().equals(backupNode.getPlayerId())) {
            System.out.println("Backup node has crashed. Selecting new backup...");
            selectNewBackup();
        }
    }
    private void informBackupNodePrimaryCrashed() throws RemoteException {
        if (backupNode != null) {
            if (backupNode.getPlayerId().equals(this.playerId)) {
                // 备份节点是自己，直接处理主节点崩溃
                System.out.println("备份节点是自己，直接处理主节点崩溃。");
                primaryCrashHandled = false; // 重置处理标志，以便能处理主节点崩溃
                primaryNodeCrashed();
            } else {
                try {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    GameNodeInterface backupNodeStub = (GameNodeInterface) registry.lookup("GameNode_" + backupNode.getPlayerId());
                    backupNodeStub.primaryNodeCrashed();
                } catch (Exception e) {
                    System.out.println("无法通知备份节点：" + e.getMessage());
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("没有备份节点可通知主节点崩溃。");
        }
    }
    @Override
    public void primaryNodeCrashed() throws RemoteException {
        if (isBackup) {
            synchronized (this) {
                if (!primaryCrashHandled) {
                    // 首先尝试 ping 主节点
                    if (!isPrimaryNodeAlive()) {
                        primaryCrashHandled = true;
                        handlePrimaryNodeCrash();
                    } else {
                        System.out.println("主节点仍然存活，忽略错误的崩溃通知。");
                    }
                } else {
                    System.out.println("主节点崩溃已在处理中，忽略重复的通知。");
                }
            }
        }
    }
    private void handlePrimaryNodeCrash() {
        synchronized (this) {
            System.out.println("备份节点检测到主节点崩溃。正在尝试选举新的主节点...");

            // 从 updatedPlayers 中选择候选节点（排除自己和已崩溃的节点）
            List<PlayerInfo> candidates = new ArrayList<>(updatedPlayers);
            candidates.removeIf(player -> player.getPlayerId().equals(this.playerId)); // 移除自己
            candidates.removeIf(player -> crashedPlayers.stream()
                    .anyMatch(cp -> cp.getPlayerId().equals(player.getPlayerId()))); // 移除已崩溃的节点

            boolean newPrimaryElected = false;

            if (!candidates.isEmpty()) {
                Collections.shuffle(candidates);
                for (PlayerInfo candidate : candidates) {
                    try {
                        Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                        GameNodeInterface candidateStub = (GameNodeInterface) registry.lookup("GameNode_" + candidate.getPlayerId());
                        candidateStub.ping();
                        // 候选节点存活，设置为新的主节点
                        candidateStub.becomePrimary();
                        // 现在将信息同步到新的主节点
                        primaryNode = candidate;
                        GossipMessage message = new GossipMessage(
                                primaryNode,
                                backupNode,
                                version,
                                updatedPlayers,
                                crashedPlayers,
                                this.playerInfo,
                                gameState,
                                GossipMessage.MessageType.BACKUP_SYNC
                        );
                        candidateStub.receiveGossipMessage(message);
                        System.out.println("已选举新的主节点：" + candidate.getPlayerId());
                        newPrimaryElected = true;
                        break;
                    } catch (Exception e) {
                        System.out.println("无法联系候选节点 " + candidate.getPlayerId() + "：" + e.getMessage());
                        // 尝试下一个候选节点
                    }
                }
            } else {
                System.out.println("没有可用的候选节点可选为新的主节点。");
            }

            if (!newPrimaryElected) {
                // 如果没有成功选举新的主节点，备份节点自身升级为主节点
                try {
                    System.out.println("备份节点将自身升级为主节点。");
                    backupNode = null;
                    becomePrimary();
                    // 更新 primaryNode 为自身
                    primaryNode = this.playerInfo;
                    // 通知其他节点新的主节点信息
                } catch (Exception e) {
                    System.out.println("备份节点升级为主节点时发生错误：" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    private boolean isPrimaryNodeAlive() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            GameNodeInterface primaryNodeStub = (GameNodeInterface) registry.lookup("GameNode_" + primaryNode.getPlayerId());
            primaryNodeStub.ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    private void requestPrimaryNodeInfoAndNotifyJoin() {
        players.removeIf(player -> player.getPlayerId().equals(this.playerId)); // 排除自己
        if (players.isEmpty()) {
            updatedPlayers.add(this.playerInfo);
            try {
                becomePrimary();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
            System.out.println("No existing nodes found. This node becomes the primary node.");
            return;
        }
        int nodesToContact = Math.min(3, players.size()); // 最多联系3个节点
        Collections.shuffle(players);
        List<PlayerInfo> nodesToInteract = players.subList(0, nodesToContact);
        System.out.println("Nodes to interact: " + nodesToInteract);
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
                try {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                    GameNodeInterface primaryStub = (GameNodeInterface) registry.lookup("GameNode_" + primaryNode.getPlayerId());
                    GossipMessage message = new GossipMessage(
                            primaryNode, // 旧的主节点（已崩溃）
                            backupNode,
                            version,
                            updatedPlayers,
                            crashedPlayers,
                            this.playerInfo,
                            gameState,
                            GossipMessage.MessageType.JOIN
                    );
                    primaryStub.receiveGossipMessage(message);
                } catch (Exception e) {
                    System.out.println("无法联系主节点：" + e.getMessage());
                }
                System.out.println(gameState);
                System.out.println(playerInfo);
                if (backupNode == null && !isBackup && !isPrimary && primaryNode != null) {
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
                    this.playerInfo
            );

            // 发送 TrackerMessage 并接收响应
            TrackerMessage response = sendTrackerMessage(joinMessage);

            if ("JOIN_RESPONSE".equals(response.getMessageType())) {
                // 处理 JOIN_RESPONSE
                this.players = response.getPlayerList();
                System.out.println("Received JOIN_RESPONSE from Tracker. Player list : " + players);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public TrackerMessage sendTrackerMessage(TrackerMessage message) throws RemoteException {
        try {
            return tracker.handleTrackerMessage(message);
        } catch (RemoteException e) {
            e.printStackTrace();
            throw e;
        }
    }

}