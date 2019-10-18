/**
 * According to https://bukkit.org/threads/how-to-create-custom-world-generators.79066/
 */

package de.saar.minecraft.worldtest;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;

public class FlatChunkGenerator extends ChunkGenerator {
    //int currentHeight = 10;

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, BiomeGrid biome) {
        // TODO Chunk generation code here.
        //SimplexOctaveGenerator generator = new SimplexOctaveGenerator(new Random(world.getSeed()), 8);
        ChunkData chunk = createChunkData(world);
        //generator.setScale(0.005D);

        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) {
                chunk.setBlock(x, 1, z, Material.GRASS);
                chunk.setBlock(x, 0, z, Material.BEDROCK);
            }
        return chunk;
    }

    public byte[][] generateBlockSections(World world, Random random, int x, int z, BiomeGrid biomeGrid) {
        byte[][] result = new byte[world.getMaxHeight() / 16][]; //world height / chunk part height (=16, look above)

        // Set ground layer of bedrock
        for (x = 0; x < 16; x++) {
            for (z = 0; z < 16; z++) {
                setBlock(result, x, 0, z, (byte) Material.BEDROCK.getId());
            }
        }
        // Grass layer on top
        for (x = 0; x < 16; x++) {
            for (z = 0; z < 16; z++) {
                setBlock(result, x, 1, z, (byte) Material.GRASS.getId());
            }
        }
        return result;
    }

    private void setBlock(byte[][] result, int x, int y, int z, byte blkid) {
        // is this chunk part already initialized?
        if (result[y >> 4] == null) {
            // Initialize the chunk part
            result[y >> 4] = new byte[4096];
        }
        // set the block (look above, how this is done)
        result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
    }

//  @Override
//  public Location getFixedSpawnLocation​(World world, Random random) {
//    return new Location(world, 0, 0, 0);
//  }

}