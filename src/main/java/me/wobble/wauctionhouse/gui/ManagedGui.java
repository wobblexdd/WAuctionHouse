package me.wobble.wauctionhouse.gui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;

public final class ManagedGui {

    public enum Type {
        AUCTION_MAIN,
        MY_LISTINGS
    }

    public static final class Holder implements InventoryHolder {

        private final Type type;
        private Inventory inventory;

        private Holder(Type type) {
            this.type = type;
        }

        public Type type() {
            return type;
        }

        private void attach(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private ManagedGui() {
    }

    public static Inventory createInventory(Type type, int size, net.kyori.adventure.text.Component title) {
        Holder holder = new Holder(type);
        Inventory inventory = Bukkit.createInventory(holder, size, title);
        holder.attach(inventory);
        return inventory;
    }

    public static @Nullable Type getType(InventoryView view) {
        if (view == null) {
            return null;
        }

        return getType(view.getTopInventory());
    }

    public static @Nullable Type getType(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        InventoryHolder holder = inventory.getHolder();
        if (holder instanceof Holder managedHolder) {
            return managedHolder.type();
        }

        return null;
    }

    public static boolean isManaged(InventoryView view) {
        return getType(view) != null;
    }
}
