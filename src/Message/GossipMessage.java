package Message;
import java.io.Serializable;
public class GossipMessage implements Serializable {
    private String primaryNodeIp;
    private int primaryNodePort;

    public GossipMessage(String primaryNodeIp, int primaryNodePort) {
        this.primaryNodeIp = primaryNodeIp;
        this.primaryNodePort = primaryNodePort;
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

    public String toString() {
        return primaryNodeIp + ":" + primaryNodePort;
    }


}
