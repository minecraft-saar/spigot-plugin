package de.saar.minecraft.replay;

import de.saar.minecraft.broker.db.tables.records.GamesRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import de.saar.minecraft.broker.db.Tables;
import de.saar.minecraft.broker.db.tables.records.GameLogsRecord;

public class ReplayPlugin extends JavaPlugin{
    public ReplayListener listener;
    private FileConfiguration config;
    private DSLContext jooq;
    private static Logger logger = LogManager.getLogger(ReplayPlugin.class);

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        config = getConfig();

        SelectGameCommand command = new SelectGameCommand(this);
        logger.info("command {}", command);
        this.getCommand("select").setExecutor(command);
        // TODO: add stop replay and/or pause replay command

        listener = new ReplayListener();
        getServer().getPluginManager().registerEvents(listener, this);

        // Get database connection
        String url = config.getString("url");
        String user = config.getString("user");
        String password = config.getString("password");

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
