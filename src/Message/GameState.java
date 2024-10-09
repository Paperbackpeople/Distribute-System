package Message;

import Player.*;
import Treasure.*;
import Position.*;
import java.io.Serializable;
import java.util.*;


public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;
    private int mazeSize = 0;
    private int numTreasures = 0;
    private HashMap<Position, TreasureInfo> treasures;
    private List<PlayerInfo> players;
    private HashMap<String, Integer> playerScores;
    private HashMap<String, Position> playerPositions;
    private String[][]maze;

    public GameState() {
        this.players = new ArrayList<PlayerInfo>();
        this.treasures = new HashMap<>();
        this.maze = new String[mazeSize][mazeSize];
        this.playerPositions = new HashMap<>();
        this.playerScores = new HashMap<>();

    }

    public void initializeGameState() {
        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                maze[i][j] = " ";
            }
        }
        Random random = new Random();

        // 初始化玩家位置
        for (PlayerInfo player : players) {
            int x, y;
            // 随机生成位置，直到找到空白位置
            do {
                x = random.nextInt(mazeSize);
                y = random.nextInt(mazeSize);
            } while (!Objects.equals(maze[x][y], " "));
            if(player.getX() != -1 && player.getY() != -1){
                playerPositions.put(player.getPlayerId(), new Position(x, y));
                playerScores.put(player.getPlayerId(), 0);
            }
            maze[x][y] = player.getPlayerId();
        }

        initializeTreasures();
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

    public HashMap<String, Integer> getPlayerScores() {
        return playerScores;
    }

    public HashMap<String, Position> getPlayerPositions() {
        return playerPositions;
    }

    public void setPlayerScores(HashMap<String, Integer> playerScores) {
        this.playerScores = playerScores;
    }

    public void setPlayerPositions(HashMap<String, Position> playerPositions) {
        this.playerPositions = playerPositions;
    }

    public String[][] getMaze() {
        return maze;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfo> players) {
        this.players = players;
    }

    public void setTreasures(HashMap<Position, TreasureInfo> treasures) {
        this.treasures = treasures;
    }

    public HashMap<Position, TreasureInfo> getTreasures() {
        return treasures;
    }

    public void setMaze(String[][] maze) {
        this.maze = maze;
    }

    public int getMazeSize() {
        return mazeSize;
    }

    public void setMazeSize(int mazeSize) {
        this.mazeSize = mazeSize;
    }

    public void initializeTreasures() {
        for (int i = 0; i < numTreasures; i++) {
            generateNewTreasure();
        }
    }

    public void generateNewTreasure() {
        Random rand = new Random();
        int x, y;
        while (true) {
            x = rand.nextInt(mazeSize);
            y = rand.nextInt(mazeSize);
            if (Objects.equals(maze[x][y], " ")) {
                break;
            }
        }
        TreasureInfo treasure = new TreasureInfo(x, y);
        treasures.put(new Position(x, y), treasure);
        maze[x][y] = "*";  // 在迷宫中放置宝藏
    }

    // 玩家收集宝藏后随机产生新宝藏
    public void collectTreasure(int x, int y) {
        treasures.remove(new Position(x, y));
        generateNewTreasure();
    }

}