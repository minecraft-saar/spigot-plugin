package de.saar.minecraft.communication;

import java.net.UnknownHostException;
import org.apache.commons.collections4.BidiMap;

public interface Client {
    void shutdown() throws InterruptedException;

    String registerGame(String playerName, String playerIp) throws UnknownHostException;

    void playerReady(int gameId);

    void finishGame(int gameId);

    void sendPlayerPosition(int gameId, int x, int y, int z, double xDir, double yDir,
                                     double zDir);

    void sendBlockPlaced(int gameId, int x, int y, int z, int type);

    void sendBlockDestroyed(int gameId, int x, int y, int z, int type);

    void sendMinecraftServerError(int gameId, String message);

    void sendWorldFileError(int gameId, String message);

    void sendTextMessage(int gameId, String message);

    int getGameIdForPlayer(String playerName);

    String getPlayernameFromGameId(int gameId);

    BidiMap<String, Integer> getActiveGames();

}
