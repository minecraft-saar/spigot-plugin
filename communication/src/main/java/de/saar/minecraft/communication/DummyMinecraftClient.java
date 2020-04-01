package de.saar.minecraft.communication;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class DummyMinecraftClient implements Client {
    private static Logger logger = LogManager.getLogger(MinecraftClient.class);
    private BidiMap<String, Integer> activeGames;
    private int dummyGameId;
    private WorldTestPlugin plugin;

    public DummyMinecraftClient(WorldTestPlugin plugin) {
        activeGames = new DualHashBidiMap<>();
        dummyGameId = 0;
        this.plugin = plugin;
    }


    public void shutdown() {
    }

    /**
     * Starts a new game with the world "bridge".
     */
    public String registerGame(String playerName, String playerIp) {
        // remember active games
        activeGames.put(playerName, dummyGameId++);
        return "bridge";
    }

    @Override
    public void playerReady(int gameId) {

    }

    /**
     * Ends the game.
     */
    public void finishGame(int gameId) {
        activeGames.values().remove(gameId);
        logger.info(String.format("Removed player %d", gameId));
        logger.info(activeGames.toString());
    }

    public void sendPlayerPosition(int gameId, int x, int y, int z, double xDir, double yDir,
                                     double zDir) {
        String message = String.format("Your position is %d-%d-%d looking in direction %f-%f-%f",
            x, y, z, xDir, yDir, zDir);
        String playerName = activeGames.getKey(gameId);
        plugin.sendTextMessage(playerName, message);
    }

    public int getGameIdForPlayer(String playerName) {
        return this.activeGames.get(playerName);
    }

    public String getPlayernameFromGameId(int gameId) {
        return this.activeGames.getKey(gameId);
    }

    public BidiMap<String, Integer> getActiveGames() {
        return this.activeGames;
    }

    public void sendBlockPlaced(int gameId, int x, int y, int z, int type) {
        String message = String.format("A %d block was placed at %d-%d-%d", type, x, y, z);
        String playerName = activeGames.getKey(gameId);
        plugin.sendTextMessage(playerName, message);
    }

    public void sendBlockDestroyed(int gameId, int x, int y, int z, int type) {
        String message = String.format("A %d block was destroyed at %d-%d-%d", type, x, y, z);
        String playerName = activeGames.getKey(gameId);
        plugin.sendTextMessage(playerName, message);
    }

    public void sendMinecraftServerError(int gameId, String message) {
    }

    public void sendWorldFileError(int gameId, String message) {
    }

    public void sendTextMessage(int gameId, String message){

    }

}
