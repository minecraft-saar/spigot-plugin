package de.saar.minecraft.replay;

import com.github.agomezmoron.multimedia.recorder.configuration.VideoRecorderConfiguration;
import de.saar.minecraft.broker.db.tables.records.GamesRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitTask;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import de.saar.minecraft.broker.db.Tables;
import de.saar.minecraft.broker.db.tables.records.GameLogsRecord;

public class ReplayPlugin extends JavaPlugin{
    public ReplayListener listener;
    BukkitTask currentReplay;
    private FileConfiguration config;
    private DSLContext jooq;
    private static Logger logger = LogManager.getLogger(ReplayPlugin.class);

    class SelectDatabaseCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
            if (args.length == 0 || args.length > 3) {
                commandSender.sendMessage("usage: /connect db [user [password]]");
                return false;
            }
            String db = args[0];
            if (! db.contains(":")) {
                db = "jdbc:mysql://localhost:3306/" + db;
            }
            if (args.length == 1) {
                connectToDatabase(db, config.getString("user"), config.getString("password"));
            } else if (args.length == 2) {
                connectToDatabase(db, args[1], config.getString("password"));
            } else {
                connectToDatabase(db, args[1], args[2]);
            }
            return true;
        }
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        config = getConfig();

        File videoPath = new File(config.getString("videoPath"));
        VideoRecorderConfiguration.setVideoDirectory(videoPath);
        VideoRecorderConfiguration.setCaptureInterval(config.getInt("captureInterval"));

        SelectGameCommand command = new SelectGameCommand(this);
        logger.info("command {}", command);
        this.getCommand("select").setExecutor(command);

        RecordCommand recordCommand = new RecordCommand(command);
        this.getCommand("record").setExecutor(recordCommand);

        this.getCommand("stopRecording").setExecutor(new StopRecordingCommand(this));

        this.getCommand("connect").setExecutor(new SelectDatabaseCommand());

        listener = new ReplayListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        // Get database connection
        String url = config.getString("url");
        String user = config.getString("user");
        String password = config.getString("password");
        connectToDatabase(url, user, password);
    }
    
    public void connectToDatabase(String url, String user, String password) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);
            logger.info("conn {}", conn);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        jooq = DSL.using(
                conn,
                SQLDialect.valueOf("MYSQL")
        );
        logger.info("Connected to database at {}.", url);
    }

    public GamesRecord getGame(int gameId) {
        return jooq.selectFrom(Tables.GAMES)
                .where(Tables.GAMES.ID.equal(gameId))
                .fetchOne();
    }

    public Result<GameLogsRecord> getGameLog(int gameId) {
        return jooq.selectFrom(Tables.GAME_LOGS)
                .where(Tables.GAME_LOGS.GAMEID.equal(gameId))
                .orderBy(Tables.GAME_LOGS.ID.asc())
                .fetch();
    }
}
