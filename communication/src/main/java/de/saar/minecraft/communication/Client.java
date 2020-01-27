package de.saar.minecraft.communication;

import java.net.UnknownHostException;
import java.util.HashMap;

public interface Client {
    public void shutdown() throws InterruptedException;

    public String registerGame(String playerName, String playerIp) throws UnknownHostException;

    public void finishGame(int gameId);

    public String sendPlayerPosition(int gameId, int x, int y, int z, double xDir, double yDir,
                                     double zDir);

    public String sendBlockPlaced(int gameId, int x, int y, int z, int type);

    public String sendBlockDestroyed(int gameId, int x, int y, int z, int type);

    public void sendMinecraftServerError(int gameId, String message);

    public void sendWorldFileError(int gameId, String message);

    int getGameIdForPlayer(String playerName);

    HashMap<String, Integer> getActiveGames();

}
