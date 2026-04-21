package me.klouse.kauctionhouse.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyProvider {

    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyProvider(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        if (provider == null) {
            return false;
        }

        this.economy = provider.getProvider();
        return this.economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
