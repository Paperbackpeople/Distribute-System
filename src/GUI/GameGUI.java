package GUI;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import Player.*;
import Client.GamerNode;

public class GameGUI extends JFrame {
    private final JLabel scoreLabel;
    private final JPanel mazePanel;
    private final String playerId;
    private final String playerAddress;
    private final PlayerInfo player;
    private final char[][] maze;
    private List<Treasure> treasures;
    List<PlayerInfo> players;
    private final int mazeSize = 15;
    private final int numTreasures = 10;
    

    public GameGUI(String playerId, String playerAddress, List<PlayerInfo> players) {
        this.playerId = playerId;
        this.playerAddress = playerAddress;
        this.players = players;

        setTitle("Maze Game - " + playerId);
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        scoreLabel = new JLabel("Score: 0");
        mazePanel = new JPanel(new GridLayout(mazeSize, mazeSize)); // 假设迷宫大小为15x15
        maze = new char[mazeSize][mazeSize];

        for(int i=0;i<15;i++){
            for(int j=0;j<15;j++){
                maze[i][j]=' ';
            }
        }

        player = new PlayerInfo(playerId, playerAddress);
        player.setPosition(0, 0);
        maze[0][0] = player.getPlayerId().charAt(0);

        //显示所有玩家的位置 
        updateGameState();

        add(scoreLabel, BorderLayout.NORTH);
        add(mazePanel, BorderLayout.CENTER);
    }
    public void initializeGameState(){
        System.out.println("Initializing game state for player: " + playerId);
        // Set initial player positions, treasures, etc.
        for (PlayerInfo p : players) {
            maze[p.getX()][p.getY()] = p.getPlayerId().charAt(0);
        }
        updateMaze();
    }

    public void updateGameState() {

        //从服务器获取最新的游戏状态
        
        /*
        for (PlayerInfo p : players) {
            maze[p.getX()][p.getY()] = p.getPlayerId().charAt(0);
        }
        */
        updateMaze();
    }

    public void movePlayer(int move) {
        try {
            if (move >= 1 && move <= 4) {
                System.out.println("Moving player " + playerId + " to direction " + move);
                movePlayer(move);
            } else if (move == 9) {
                exitGame();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int X = player.getX();
        int Y = player.getY();
        switch (move) {
            case 1: X = X-1; break; // Move left
            case 2: Y = Y+1; break; // Move down
            case 3: X = X+1; break; // Move right
            case 4: Y = Y-1; break; // Move up
        }
        // Check if new position is within bounds
        if (X >= 0 && X < mazeSize && Y >= 0 && Y < mazeSize) {
            // Check if player collects a treasure
            if (maze[X][Y] == '*') {
                player.incrementScore();
                updateScore(player.getScore());
                collectTreasure(X, Y);
                generateNewTreasure();
            }
            // Clear previous player position
            maze[player.getX()][player.getY()] = ' ';
            player.setPosition(X, Y);
            maze[X][Y] = player.getPlayerId().charAt(0);  // Set new position
            updateMaze();
        }
    }

    public void updateMaze() {
        mazePanel.removeAll();
        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                mazePanel.add(new JLabel(String.valueOf(maze[i][j])));
            }
        }
        mazePanel.revalidate();
        mazePanel.repaint();
    }

    public void exitGame() {
        System.out.println("Exiting game for player: " + playerId);
        dispose();  // Close the GUI
    }

    private void initializeTreasures() {
        treasures = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < numTreasures; i++) {
            int x, y;
            do {
                x = rand.nextInt(mazeSize);
                y = rand.nextInt(mazeSize);
            } while (maze[x][y] != ' ');  

            Treasure treasure = new Treasure(x, y);
            treasures.add(treasure);
            maze[x][y] = '*';  
        }
    }

    public void generateNewTreasure() {
        int x, y;
        do {
            x = (int)(Math.random() * mazeSize);
            y = (int)(Math.random() * mazeSize);
        } while (maze[x][y] != ' '); // 确保新宝藏生成在空位置
    
        maze[x][y] = '*'; // 在随机位置放置宝藏
    }    

    // 玩家收集宝藏后随机产生新宝藏
    private void collectTreasure(int x, int y) {
        for (Treasure treasure : treasures) {
            if (treasure.getX() == x && treasure.getY() == y) {
                treasures.remove(treasure);
                break;
            }
        }
        generateNewTreasure();
    }

    public void updateScore(int score) {
        scoreLabel.setText("Score: " + score);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PlayerInfo player1 = new PlayerInfo("P1", "127.0.0.1");
            player1.setPosition(0, 0);

            PlayerInfo player2 = new PlayerInfo("P2", "127.0.0.2");
            player2.setPosition(2, 2);

            List<PlayerInfo> players = List.of(player1, player2);

            GameGUI game = new GameGUI("P1", "127.0.0.1", players); 
            game.setVisible(true);
        });
    }
}

class Treasure {
    private int x;
    private int y;

    public Treasure(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
}