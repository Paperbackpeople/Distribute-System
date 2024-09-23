package Interface;
import Message.GossipMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameNodeInterface extends Remote {
    void receiveGossipMessage(GossipMessage message) throws RemoteException;
}
