package Message;
import Player.PlayerInfo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class GossipMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String primaryNodeIp;
    private int primaryNodePort;
    private int version;
    private List<PlayerInfo> updatedPlayers;

    public GossipMessage(String primaryNodeIp, int primaryNodePort, int version, List<PlayerInfo> updatedPlayers) {
        this.primaryNodeIp = primaryNodeIp;
        this.primaryNodePort = primaryNodePort;
        this.version = version;
        this.updatedPlayers = updatedPlayers;

    }
    public void setPrimaryNodeIp(String primaryNodeIp) {
        this.primaryNodeIp = primaryNodeIp;
    }

    public void setPrimaryNodePort(int primaryNodePort) {
        this.primaryNodePort = primaryNodePort;
    }

    public String getPrimaryNodeIp() {
        return primaryNodeIp;
    }

    public int getPrimaryNodePort() {
        return primaryNodePort;
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

}
