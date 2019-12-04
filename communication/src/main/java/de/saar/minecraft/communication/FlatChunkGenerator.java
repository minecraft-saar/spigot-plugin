package de.saar.minecraft.communication;

/**
 * According to https://bukkit.org/threads/how-to-create-custom-world-generators.79066/
 */

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
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
//        world.getWorldBorder().setCenter(getFixedSpawnLocation(world, null));
        WorldBorder border = world.getWorldBorder();
        border.setSize(32); // Because of chunk size 16
        ChunkData chunk = createChunkData(world);

        Location chunkLocation = new Location(world, chunkX, 0, chunkZ);
        if (!border.isInside(chunkLocation)){
            logger.info("Outside border " + chunkLocation.getX() + " " + border.getCenter().getBlockX());
            return chunk;
        }
        // Set ground blocks
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                chunk.setBlock(x, 1, z, Material.BEDROCK);
                chunk.setBlock(x, 0, z, Material.BEDROCK);
            }
        return chunk;
    }

    @Override
    public Location getFixedSpawnLocation(World world, Random random) {
        return new Location(world, 0, 2, 0);
    }
}
