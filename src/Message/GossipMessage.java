package Message;
import java.io.Serializable;
public class GossipMessage implements Serializable {
    private String primaryNodeIp;
    private int primaryNodePort;
    private int version;

    public GossipMessage(String primaryNodeIp, int primaryNodePort, int version) {
        this.primaryNodeIp = primaryNodeIp;
        this.primaryNodePort = primaryNodePort;
        this.version = version;
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

}
