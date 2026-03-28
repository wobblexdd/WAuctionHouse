package me.wobble.wobbleauction.service;

import me.wobble.wobbleauction.WobbleAuction;
import me.wobble.wobbleauction.repository.ExpiredRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class ExpiredService {

    public enum ClaimResult {
        SUCCESS,
        NOTHING,
        INVENTORY_FULL
    }

    private final WobbleAuction plugin;
    private final ExpiredRepository expiredRepository;

    public ExpiredService(WobbleAuction plugin, ExpiredRepository expiredRepository) {
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
            if (player.getInventory().firstEmpty() == -1) {
                return ClaimResult.INVENTORY_FULL;
            }

            ItemStack item = entry.item();
            player.getInventory().addItem(item);
            expiredRepository.deleteById(entry.id());
        }

        return ClaimResult.SUCCESS;
    }
}
