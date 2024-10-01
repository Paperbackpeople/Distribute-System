package GUI;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import Player.*;
//import Client.GamerNode;

public class GameGUI extends JFrame {
    private final JLabel scoreLabel;
    private final JPanel mazePanel;
    private final PlayerInfo player;
    private final char[][] maze;
    private List<Treasure> treasures;
    private List<PlayerInfo> players;
    private final int mazeSize = 15;
    private final int numTreasures = 10;
    

    public GameGUI(PlayerInfo player, List<PlayerInfo> players) {
        this.player = player;
        this.players = players;

        setTitle("Maze Game - " + player.getPlayerId());
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

        player.setPosition(0, 0);
        maze[0][0] = player.getPlayerId().charAt(0);
        
        initializeGameState();
        initializeTreasures();
        updateGameState();

        add(scoreLabel, BorderLayout.NORTH);
        add(mazePanel, BorderLayout.CENTER);
    }
    public void initializeGameState(){
        //从服务器获取最新的游戏状态，并初始化player的位置

        System.out.println("Initializing game state for player: " + player.getPlayerId());
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
        if (move >= 0 && move <= 4) {
            System.out.println("Moving player " + player.getPlayerId() + " to direction " + move);
        } else if (move == 9) {
            exitGame();
        }

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
                updateScore(player.getScore());
                collectTreasure(X, Y);
                generateNewTreasure();
            }
            // Clear previous player position
            maze[player.getX()][player.getY()] = ' ';
            player.setPosition(X, Y);
            maze[X][Y] = player.getPlayerId().charAt(0);  // Set new position
            updateMaze();
        }else {
            System.out.println("Invalid move: out of bounds");
        }
    }

    public void updateMaze() {
        mazePanel.removeAll();  // 清空当前组件
    
        // 使用 BorderLayout 以便更精确控制布局
        mazePanel.setLayout(new BorderLayout());
    
        // 设置字体为固定宽度，确保对齐
        Font monoFont = new Font("Monospaced", Font.PLAIN, 12);
        Dimension labelSize = new Dimension(30, 30);  // 每个格子30x30像素
    
        // 添加顶部边框
        JPanel topBorderPanel = new JPanel(new GridLayout(1, mazeSize + 2));  // 1行，mazeSize+2列（包括左右边框）
        topBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  // 左上角
        for (int j = 0; j < mazeSize; j++) {
            topBorderPanel.add(new JLabel("--", SwingConstants.CENTER));  // 水平边框
        }
        topBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  // 右上角
    
        // 添加顶部边框到 mazePanel
        mazePanel.add(topBorderPanel, BorderLayout.NORTH);
    
        // 创建迷宫主体（带左右边框）
        JPanel centerPanelWithBorders = new JPanel(new BorderLayout());
    
        // 左边框
        JPanel leftBorderPanel = new JPanel(new GridLayout(mazeSize, 1));
        for (int i = 0; i < mazeSize; i++) {
            leftBorderPanel.add(new JLabel("|", SwingConstants.CENTER));
        }
    
        // 迷宫主体
        JPanel mazeBodyPanel = new JPanel(new GridLayout(mazeSize, mazeSize));
        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                JLabel cellLabel = new JLabel(String.valueOf(maze[i][j]), SwingConstants.CENTER);
                cellLabel.setFont(monoFont);
                cellLabel.setPreferredSize(labelSize);
                mazeBodyPanel.add(cellLabel);  // 将迷宫格子添加到主体面板
            }
        }
    
        // 右边框
        JPanel rightBorderPanel = new JPanel(new GridLayout(mazeSize, 1));
        for (int i = 0; i < mazeSize; i++) {
            rightBorderPanel.add(new JLabel("|", SwingConstants.CENTER));
        }
    
        // 将左边框、迷宫主体、右边框组合
        centerPanelWithBorders.add(leftBorderPanel, BorderLayout.WEST);
        centerPanelWithBorders.add(mazeBodyPanel, BorderLayout.CENTER);
        centerPanelWithBorders.add(rightBorderPanel, BorderLayout.EAST);
    
        // 将组合好的迷宫主体添加到 mazePanel 中间
        mazePanel.add(centerPanelWithBorders, BorderLayout.CENTER);
    
        // 添加底部边框
        JPanel bottomBorderPanel = new JPanel(new GridLayout(1, mazeSize + 2));  // 1行，mazeSize+2列
        bottomBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  // 左下角
        for (int j = 0; j < mazeSize; j++) {
            bottomBorderPanel.add(new JLabel("--", SwingConstants.CENTER));  // 底部水平边框
        }
        bottomBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  // 右下角
    
        // 添加底部边框到 mazePanel
        mazePanel.add(bottomBorderPanel, BorderLayout.SOUTH);
    
        // 刷新面板
        mazePanel.revalidate();
        mazePanel.repaint();
    }
    
    
    

    public void exitGame() {
        System.out.println("Exiting game for player: " + player.getPlayerId());
        dispose();  // Close the GUI
    }

    public void initializeTreasures() {
        treasures = new ArrayList<>();
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
        Treasure treasure = new Treasure(x, y);
        treasures.add(treasure);
        maze[x][y] = '*';  
    }    

    // 玩家收集宝藏后随机产生新宝藏
    public void collectTreasure(int x, int y) {
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
            PlayerInfo player1 = new PlayerInfo("1", "127.0.0.1");
            player1.setPosition(0, 0);

            PlayerInfo player2 = new PlayerInfo("2", "127.0.0.2");
            player2.setPosition(2, 2);

            List<PlayerInfo> players = List.of(player1, player2);

            GameGUI game = new GameGUI(player1, players); 
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