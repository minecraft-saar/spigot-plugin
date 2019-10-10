package de.saar.minecraft.communication;

import org.bukkit.plugin.java.JavaPlugin;

public class CommunicationPlugin extends JavaPlugin{

    // Fired when plugin is first enabled
    @Override
    public void onEnable() {
        System.out.println("Trying to enable CommunicationPlugin");
        MinecraftClient client = new MinecraftClient("localhost", 2802);
        getServer().getPluginManager().registerEvents(new MinecraftListener(client), this);
    }

    // Fired when plugin is disabled
    @Override
    public void onDisable() {

    }
}
