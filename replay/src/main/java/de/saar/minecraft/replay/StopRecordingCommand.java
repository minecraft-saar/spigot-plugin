package de.saar.minecraft.replay;

import com.github.agomezmoron.multimedia.recorder.VideoRecorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.net.MalformedURLException;

public class StopRecordingCommand implements CommandExecutor {

    private ReplayPlugin plugin;
    private static final Logger logger = LogManager.getLogger(StopRecordingCommand.class);

    StopRecordingCommand(ReplayPlugin plugin){
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            plugin.listener.setMovementLocked(false);
            Bukkit.getScheduler().cancelTask(plugin.currentReplay.getTaskId());
            String videoPath = VideoRecorder.stop();
            sender.sendMessage("Finished recording: " + videoPath);
            return true;
        } catch (MalformedURLException e) {
            logger.error(e.getMessage());
            return false;
        }
    }
}
