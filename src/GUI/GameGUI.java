package GUI;

import javax.swing.*;
import Message.GameState;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Player.*;
import Position.Position;

public class GameGUI extends JFrame {
    private final JPanel mazePanel;
    private  String[][] maze;
    private String playerId;
    private final int mazeSize;
    private List<PlayerInfo> players;
    private String primaryNode;
    private String backupNode;
    private GameState state;
    private HashMap<String, Integer> playerScores;
    private HashMap<String, Position> playerPositions;

    private LocalTime startTime;


    public GameGUI(String playerId, GameState state) {
        this.playerId = playerId;
        this.state = state;
        this.mazeSize = state.getN();
        this.startTime = state.getStartTime();

        setTitle("Maze Game - " + playerId);
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        mazePanel = new JPanel(new GridLayout(mazeSize, mazeSize)); // mazeSize = 15
        maze = new String[mazeSize][mazeSize];

        updateGameState(state);
        add(mazePanel, BorderLayout.CENTER);
        // 设置关闭操作
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 添加窗口监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                // 在窗口关闭后，退出程序
                System.exit(0);
            }
        });
    }

    public void updateGameState(GameState state) {
            players = state.getPlayers();
            maze = state.getMaze();
            playerScores = state.getPlayerScores();
            playerPositions = state.getPlayerPositions();
            // 更新主服务器和备份服务器信息
            primaryNode = state.getPrimaryNode();
            backupNode = state.getBackupNodeId();
        // 重新绘制迷宫
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
        for (Map.Entry<String, Integer> entry : playerScores.entrySet()) {
            String playerId = entry.getKey();
            int score = entry.getValue();
            leftInfoPanel.add(new JLabel(playerId + ": " + score));
        }
        // add primaryNode and backupNode
        leftInfoPanel.add(new JLabel("Main Server: " + primaryNode));
        leftInfoPanel.add(new JLabel("Backup Server: " + backupNode));
        // add startTime
        leftInfoPanel.add(new JLabel("<html>Game Start Time:<br>" + state.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "</html>"));
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
        System.out.println("Updating maze for player: " + Arrays.deepToString(maze));
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

  }