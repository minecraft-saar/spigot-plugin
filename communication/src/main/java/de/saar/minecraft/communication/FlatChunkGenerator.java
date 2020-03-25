/**
 * According to https://bukkit.org/threads/how-to-create-custom-world-generators.79066/
 */

package de.saar.minecraft.communication;

import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.generator.ChunkGenerator;


public class FlatChunkGenerator extends ChunkGenerator {

    private static Logger logger = LogManager.getLogger(FlatChunkGenerator.class);

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ,
                                       BiomeGrid biome) {
        WorldBorder border = world.getWorldBorder();
        ChunkData chunk = createChunkData(world);

        Location chunkLocation = new Location(world, chunkX, 0, chunkZ);
        if (!border.isInside(chunkLocation)) {
            logger.debug(String.format("Chunk %d-%d Outside border %d-%d",
                chunkLocation.getBlockX(),
                chunkLocation.getBlockZ(),
                border.getCenter().getBlockX(),
                border.getCenter().getBlockZ()));
            return chunk;
        }
        // Set ground blocks
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunk.setBlock(x, 65, z, Material.LIME_CONCRETE);
                chunk.setBlock(x, 64, z, Material.BEDROCK);
            }
        }
        return chunk;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 16, 66, 16);
    }  // Coordinates from 0 to 32 instead of -16 to 16
}
