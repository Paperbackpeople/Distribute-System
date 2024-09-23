package Server;

import Interface.GameServer;
import Player.PlayerInfo;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class PrimaryServer extends UnicastRemoteObject implements GameServer {
    private Map<String, PlayerInfo> gameState;
    private GameServer backupServer;

    public PrimaryServer() throws RemoteException {
        super();
        gameState = new HashMap<>();
    }

    @Override
    public void registerPlayer(String playerId) throws RemoteException {
        gameState.put(playerId, new PlayerInfo(playerId, "localhost"));
        syncWithBackup();
    }

    @Override
    public void updatePlayerPosition(String playerId, int newX, int newY) throws RemoteException {
        PlayerInfo player = gameState.get(playerId);
        player.setPosition(newX, newY);
        syncWithBackup();
    }

    @Override
    public Map<String, PlayerInfo> getGameState() throws RemoteException {
        return gameState;
    }

    @Override
    public void updateGameState(Map<String, PlayerInfo> newState) throws RemoteException {
        gameState = newState;
    }

    @Override
    public boolean ping() throws RemoteException {
        return true;
    }

    public void setBackupServer(GameServer backupServer) {
        this.backupServer = backupServer;
    }

    private void syncWithBackup() throws RemoteException {
        if (backupServer != null) {
            backupServer.updateGameState(gameState);
        }
    }
}