package Player;

import java.io.Serializable;

public class PlayerInfo implements Serializable {
    private final String playerId;
    private String IpAddress;
    private int x;
    private int y;
    private int score;

    // 构造函数
    public PlayerInfo(String playerId, String playerAddress, int x, int y, int score) {
        this.playerId = playerId;
        this.IpAddress = playerAddress;
        this.x = 0; // 初始位置
        this.y = 0;
        this.score = 0;
    }
    public PlayerInfo(String playerId) {
        this.playerId = playerId;
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

    public String getIpAddress() {
        return IpAddress;
    }

    public void setIpAddress(String ipAddress) {
        IpAddress = ipAddress;
    }

    @Override
    public String toString() {
        return "PlayerInfo{" +
                "playerId='" + playerId + '\'' +
                '}';
    }

    public void incrementScore() {
        this.score++;
    }
}