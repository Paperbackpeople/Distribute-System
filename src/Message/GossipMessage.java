package Message;
import Player.PlayerInfo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class GossipMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private PlayerInfo primaryNode;
    private int version;
    private List<PlayerInfo> updatedPlayers;
    private List<PlayerInfo> crashedPlayers; // 存储崩溃的玩家ID


    public GossipMessage(PlayerInfo primaryNode, int version, List<PlayerInfo> updatedPlayers,
                         List<PlayerInfo> crashedPlayers) {
        this.primaryNode = primaryNode;
        this.version = version;
        this.updatedPlayers = updatedPlayers;
        this.crashedPlayers = crashedPlayers;

    }
    public void setPrimaryNode(PlayerInfo primaryNode) {
        this.primaryNode = primaryNode;
    }

    public PlayerInfo getPrimaryNode() {
        return primaryNode;
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

}
