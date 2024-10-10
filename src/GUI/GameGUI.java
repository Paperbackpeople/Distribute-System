package GUI;

import javax.swing.*;
import Message.GameState;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import Player.*;
import Position.Position;

public class GameGUI extends JFrame {
    private final JLabel scoreLabel;
    private final JPanel mazePanel;
    private  String[][] maze;
    private String playerId;
    private final int mazeSize;
    private List<PlayerInfo> players;
    private String primaryNode = "01";
    private String backupNode = "02";
    private GameState state;
    private final LocalTime startTime;


    public GameGUI(String playerId, GameState state) {
        this.playerId = playerId;
        this.state = state;
        this.mazeSize = state.getN();
        this.startTime = LocalTime.now();

        setTitle("Maze Game - " + playerId);
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        scoreLabel = new JLabel("Score: 0");
        mazePanel = new JPanel(new GridLayout(mazeSize, mazeSize)); // mazeSize = 15
        maze = new String[mazeSize][mazeSize];

        updateGameState();
        add(scoreLabel, BorderLayout.NORTH);
        add(mazePanel, BorderLayout.CENTER);
    }

    public void updateGameState() {
        players = state.getPlayers();
        //primaryNode = state.getPrimaryNode().getPlayerId();
        //backupNode = state.getBackupNode().getPlayerId();
        maze = state.getMaze();
        updateMaze();
    }
    
    public void updateMaze() {
        mazePanel.removeAll();
        mazePanel.setLayout(new BorderLayout());
        Font monoFont = new Font("Monospaced", Font.PLAIN, 12);
        Dimension labelSize = new Dimension(30, 30);
        JPanel leftInfoPanel = new JPanel();
        leftInfoPanel.setLayout(new BoxLayout(leftInfoPanel, BoxLayout.Y_AXIS));
        leftInfoPanel.setPreferredSize(new Dimension(150, 400));  

        // add scores
        leftInfoPanel.add(new JLabel("Player Scores:"));
        for (PlayerInfo p : players) {
            leftInfoPanel.add(new JLabel(p.getPlayerId() + ": " + state.getPlayerScores().get(p.getPlayerId())));  // 显示每个玩家的得分
        }
        // add primaryNode and backupNode
        leftInfoPanel.add(new JLabel("Main Server: " + primaryNode));
        leftInfoPanel.add(new JLabel("Backup Server: " + backupNode));
        // add startTime
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        leftInfoPanel.add(new JLabel("Game Start Time: " + startTime.format(timeFormatter)));

        mazePanel.add(leftInfoPanel, BorderLayout.WEST);

        // topBorder
        JPanel topBorderPanel = new JPanel(new GridLayout(1, mazeSize + 2));
        topBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  
        for (int j = 0; j < mazeSize; j++) {
            topBorderPanel.add(new JLabel("--", SwingConstants.CENTER));  
        }
        topBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  
        //add topBorder
        mazePanel.add(topBorderPanel, BorderLayout.NORTH);
        JPanel leftBorderPanel = new JPanel(new GridLayout(mazeSize, 1));
        for (int i = 0; i < mazeSize; i++) {
            leftBorderPanel.add(new JLabel("|", SwingConstants.CENTER));
        }
        // maze
        JPanel mazeBodyPanel = new JPanel(new GridLayout(mazeSize, mazeSize));
        for (int i = 0; i < mazeSize; i++) {
            for (int j = 0; j < mazeSize; j++) {
                JLabel cellLabel = new JLabel(String.valueOf(maze[i][j]), SwingConstants.CENTER);
                cellLabel.setFont(monoFont);
                cellLabel.setPreferredSize(labelSize);
                cellLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                mazeBodyPanel.add(cellLabel);  // add maze to the bodyPanel
            }
        }

        JPanel rightBorderPanel = new JPanel(new GridLayout(mazeSize, 1));
        for (int i = 0; i < mazeSize; i++) {
            rightBorderPanel.add(new JLabel("|", SwingConstants.CENTER));
        }

        JPanel bottomBorderPanel = new JPanel(new GridLayout(1, mazeSize + 2));

        bottomBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  
        for (int j = 0; j < mazeSize; j++) {
            bottomBorderPanel.add(new JLabel("--", SwingConstants.CENTER));  // bottom Border
        }
        bottomBorderPanel.add(new JLabel("+", SwingConstants.CENTER));  

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
        System.out.println("Exiting game for player: " + playerId);
        dispose();  // Close the GUI
    }

    public static void main(String[] args) {
        PlayerInfo player1 = new PlayerInfo("01");
        PlayerInfo player2 = new PlayerInfo("02");
        //若player.setPosition(x,y)则会产生bug，因为init gamestate时默认(x，y)=(-1，-1)
        //在导入数据的时候判断为仅在(x，y)=(-1，-1)的时候会加入到gameState.playerScores和gameState.playerPositions中
        //因此若init之前就setPosition会导致两个hashmap为空

        List<PlayerInfo> players = List.of(player1, player2);
        //懒得排除自己了直接先这样写了，我看到GamerNode里面的player是排除了自身的，应该没问题

        GameState state = new GameState(15,10);
        state.setPlayers(players);
        state.initializeGameState();
        GameGUI game = new GameGUI(player1.getPlayerId(), state);
        game.setVisible(true);
        System.out.println(state);

        //方便看对比
        try {
            // delay
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        //随便移动一下
        Position curplayerPosition = state.getPlayerPositions().getOrDefault(player1.getPlayerId(), new Position(-1, -1));
        state.getPlayerPositions().put(player1.getPlayerId(), curplayerPosition);
        state.getMaze()[player1.getX()+1][player1.getY()+1] = player1.getPlayerId();
        
        //这里因为GUI的Label大小在init的时候根据mazeSize生成，所以无法修改，最好不要用setMazeSize
        game.updateGameState();
        System.out.println(state);

        try {
            // delay
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        game.exitGame();
    }
        
}