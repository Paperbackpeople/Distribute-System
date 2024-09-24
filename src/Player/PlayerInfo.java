package Player;

import java.io.Serializable;

public class PlayerInfo implements Serializable {
    private final String playerId;
    private String IpAddress;
    private int port;
    private int x;
    private int y;
    private int score;

    // 构造函数
    public PlayerInfo(String playerId, String playerAddress) {
        this.playerId = playerId;
        this.IpAddress = playerAddress;
        this.port = 0; // 初始端口
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

    public String getIpAddress() {
        return IpAddress;
    }

    public void setIpAddress(String ipAddress) {
        IpAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void incrementScore() {
        this.score++;
    }
}