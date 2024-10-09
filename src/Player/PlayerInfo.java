package Player;

import java.io.Serializable;
import java.util.Objects;

public class PlayerInfo implements Serializable {
    private final String playerId;
    private int x = -1;
    private int y = -1;
    private int score = 0;

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

    public void incrementScore() {
        this.score++;
    }

    @Override
    public String toString() {
        return "PlayerInfo{" +
                "playerId='" + playerId +
                ", x=" + x +
                ", y=" + y +
                "}";
    }

    // 重写 equals() 方法
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlayerInfo that = (PlayerInfo) o;

        return Objects.equals(playerId, that.playerId);
    }

    // 重写 hashCode() 方法
    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }
}