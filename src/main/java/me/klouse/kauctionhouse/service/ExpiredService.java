package me.klouse.kauctionhouse.service;

import me.klouse.kauctionhouse.KAuctionHouse;
import me.klouse.kauctionhouse.repository.ExpiredRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ExpiredService {

    public enum ClaimResult {
        SUCCESS,
        NOTHING,
        INVENTORY_FULL
    }

    private final KAuctionHouse plugin;
    private final ExpiredRepository expiredRepository;

    public ExpiredService(KAuctionHouse plugin, ExpiredRepository expiredRepository) {
        this.plugin = plugin;
        this.expiredRepository = expiredRepository;
    }

    public ClaimResult claimAll(Player player) {
        List<ExpiredRepository.ExpiredEntry> items =
                expiredRepository.findByOwner(player.getUniqueId());

        if (items.isEmpty()) {
            return ClaimResult.NOTHING;
        }

        for (ExpiredRepository.ExpiredEntry entry : items) {
            ItemStack item = entry.item();
            if (item == null) {
                expiredRepository.deleteById(entry.id());
                continue;
            }

            if (player.getInventory().firstEmpty() == -1) {
                return ClaimResult.INVENTORY_FULL;
            }

            player.getInventory().addItem(item.clone());
            expiredRepository.deleteById(entry.id());
        }

        return ClaimResult.SUCCESS;
    }

}
