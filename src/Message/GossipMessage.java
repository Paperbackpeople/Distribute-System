package Message;
import Player.PlayerInfo;
import Message.GameState;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;


public class GossipMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private PlayerInfo primaryNode;
    private PlayerInfo backupNode;
    private int version;
    private List<PlayerInfo> updatedPlayers;
    private List<PlayerInfo> crashedPlayers; // 存储崩溃的玩家ID
    private PlayerInfo senderInfo; // 发送者的信息
    private GameState gameState; // 游戏状态


    public GossipMessage(PlayerInfo primaryNode, PlayerInfo backupNode, int version, List<PlayerInfo> updatedPlayers,
                         List<PlayerInfo> crashedPlayers, PlayerInfo senderInfo, GameState gameState) {
        this.primaryNode = primaryNode;
        this.backupNode = backupNode;
        this.version = version;
        this.updatedPlayers = updatedPlayers;
        this.crashedPlayers = crashedPlayers;
        this.senderInfo = senderInfo;
        this.gameState = gameState;

    }
    public void setPrimaryNode(PlayerInfo primaryNode) {
        this.primaryNode = primaryNode;
    }

    public PlayerInfo getPrimaryNode() {
        return primaryNode;
    }
    public PlayerInfo getBackupNode() {
        return backupNode;
    }
    public void setBackupNode(PlayerInfo backupNode) {
        this.backupNode = backupNode;
    }

    public int getVersion() {
        return version;
    }
    public void setVersion(int version) {
        this.version = version;
    }
    public List<PlayerInfo> getUpdatedPlayers() {
        return updatedPlayers;
    }
    public void setUpdatedPlayers(List<PlayerInfo> updatedPlayers) {
        this.updatedPlayers = updatedPlayers;
    }
    public List<PlayerInfo> getCrashedPlayers() {
        return crashedPlayers;
    }
    public void setCrashedPlayers(List<PlayerInfo> crashedPlayers) {
        this.crashedPlayers = crashedPlayers;
    }

    public PlayerInfo getSenderInfo() {
        return senderInfo;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setSenderInfo(PlayerInfo senderInfo) {
        this.senderInfo = senderInfo;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public String toString() {
        return "GossipMessage{" +
                "primaryNode=" + primaryNode +
                ", backupNode=" + backupNode +
                ", version=" + version +
                ", updatedPlayers=" + updatedPlayers +
                ", crashedPlayers=" + crashedPlayers +
                ", senderInfo=" + senderInfo +
                ", gameState=" + gameState +
                '}';
    }
}
