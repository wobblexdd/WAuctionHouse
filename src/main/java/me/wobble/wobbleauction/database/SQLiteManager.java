package me.wobble.wobbleauction.database;

import me.wobble.wobbleauction.WobbleAuction;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class SQLiteManager {

    private final WobbleAuction plugin;
    private Connection connection;

    public SQLiteManager(WobbleAuction plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder.");
            }

            String fileName = plugin.getConfig().getString("database.file", "data.db");
            File dbFile = new File(plugin.getDataFolder(), fileName);

            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            plugin.getLogger().info("Connected to SQLite database.");
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not connect to SQLite database", exception);
        }
    }

    public void createTables() {
        String listingsTable = """
                CREATE TABLE IF NOT EXISTS auction_listings (
                    listing_id TEXT PRIMARY KEY,
                    seller_uuid TEXT NOT NULL,
                    buyer_uuid TEXT,
                    item_data TEXT NOT NULL,
                    price REAL NOT NULL,
                    status TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL,
                    sold_at INTEGER
                );
                """;

        String claimsTable = """
                CREATE TABLE IF NOT EXISTS auction_claims (
                    player_uuid TEXT PRIMARY KEY,
                    amount REAL NOT NULL
                );
                """;

        String expiredItemsTable = """
                CREATE TABLE IF NOT EXISTS auction_expired_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    owner_uuid TEXT NOT NULL,
                    item_data TEXT NOT NULL,
                    expired_at INTEGER NOT NULL
                );
                """;

        String historyTable = """
                CREATE TABLE IF NOT EXISTS auction_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    listing_id TEXT NOT NULL,
                    seller_uuid TEXT NOT NULL,
                    buyer_uuid TEXT NOT NULL,
                    price REAL NOT NULL,
                    sold_at INTEGER NOT NULL
                );
                """;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(listingsTable);
            statement.executeUpdate(claimsTable);
            statement.executeUpdate(expiredItemsTable);
            statement.executeUpdate(historyTable);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not create tables", exception);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException exception) {
            plugin.getLogger().severe("Could not close SQLite connection.");
        }
    }
}
