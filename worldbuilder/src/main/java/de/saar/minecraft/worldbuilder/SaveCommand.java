package de.saar.minecraft.worldbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashSet;
import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class SaveCommand implements CommandExecutor {
    static Logger logger = LogManager.getLogger(SaveCommand.class);


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            // Get current world
            World world = player.getWorld();

            // TODO: Check if given filename is valid
            if (args.length != 1){
                return false;
            }
            String filename = args[0];
            try {
                logger.info("Saving world {} in {}", world.getName(), filename);
                saveBuiltStructure(filename, world);
                logger.info("saved.");
                sender.sendMessage("You saved this world to file " + filename);
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
                sender.sendMessage("Saving failed");
                return false;
            }
            return true;
        }
        if (sender instanceof ConsoleCommandSender) {
            throw new NotImplementedException("Save command send by console");
        }
        if (sender instanceof BlockCommandSender) {
            throw new RuntimeException("Save command send by block");
        }
        throw new RuntimeException("Save command send by unknown sender");
    }


    /**
     * Saves all non-air blocks above the ground from a world to a csv-file.
     * @param filename csv-file where the blocks should be saved
     * @param world the Bukkit world where the structure should be loaded
     * @throws FileNotFoundException if it cannot write to "filename"
     */
    private void saveBuiltStructure(String filename, World world) throws FileNotFoundException {
        WorldBorder border = world.getWorldBorder();
        HashSet<Block> toSave = new HashSet<>();
        Location center = border.getCenter();
        int radius = ((Double)border.getSize()).intValue();  // TODO how to round here?

        // Loop over height until there are only air blocks
        boolean foundSolid = true;
        int y = 1; // Upmost ground layer
        while (foundSolid) {
            foundSolid = false;
            y++;
            // Loop over every block in this plain
            for (int x = center.getBlockX() - radius; x <= center.getBlockX() + radius; x++) {
                for (int z = center.getBlockZ() - radius; z <= center.getBlockZ() + radius; z++) {
                    Block currentBlock = world.getBlockAt(x,y,z);
                    if (!currentBlock.getType().isAir()) {
                        toSave.add(currentBlock);
                        foundSolid = true;
                        logger.debug("Adding to save set {}", currentBlock);
                    }
                }
                logger.debug("x is {}", x);
            }
        }
        // Save blocks
        File csvOutputFile = new File(filename);
        PrintWriter pw = new PrintWriter(csvOutputFile);
        for (Block block:toSave) {
            String line = String.format("%d,%d,%d,",
                block.getX(),
                block.getY(),
                block.getZ())
                + block.getType().name();
            pw.println(line);
            logger.debug("Saved: {}", line);
        }
        pw.flush();
    }
}
