package Message;

import Player.PlayerInfo;

import java.io.Serializable;
import java.util.List;

public class ElectionMessage implements Serializable {
    private int electionVersion;
    private List<PlayerInfo> candidate;
    private int participantCount; // 新增字段，参与选举的节点计数

    public ElectionMessage(int electionVersion, List<PlayerInfo> candidate, int participantCount) {
        this.electionVersion = electionVersion;
        this.candidate = candidate;
        this.participantCount = participantCount;
    }

    // Getter 和 Setter 方法

    public int getElectionVersion() {
        return electionVersion;
    }

    public List<PlayerInfo> getCandidate() {
        return candidate;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setElectionVersion(int electionVersion) {
        this.electionVersion = electionVersion;
    }

    public void setCandidate(List<PlayerInfo> candidate) {
        this.candidate = candidate;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }
}