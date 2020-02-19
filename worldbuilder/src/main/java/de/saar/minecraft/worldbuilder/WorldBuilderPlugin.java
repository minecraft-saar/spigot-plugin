package de.saar.minecraft.worldbuilder;


import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldBuilderPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getCommand("example").setExecutor(new ExampleCommand());
        this.getCommand("save").setExecutor(new SaveCommand());
        World world = getServer().getWorld("world");
        world.setTime(1200);
        WorldBorder border = world.getWorldBorder();
        world.setSpawnLocation(16,2,16);
        border.setCenter(world.getSpawnLocation());
        border.setSize(32);

        // TODO: make players join at spawn location

    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {

    }
}
