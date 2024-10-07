package Message;

import Player.*;
import Treasure.*;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    private int mazeSize = 15; 
    private int numTreasures = 10; 
    private List<TreasureInfo> treasures;
    private List<PlayerInfo> players;
    private char[][]maze;
    private PlayerInfo primaryPlayerInfo;
    private PlayerInfo backuPlayerInfo;

    public GameState() {
        this.players = new ArrayList<PlayerInfo>();
        this.treasures = new ArrayList<TreasureInfo>();
        this.maze = new char[mazeSize][mazeSize];
        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                maze[i][j] = ' ';
            }
        }

    }

    public void initializeGameState() {
        Random random = new Random();
        
        // 初始化玩家位置
        for (PlayerInfo player : players) {
            int x, y;
            // 随机生成位置，直到找到空白位置
            do {
                x = random.nextInt(mazeSize);
                y = random.nextInt(mazeSize);
            } while (maze[x][y] != ' ');

            player.setPosition(x, y);
            player.setScore(0);
            maze[x][y] = player.getPlayerId().charAt(0);
        }

        initializeTreasures();

        if (players.size() > 0) {
            primaryPlayerInfo = players.get(0);
        }
        if (players.size() > 1) {
            backuPlayerInfo = players.get(1);
        }

        System.out.println("Game state initialized with " + players.size() + " players and " + numTreasures + " treasures.");
    }

    public int getN() {
        return mazeSize;
    }

    public int getK() {
        return numTreasures;
    }

    public void setN(int n) {
        this.mazeSize = n;
    }

    public void setK(int k) {
        this.numTreasures = k;
    }

    public void updateMaze(int x, int y, char content) {
        maze[x][y] = content;
    }

    public char[][] getMaze() {
        return maze;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    // 添加玩家到游戏状态
    public void addPlayer(PlayerInfo player) {
        players.add(player);
        updateMaze(player.getX(), player.getY(), player.getPlayerId().charAt(0));  // 在迷宫中放置玩家
    }

      // 更新玩家位置
    public void updatePlayerPosition(String playerId, int x, int y) {
        for (PlayerInfo player : players) {
            if (player.getPlayerId().equals(playerId)) {
                updateMaze(player.getX(), player.getY(), ' '); 
                player.setPosition(x, y); 
                updateMaze(x, y, playerId.charAt(0)); 
                break;
            }
        }
    }

    public void movePlayer(PlayerInfo player, int move) {
        int X = player.getX();
        int Y = player.getY();
        switch (move) {
            case 0: break;
            case 1: X = X-1; break; // Move left
            case 2: Y = Y+1; break; // Move down
            case 3: X = X+1; break; // Move right
            case 4: Y = Y-1; break; // Move up
            default: return;
        }
        // Check if new position is within bounds
        if (X >= 0 && X < mazeSize && Y >= 0 && Y < mazeSize) {
            // Check if player collects a treasure
            if (maze[X][Y] == '*') {
                player.incrementScore();
                collectTreasure(X, Y);
            }else if(maze[X][Y] != ' '){
                System.out.println("Invalid move: This cell is already occupied.");
                return;
            }
            // Clear previous player position
            maze[player.getX()][player.getY()] = ' ';
            player.setPosition(X, Y);
            maze[X][Y] = player.getPlayerId().charAt(0);  // Set new position
        }else {
            System.out.println("Invalid move: out of bounds");
        }
    }

    public void updatePlayerScore(String playerId) {
        for (PlayerInfo player : players) {
            if (player.getPlayerId().equals(playerId)) {
                player.incrementScore();  // 更新玩家分数
                break;
            }
        }
    }

    // 移除玩家
    public void removePlayer(String playerId) {
        for (PlayerInfo player : players) {
            if (player.getPlayerId().equals(playerId)) {
                updateMaze(player.getX(), player.getY(), ' ');
                break;
            }
        }
        players.removeIf(player -> player.getPlayerId().equals(playerId));
    }

    // 设置主节点和备份节点
    public void setPrimaryNode(PlayerInfo primaryNode) {
        this.primaryPlayerInfo = primaryNode;
    }

    public void setBackupNode(PlayerInfo backupNode) {
        this.backuPlayerInfo = backupNode;
    }

    public PlayerInfo getPrimaryNode() {
        return primaryPlayerInfo;
    }

    public PlayerInfo getBackupNode() {
        return backuPlayerInfo;
    }

    public void initializeTreasures() {
        for (int i = 0; i < numTreasures; i++) {
              generateNewTreasure();
        }
    }

    public void generateNewTreasure() {
        Random rand = new Random();
        int x, y;
        do {
            x = rand.nextInt(mazeSize);
            y = rand.nextInt(mazeSize);
        } while (maze[x][y] != ' ');  
        TreasureInfo treasure = new TreasureInfo(x, y);
        treasures.add(treasure);
        maze[x][y] = '*';  
    }    

    // 玩家收集宝藏后随机产生新宝藏
    public void collectTreasure(int x, int y) {
        treasures.removeIf(treasure -> treasure.getX() == x && treasure.getY() == y);
        generateNewTreasure();
    }

    // 获取宝藏位置列表
    public List<TreasureInfo> getTreasures() {
        return treasures;
    }
    
}
