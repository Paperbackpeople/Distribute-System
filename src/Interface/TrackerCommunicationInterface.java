package Interface;

import Message.TrackerMessage;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TrackerCommunicationInterface extends Remote {
    /**
     * 发送 TrackerMessage 到 Tracker，并接收响应
     * @param message 需要发送的 TrackerMessage
     * @return TrackerMessage 响应消息
     * @throws RemoteException
     */
    TrackerMessage sendTrackerMessage(TrackerMessage message) throws RemoteException;
}