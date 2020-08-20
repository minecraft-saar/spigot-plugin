package de.saar.minecraft.replay;

import com.github.agomezmoron.multimedia.recorder.VideoRecorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.MalformedURLException;

public class RecordCommand implements CommandExecutor {

    private SelectGameCommand selectGameCommand;
    private static final Logger logger = LogManager.getLogger(RecordCommand.class);

    RecordCommand(SelectGameCommand selectGameCommand) {
        this.selectGameCommand = selectGameCommand;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (! (sender instanceof Player)) {
            return false;
        }
        if (args.length < 1) {
            sender.sendMessage("Missing game id for record");
            return false;
        }
        Player player = (Player) sender;
//        if (VideoRecorder.running()) {
            try {
                String videoPath = VideoRecorder.stop();
                player.sendMessage("Finished recording: " + videoPath);
            } catch (MalformedURLException e) {
                logger.error(e.getMessage());
            }
//        }
        VideoRecorder.start("replay_" + args[0]);
        sender.sendMessage("Started recording");

        int gameId = Integer.parseInt(args[0]);
        selectGameCommand.runCommand(gameId, player);

//        // TODO: wait until selectGameCommand wakes thread up
//
//        try {
//            while (selectGameCommand.plugin.currentReplay != null) {
//                Thread.sleep(1000);
//                }
//            } catch (InterruptedException ie) {
//            try {
//                String videoPathString = VideoRecorder.stop();
//                sender.sendMessage("Finished recording: " + videoPathString);
//            } catch (MalformedURLException me) {
//                me.printStackTrace();
//            }
//            }

        return true;
    }
}
