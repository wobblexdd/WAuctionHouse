package me.klouse.kauctionhouse;

import me.klouse.kauctionhouse.command.AuctionCommand;
import me.klouse.kauctionhouse.database.SQLiteManager;
import me.klouse.kauctionhouse.economy.EconomyProvider;
import me.klouse.kauctionhouse.listener.AuctionMenuListener;
import me.klouse.kauctionhouse.listener.InventorySafetyListener;
import me.klouse.kauctionhouse.listener.PlayerSearchListener;
import me.klouse.kauctionhouse.repository.AuctionRepository;
import me.klouse.kauctionhouse.repository.ClaimRepository;
import me.klouse.kauctionhouse.repository.ExpiredRepository;
import me.klouse.kauctionhouse.service.AuctionService;
import me.klouse.kauctionhouse.service.ClaimService;
import me.klouse.kauctionhouse.service.ExpirationService;
import me.klouse.kauctionhouse.service.ExpiredService;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public final class KAuctionHouse extends JavaPlugin {

    private FileConfiguration messagesConfig;
    private File messagesFile;

    private EconomyProvider economyProvider;
    private SQLiteManager sqliteManager;

    private AuctionRepository auctionRepository;
    private ClaimRepository claimRepository;
    private ExpiredRepository expiredRepository;

    private AuctionService auctionService;
    private ClaimService claimService;
    private ExpirationService expirationService;
    private ExpiredService expiredService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfNotExists("messages.yml");
        loadMessages();

        this.economyProvider = new EconomyProvider(this);
        if (!economyProvider.setup()) {
            getLogger().severe("Vault economy provider not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.sqliteManager = new SQLiteManager(this);
        sqliteManager.connect();
        sqliteManager.createTables();

        this.auctionRepository = new AuctionRepository(sqliteManager);
        this.claimRepository = new ClaimRepository(sqliteManager);
        this.expiredRepository = new ExpiredRepository(sqliteManager);

        this.claimService = new ClaimService(this, economyProvider, claimRepository);
        this.auctionService = new AuctionService(this, economyProvider, auctionRepository, expiredRepository);
        this.expirationService = new ExpirationService(this, auctionRepository, expiredRepository);
        this.expiredService = new ExpiredService(this, expiredRepository);

        PluginCommand auctionCommand = getCommand("ah");
        if (auctionCommand != null) {
            AuctionCommand executor = new AuctionCommand(this, auctionService, claimService, expiredService);
            auctionCommand.setExecutor(executor);
            auctionCommand.setTabCompleter(executor);
        }

        AuctionMenuListener auctionMenuListener = new AuctionMenuListener(this);

        getServer().getPluginManager().registerEvents(auctionMenuListener, this);
        getServer().getPluginManager().registerEvents(new InventorySafetyListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerSearchListener(this, auctionMenuListener), this);

        expirationService.start();

        getLogger().info("KAuctionHouse enabled.");
    }

    @Override
    public void onDisable() {
        if (expirationService != null) {
            expirationService.stop();
        }

        if (sqliteManager != null) {
            sqliteManager.close();
        }

        getLogger().info("KAuctionHouse disabled.");
    }

    public void reloadPlugin() {
        reloadConfig();
        loadMessages();
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }

    private void loadMessages() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Could not create plugin data folder.");
        }

        this.messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResourceIfNotExists("messages.yml");
        }

        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void saveMessages() {
        if (messagesConfig == null || messagesFile == null) {
            return;
        }

        try {
            messagesConfig.save(messagesFile);
        } catch (IOException exception) {
            getLogger().log(Level.SEVERE, "Could not save messages.yml", exception);
        }
    }

    public FileConfiguration getMessagesConfig() {
        return Objects.requireNonNull(messagesConfig, "messagesConfig");
    }

    public EconomyProvider getEconomyProvider() {
        return economyProvider;
    }

    public SQLiteManager getSqliteManager() {
        return sqliteManager;
    }

    public AuctionRepository getAuctionRepository() {
        return auctionRepository;
    }

    public ClaimRepository getClaimRepository() {
        return claimRepository;
    }

    public ExpiredRepository getExpiredRepository() {
        return expiredRepository;
    }

    public AuctionService getAuctionService() {
        return auctionService;
    }

    public ClaimService getClaimService() {
        return claimService;
    }

    public ExpirationService getExpirationService() {
        return expirationService;
    }

    public ExpiredService getExpiredService() {
        return expiredService;
    }
}
