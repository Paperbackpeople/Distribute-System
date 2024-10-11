package Interface;
import Message.ElectionMessage;
import Message.GossipMessage;
import Player.PlayerInfo;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface GameNodeInterface extends Remote {
    void receiveGossipMessage(GossipMessage message) throws RemoteException;
    void ping() throws RemoteException;
    void becomeBackup() throws RemoteException;
    void primaryNodeCrashed() throws RemoteException; // 新方法
    void becomePrimary() throws RemoteException;

}
