package Player;

import java.io.Serializable;

public class PlayerInfo implements Serializable {
    private final String playerId;
    private String playerAddress;
    private int x;
    private int y;
    private int score;

    // 构造函数
    public PlayerInfo(String playerId, String playerAddress) {
        this.playerId = playerId;
        this.playerAddress = playerAddress;
        this.x = 0; // 初始位置
        this.y = 0;
        this.score = 0;
    }

    // getter 和 setter 方法
    public String getPlayerId() {
        return playerId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getScore() {
        return score;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public String getPlayerAddress() {
        return playerAddress;
    }

    public void setPlayerAddress(String playerAddress) {
        this.playerAddress = playerAddress;
    }

    public void incrementScore() {
        this.score++;
    }
}