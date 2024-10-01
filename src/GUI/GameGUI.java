package GUI;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
    private String mainServerPlayer = "P1";  
    private String backupServerPlayer = "P2"; 
    private final LocalTime startTime; 
    

    public GameGUI(PlayerInfo player, List<PlayerInfo> players) {
        this.player = player;
        this.players = players;
        this.startTime = LocalTime.now();

        setTitle("Maze Game - " + player.getPlayerId());
        setSize(600, 500);
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
            }else if(maze[X][Y] != ' '){
                System.out.println("Invalid move: This cell is already occupied.");
                return;
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
        mazePanel.removeAll();  
        mazePanel.setLayout(new BorderLayout());
        Font monoFont = new Font("Monospaced", Font.PLAIN, 12);
        Dimension labelSize = new Dimension(30, 30); 
        JPanel leftInfoPanel = new JPanel();
        leftInfoPanel.setLayout(new BoxLayout(leftInfoPanel, BoxLayout.Y_AXIS)); 
        leftInfoPanel.setPreferredSize(new Dimension(150, 400));  // 设置左侧面板的宽度
    
        // 添加所有玩家的得分
        leftInfoPanel.add(new JLabel("Player Scores:"));
        for (PlayerInfo p : players) {
            leftInfoPanel.add(new JLabel(p.getPlayerId() + ": " + p.getScore()));  // 显示每个玩家的得分
        }
        // 添加主服务器和备份服务器信息
        leftInfoPanel.add(new JLabel("Main Server: " + mainServerPlayer));
        leftInfoPanel.add(new JLabel("Backup Server: " + backupServerPlayer));
        // 添加游戏开始时间
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        leftInfoPanel.add(new JLabel("Game Start Time: " + startTime.format(timeFormatter)));

        mazePanel.add(leftInfoPanel, BorderLayout.WEST);
    
        // 顶部边框
        JPanel topBorderPanel = new JPanel(new GridLayout(1, mazeSize + 2)); 
        topBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  // 左上角
        for (int j = 0; j < mazeSize; j++) {
            topBorderPanel.add(new JLabel("--", SwingConstants.CENTER));  // 水平边框
        }
        topBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  // 右上角
        // 添加顶部边框
        mazePanel.add(topBorderPanel, BorderLayout.NORTH);
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

        JPanel rightBorderPanel = new JPanel(new GridLayout(mazeSize, 1));
        for (int i = 0; i < mazeSize; i++) {
            rightBorderPanel.add(new JLabel("|", SwingConstants.CENTER));
        }
    
        JPanel bottomBorderPanel = new JPanel(new GridLayout(1, mazeSize + 2)); 
        
        bottomBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  // 左下角
        for (int j = 0; j < mazeSize; j++) {
            bottomBorderPanel.add(new JLabel("--", SwingConstants.CENTER));  // 底部水平边框
        }
        bottomBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  // 右下角

        JPanel combinedPanel = new JPanel(new BorderLayout());
        combinedPanel.add(leftBorderPanel, BorderLayout.WEST);
        combinedPanel.add(mazeBodyPanel, BorderLayout.CENTER);
        combinedPanel.add(rightBorderPanel, BorderLayout.EAST);
        combinedPanel.add(topBorderPanel, BorderLayout.NORTH);
        combinedPanel.add(bottomBorderPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(50, 400)); 

        mazePanel.add(combinedPanel, BorderLayout.CENTER);
        mazePanel.add(rightPanel, BorderLayout.EAST);    
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

    public void processInput(){
        Scanner scanner = new Scanner(System.in);
        String command;
        
        while (true) {
            System.out.print("> ");
            command =  scanner.nextLine().trim();
            try {
                int move = Integer.parseInt(command);
                if ( move>= 0 && move <= 4) {
                    System.out.println("Moving player " + player.getPlayerId() + " to direction " + move);
                    movePlayer(move);
                } else if (move == 9) {
                    exitGame();
                    System.out.println("Player has exited the game.");
                    return;  // 退出循环，结束程序
                }else{
                    System.out.println("Invalid command. Use 0, 1, 2, 3, 4, or 9.");
                }
            } catch (NumberFormatException e) {
                // 捕获无法转换为整数的输入
                System.out.println("Invalid input. Please enter a valid number (0, 1, 2, 3, 4, or 9).");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PlayerInfo player1 = new PlayerInfo("A", "127.0.0.1");
            player1.setPosition(0, 0);

            PlayerInfo player2 = new PlayerInfo("B", "127.0.0.2");
            player2.setPosition(2, 2);

            List<PlayerInfo> players = List.of(player1, player2);

            GameGUI game = new GameGUI(player1, players); 
            game.setVisible(true);
            // 启动标准输入读取线程
            new Thread(() -> game.processInput()).start();
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