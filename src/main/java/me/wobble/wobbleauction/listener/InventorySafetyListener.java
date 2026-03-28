package me.wobble.wobbleauction.listener;

import me.wobble.wobbleauction.util.ChatUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class InventorySafetyListener implements Listener {

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().title() == null) {
            return;
        }

        if (event.getView().title().equals(ChatUtil.mm("<dark_gray>ᴍʏ ʟɪsᴛɪɴɢs"))
                || event.getView().title().equals(ChatUtil.mm("<dark_gray>ᴀᴜᴄᴛɪᴏɴ"))
                || event.getView().title().equals(ChatUtil.mm("<dark_gray>ᴀᴜᴄᴛɪᴏɴ"))) {
            event.setCancelled(true);
        }
    }
}
