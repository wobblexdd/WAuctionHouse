package me.wobble.wauctionhouse.gui;

import me.wobble.wauctionhouse.WAuctionHouse;
import me.wobble.wauctionhouse.model.AuctionListing;
import me.wobble.wauctionhouse.util.ChatUtil;
import me.wobble.wauctionhouse.util.TextStyleUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MyListingsGUI {

    public static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public static final int BACK_SLOT = 45;
    public static final int CLOSE_SLOT = 49;
    public static final int PREVIOUS_SLOT = 48;
    public static final int NEXT_SLOT = 50;

    private final WAuctionHouse plugin;
    private final Pagination<AuctionListing> pagination;

    public MyListingsGUI(WAuctionHouse plugin) {
        this.plugin = plugin;
        this.pagination = new Pagination<>();
    }

    public void open(Player player) {
        open(player, 1);
    }

    public void open(Player player, int page) {
        List<AuctionListing> listings = plugin.getAuctionService().getActiveListingsBySeller(player.getUniqueId());
        int pageSize = CONTENT_SLOTS.length;
        int maxPage = pagination.maxPage(listings, pageSize);

        if (page < 1) {
            page = 1;
        }
        if (page > maxPage) {
            page = maxPage;
        }

        Inventory inventory = ManagedGui.createInventory(ManagedGui.Type.MY_LISTINGS, 54, ChatUtil.mm("<dark_gray>ᴍʏ ʟɪsᴛɪɴɢs"));

        fillBackground(inventory);

        inventory.setItem(BACK_SLOT, simpleItem(
                Material.ARROW,
                "<yellow>" + TextStyleUtil.smallCaps("Back"),
                List.of(ChatUtil.mm(TextStyleUtil.hint("return to auction")))
        ));

        inventory.setItem(CLOSE_SLOT, simpleItem(
                Material.BARRIER,
                "<red>" + TextStyleUtil.smallCaps("Close"),
                List.of(ChatUtil.mm(TextStyleUtil.hint("close the menu")))
        ));

        inventory.setItem(PREVIOUS_SLOT, simpleItem(
                page > 1 ? Material.ARROW : Material.GRAY_DYE,
                page > 1 ? "<yellow>" + TextStyleUtil.smallCaps("Previous Page")
                        : "<gray>" + TextStyleUtil.smallCaps("Previous Page"),
                List.of(ChatUtil.mm(TextStyleUtil.hint("go to previous page")))
        ));

        inventory.setItem(NEXT_SLOT, simpleItem(
                page < maxPage ? Material.ARROW : Material.GRAY_DYE,
                page < maxPage ? "<yellow>" + TextStyleUtil.smallCaps("Next Page")
                        : "<gray>" + TextStyleUtil.smallCaps("Next Page"),
                List.of(ChatUtil.mm(TextStyleUtil.hint("go to next page")))
        ));

        if (listings.isEmpty()) {
            inventory.setItem(22, simpleItem(
                    Material.PAPER,
                    "<red>" + TextStyleUtil.smallCaps("No Active Listings"),
                    List.of(ChatUtil.mm(TextStyleUtil.hint("you have no active listings")))
            ));
            player.openInventory(inventory);
            return;
        }

        List<AuctionListing> pageListings = pagination.page(listings, page, pageSize);

        for (int i = 0; i < Math.min(pageListings.size(), CONTENT_SLOTS.length); i++) {
            AuctionListing listing = pageListings.get(i);
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();

            List<Component> lore = new ArrayList<>();
            lore.add(ChatUtil.mm("<gray>Price:</gray> <gold>" + plugin.getAuctionService().format(listing.getPrice()) + "</gold>"));
            lore.add(ChatUtil.mm("<gray>Status:</gray> <yellow>" + listing.getStatus().name() + "</yellow>"));
            lore.add(ChatUtil.mm("<gray>ID:</gray> <dark_gray>" + listing.getListingId().toString().substring(0, 8) + "</dark_gray>"));
            lore.add(ChatUtil.mm("<dark_gray>-"));
            lore.add(ChatUtil.mm(TextStyleUtil.hint("your active listing")));

            if (meta != null) {
                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                display.setItemMeta(meta);
            }

            inventory.setItem(CONTENT_SLOTS[i], display);
        }

        player.openInventory(inventory);
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = simpleItem(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack simpleItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(ChatUtil.mm(name));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}
