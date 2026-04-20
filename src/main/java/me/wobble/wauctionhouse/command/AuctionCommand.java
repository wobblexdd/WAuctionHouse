package me.wobble.wauctionhouse.command;

import me.wobble.wauctionhouse.WAuctionHouse;
import me.wobble.wauctionhouse.gui.AuctionGUI;
import me.wobble.wauctionhouse.model.AuctionListing;
import me.wobble.wauctionhouse.service.AuctionService;
import me.wobble.wauctionhouse.service.ClaimService;
import me.wobble.wauctionhouse.service.ExpiredService;
import me.wobble.wauctionhouse.util.ChatUtil;
import me.wobble.wauctionhouse.util.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class AuctionCommand implements CommandExecutor, TabCompleter {

    private final WAuctionHouse plugin;
    private final AuctionService auctionService;
    private final ClaimService claimService;
    private final ExpiredService expiredService;
    private final AuctionGUI auctionGUI;

    public AuctionCommand(WAuctionHouse plugin, AuctionService auctionService, ClaimService claimService, ExpiredService expiredService) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.claimService = claimService;
        this.expiredService = expiredService;
        this.auctionGUI = new AuctionGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtil.message(plugin, "only-player"));
            return true;
        }

        if (args.length == 0) {
            auctionGUI.open(player);
            SoundUtil.playClick(plugin, player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "sell" -> handleSell(player, args);
            case "my" -> handleMy(player);
            case "claim" -> handleClaim(player);
            case "expired" -> handleExpired(player);
            case "cancel" -> handleCancel(player, args);
            case "reload" -> handleReload(player);
            default -> {
                player.sendMessage(ChatUtil.mm("<red>Unknown subcommand.</red>"));
                SoundUtil.playError(plugin, player);
            }
        }

        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatUtil.mm("<red>Usage: /ah sell <price></red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException exception) {
            player.sendMessage(ChatUtil.message(plugin, "invalid-price"));
            SoundUtil.playError(plugin, player);
            return;
        }

        AuctionService.SellResult result = auctionService.sellItem(player, price);

        switch (result) {
            case SUCCESS -> {
                double taxPercent = auctionService.getTaxPercent();
                double netAmount = price;
                if (taxPercent > 0.0) {
                    netAmount = Math.max(0.0, price - (price * (taxPercent / 100.0)));
                }

                player.sendMessage(ChatUtil.message(
                        plugin,
                        "listing-created",
                        "{price}", auctionService.format(price),
                        "{net}", auctionService.format(netAmount)
                ));
                SoundUtil.playSuccess(plugin, player);
            }
            case INVALID_PRICE -> {
                player.sendMessage(ChatUtil.message(plugin, "invalid-price"));
                SoundUtil.playError(plugin, player);
            }
            case PRICE_TOO_LOW -> {
                player.sendMessage(ChatUtil.message(plugin, "price-too-low"));
                SoundUtil.playError(plugin, player);
            }
            case PRICE_TOO_HIGH -> {
                player.sendMessage(ChatUtil.message(plugin, "price-too-high"));
                SoundUtil.playError(plugin, player);
            }
            case ITEM_NOT_FOUND -> {
                player.sendMessage(ChatUtil.message(plugin, "item-not-found"));
                SoundUtil.playError(plugin, player);
            }
            case ITEM_BLOCKED -> {
                player.sendMessage(ChatUtil.message(plugin, "item-blocked"));
                SoundUtil.playError(plugin, player);
            }
            case LIMIT_REACHED -> {
                player.sendMessage(ChatUtil.mm("<red>You reached your active listing limit.</red>"));
                SoundUtil.playError(plugin, player);
            }
        }
    }

    private void handleMy(Player player) {
        List<AuctionListing> listings = auctionService.getActiveListingsBySeller(player.getUniqueId());

        if (listings.isEmpty()) {
            player.sendMessage(ChatUtil.mm("<red>You have no active listings.</red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        player.sendMessage(ChatUtil.mm("<gold><bold>Your Active Listings</bold></gold>"));
        int index = 1;

        for (AuctionListing listing : listings) {
            String itemName = listing.getItem().getType().name();
            player.sendMessage(ChatUtil.mm(
                    "<gray>" + index + ".</gray> <yellow>" + itemName + "</yellow> <dark_gray>-</dark_gray> <gold>"
                            + auctionService.format(listing.getPrice()) + "</gold>"
            ));
            index++;
        }

        SoundUtil.playSuccess(plugin, player);
    }

    private void handleClaim(Player player) {
        double amount = claimService.getClaimAmount(player);

        ClaimService.ClaimResult result = claimService.claim(player);
        switch (result) {
            case SUCCESS -> {
                player.sendMessage(ChatUtil.mm("<green>You claimed <gold>" + auctionService.format(amount) + "</gold>.</green>"));
                SoundUtil.playSuccess(plugin, player);
            }
            case NOTHING_TO_CLAIM -> {
                player.sendMessage(ChatUtil.message(plugin, "nothing-to-claim"));
                SoundUtil.playError(plugin, player);
            }
            case FAILED -> {
                player.sendMessage(ChatUtil.mm("<red>Could not process your claim.</red>"));
                SoundUtil.playError(plugin, player);
            }
        }
    }

    private void handleExpired(Player player) {
        ExpiredService.ClaimResult result = expiredService.claimAll(player);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(ChatUtil.mm("<green>Expired items returned.</green>"));
                SoundUtil.playSuccess(plugin, player);
            }
            case NOTHING -> {
                player.sendMessage(ChatUtil.message(plugin, "nothing-expired"));
                SoundUtil.playError(plugin, player);
            }
            case INVENTORY_FULL -> {
                player.sendMessage(ChatUtil.mm("<red>Your inventory is full.</red>"));
                SoundUtil.playError(plugin, player);
            }
        }
    }


    private void handleCancel(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatUtil.mm("<red>Usage: /ah cancel <listing-id-prefix></red>"));
            SoundUtil.playError(plugin, player);
            return;
        }

        AuctionService.CancelResult result = auctionService.cancelListing(player, args[1]);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(ChatUtil.message(plugin, "listing-cancelled"));
                SoundUtil.playSuccess(plugin, player);
            }
            case NOT_FOUND -> {
                player.sendMessage(ChatUtil.message(plugin, "listing-cancel-not-found"));
                SoundUtil.playError(plugin, player);
            }
            case INVENTORY_FULL -> {
                player.sendMessage(ChatUtil.mm("<red>Your inventory is full.</red>"));
                SoundUtil.playError(plugin, player);
            }
        }
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("wobble.auction.reload")) {
            player.sendMessage(ChatUtil.message(plugin, "no-permission"));
            SoundUtil.playError(plugin, player);
            return;
        }

        plugin.reloadPlugin();
        player.sendMessage(ChatUtil.message(plugin, "reload-success"));
        SoundUtil.playSuccess(plugin, player);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("sell");
            suggestions.add("my");
            suggestions.add("claim");
            suggestions.add("expired");
            suggestions.add("cancel");
            suggestions.add("reload");
            return filter(suggestions, args[0]);
        }

        return suggestions;
    }

    private List<String> filter(List<String> source, String input) {
        String lower = input.toLowerCase();
        return source.stream()
                .filter(value -> value.toLowerCase().startsWith(lower))
                .toList();
    }
}
