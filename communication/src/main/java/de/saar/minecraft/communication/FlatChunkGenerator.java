package de.saar.minecraft.communication;

/**
 * According to https://bukkit.org/threads/how-to-create-custom-world-generators.79066/
 */

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;


public class FlatChunkGenerator extends ChunkGenerator {

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        ChunkData chunk = createChunkData(world);

        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                chunk.setBlock(x, 1, z, Material.GRASS);
                chunk.setBlock(x, 0, z, Material.BEDROCK);
            }
        return chunk;
    }
}
