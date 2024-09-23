package GUI;

import javax.swing.*;
import java.awt.*;

public class GameGUI extends JFrame {
    private final JLabel scoreLabel;
    private final JPanel mazePanel;

    public GameGUI(String playerId) {
        setTitle("Maze Game - " + playerId);
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        scoreLabel = new JLabel("Score: 0");

        mazePanel = new JPanel(new GridLayout(15, 15)); // 假设迷宫大小为15x15
        for (int i = 0; i < 225; i++) {
            mazePanel.add(new JLabel()); // 初始化迷宫的空格子
        }

        add(scoreLabel, BorderLayout.NORTH);
        add(mazePanel, BorderLayout.CENTER);
    }

    public void updateMaze(char[][] maze) {
        mazePanel.removeAll();
        for (int i = 0; i < maze.length; i++) {
            for (int j = 0; j < maze[i].length; j++) {
                mazePanel.add(new JLabel(String.valueOf(maze[i][j])));
            }
        }
        mazePanel.revalidate();
        mazePanel.repaint();
    }

    public void updateScore(int score) {
        scoreLabel.setText("Score: " + score);
    }
}