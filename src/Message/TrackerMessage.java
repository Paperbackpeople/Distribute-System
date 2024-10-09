package Message;

import Player.PlayerInfo;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class TrackerMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String messageType; // "JOIN" 或 "UPDATE"
    private List<PlayerInfo> playerList; // 当前玩家列表
    private int N; // 迷宫大小
    private int k; // 宝藏数量
    private int version; // 版本号
    private PlayerInfo senderInfo; // 发送者信息

    // 构造函数
    public TrackerMessage(String messageType, List<PlayerInfo> playerList, int mazeSize, int k,  PlayerInfo senderInfo) {
        this.messageType = messageType;
        this.playerList = playerList;
        this.N = mazeSize;
        this.k = k;
        this.senderInfo = senderInfo;
    }

    // Getter 和 Setter 方法
    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public List<PlayerInfo> getPlayerList() {
        return playerList;
    }

    public void setPlayerList(List<PlayerInfo> playerList) {
        this.playerList = playerList;
    }

    public int getMazeSize() {
        return N;
    }

    public void setMazeSize(int mazeSize) {
        this.N = mazeSize;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public PlayerInfo getSenderInfo() {
        return senderInfo;
    }

    public void setSenderInfo(PlayerInfo senderInfo) {
        this.senderInfo = senderInfo;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }
}