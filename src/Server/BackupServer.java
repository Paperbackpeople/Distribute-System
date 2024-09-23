package Server;

import Interface.GameServer;
import Player.PlayerInfo;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
public class BackupServer extends UnicastRemoteObject implements GameServer {

    private Map<String, PlayerInfo> gameState;

    public BackupServer() throws RemoteException {
        super();
    }

    @Override
    public void registerPlayer(String playerId) throws RemoteException {
        // 不处理玩家注册，等待主服务器同步状态
    }

    @Override
    public void updatePlayerPosition(String playerId, int newX, int newY) throws RemoteException {
        // 不处理位置更新，等待主服务器同步状态
    }

    @Override
    public Map<String, PlayerInfo> getGameState() throws RemoteException {
        return gameState;
    }

    @Override
    public void updateGameState(Map<String, PlayerInfo> newState) throws RemoteException {
        this.gameState = newState;
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }
}