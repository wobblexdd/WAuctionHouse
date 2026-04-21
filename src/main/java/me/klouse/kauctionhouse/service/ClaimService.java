package me.klouse.kauctionhouse.service;

import me.klouse.kauctionhouse.KAuctionHouse;
import me.klouse.kauctionhouse.economy.EconomyProvider;
import me.klouse.kauctionhouse.repository.ClaimRepository;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

public final class ClaimService {

    public enum ClaimResult {
        SUCCESS,
        NOTHING_TO_CLAIM,
        FAILED
    }

    private final KAuctionHouse plugin;
    private final EconomyProvider economyProvider;
    private final ClaimRepository claimRepository;

    public ClaimService(KAuctionHouse plugin, EconomyProvider economyProvider, ClaimRepository claimRepository) {
        this.plugin = plugin;
        this.economyProvider = economyProvider;
        this.claimRepository = claimRepository;
    }

    public double getClaimAmount(Player player) {
        return claimRepository.getAmount(player.getUniqueId());
    }

    public ClaimResult claim(Player player) {
        double amount = getClaimAmount(player);
        if (amount <= 0.0) {
            return ClaimResult.NOTHING_TO_CLAIM;
        }

        EconomyResponse response = economyProvider.getEconomy().depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            return ClaimResult.FAILED;
        }

        claimRepository.clear(player.getUniqueId());
        return ClaimResult.SUCCESS;
    }
}
