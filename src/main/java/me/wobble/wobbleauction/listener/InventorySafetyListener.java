package me.wobble.wobbleauction.listener;

import me.wobble.wobbleauction.gui.ManagedGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class InventorySafetyListener implements Listener {

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (ManagedGui.isManaged(event.getView())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUnsafeClick(InventoryClickEvent event) {
        if (!ManagedGui.isManaged(event.getView())) {
            return;
        }

        ClickType click = event.getClick();
        InventoryAction action = event.getAction();

        if (event.isShiftClick()
                || click == ClickType.NUMBER_KEY
                || click == ClickType.DOUBLE_CLICK
                || action == InventoryAction.COLLECT_TO_CURSOR
                || action == InventoryAction.HOTBAR_SWAP
                || action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            event.setCancelled(true);
        }
    }
}
