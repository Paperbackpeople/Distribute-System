package Treasure;

import java.io.Serializable;

public class TreasureInfo implements Serializable {
    private int x;
    private int y;

    public TreasureInfo(int x, int y) {
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