package Message;

import java.io.Serial;
public class GameState implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private int N; // 迷宫的大小
    private int K; // 宝藏的数量

    public GameState() {
    }
    public GameState(int N, int K) {
        this.N = N;
        this.K = K;
    }

    public int getN() {
        return N;
    }

    public int getK() {
        return K;
    }

    public void setN(int N) {
        this.N = N;
    }

    public void setK(int K) {
        this.K = K;
    }
}
