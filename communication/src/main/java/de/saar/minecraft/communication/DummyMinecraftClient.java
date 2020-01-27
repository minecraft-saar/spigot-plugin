package de.saar.minecraft.communication;

import java.net.UnknownHostException;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DummyMinecraftClient implements Client {
    private static Logger logger = LogManager.getLogger(MinecraftClient.class);
    private HashMap<String, Integer> activeGames;
    private int dummyGameId;

    public DummyMinecraftClient() {
        activeGames = new HashMap<>();
        dummyGameId = 0;
    }


    public void shutdown() throws InterruptedException {
    }

    /**
     * Starts a new game with the world "bridge".
     */
    public String registerGame(String playerName, String playerIp) throws UnknownHostException {
        // remember active games
        activeGames.put(playerName, dummyGameId++);
        return "bridge";
    }

    /**
     * Ends the game.
     */
    public void finishGame(int gameId) {
        activeGames.values().remove(gameId);
        logger.info(String.format("Removed player %d", gameId));
        logger.info(activeGames.toString());
    }

    public String sendPlayerPosition(int gameId, int x, int y, int z, double xDir, double yDir,
                                     double zDir) {
        return String.format("Your position is %d-%d-%d looking in direction %f-%f-%f",
            x, y, z, xDir, yDir, zDir);
    }

    public int getGameIdForPlayer(String playerName) {
        return this.activeGames.get(playerName);
    }

    public HashMap<String, Integer> getActiveGames() {
        return this.activeGames;
    }

    public String sendBlockPlaced(int gameId, int x, int y, int z, int type) {
        return String.format("A %d block was placed at %d-%d-%d", type, x, y, z);
    }

    public String sendBlockDestroyed(int gameId, int x, int y, int z, int type) {
        return String.format("A %d block was destroyed at %d-%d-%d", type, x, y, z);
    }

    public void sendMinecraftServerError(int gameId, String message) {
    }

    public void sendWorldFileError(int gameId, String message) {
    }

}
