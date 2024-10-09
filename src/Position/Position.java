package Position;

import java.io.Serializable;
// 定义 Position 类
public class Position implements Serializable {
    private int x;
    private int y;

    // 构造方法
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // Getter 和 Setter 方法
    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    // 可选：重写 toString 方法，便于打印
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    // 可选：重写 equals 和 hashCode 方法，确保在 HashMap 中的正确性
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (x != position.x) return false;
        return y == position.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }
}