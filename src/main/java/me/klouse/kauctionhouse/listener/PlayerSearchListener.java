package me.klouse.kauctionhouse.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.klouse.kauctionhouse.KAuctionHouse;
import me.klouse.kauctionhouse.util.ChatUtil;
import me.klouse.kauctionhouse.util.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class PlayerSearchListener implements Listener {

    private final KAuctionHouse plugin;
    private final AuctionMenuListener auctionMenuListener;

    public PlayerSearchListener(KAuctionHouse plugin, AuctionMenuListener auctionMenuListener) {
        this.plugin = plugin;
        this.auctionMenuListener = auctionMenuListener;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!auctionMenuListener.isAwaitingSearch(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (input.equalsIgnoreCase("cancel")) {
                auctionMenuListener.cancelAwaitingSearch(player.getUniqueId());
                SoundUtil.playClick(plugin, player);
                player.sendMessage(ChatUtil.mm("<gray>Search cancelled.</gray>"));
                return;
            }

            SoundUtil.playSuccess(plugin, player);
            auctionMenuListener.applySearch(player, input);
        });
    }
}
