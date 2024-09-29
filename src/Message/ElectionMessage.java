package Message;

import Player.PlayerInfo;

import java.io.Serializable;
import java.util.List;

public class ElectionMessage implements Serializable {
    private int electionVersion;
    private List<PlayerInfo> candidate;

    public ElectionMessage(int electionVersion, List<PlayerInfo> candidate) {
        this.electionVersion = electionVersion;
        this.candidate = candidate;
    }

    public int getElectionVersion() {
        return electionVersion;
    }
    public List<PlayerInfo> getCandidate() {
        return candidate;
    }

}