package Interface;

import Message.TrackerMessage;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TrackerInterface extends Remote {
    /**
     * 处理来自 GamerNode 的 TrackerMessage
     * @param message TrackerMessage 实例
     * @return TrackerMessage 响应消息
     * @throws RemoteException
     */
    TrackerMessage handleTrackerMessage(TrackerMessage message) throws RemoteException;
}