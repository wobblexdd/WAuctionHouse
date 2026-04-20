package me.wobble.wauctionhouse.gui;

import me.wobble.wauctionhouse.WAuctionHouse;
import me.wobble.wauctionhouse.model.AuctionListing;
import me.wobble.wauctionhouse.util.ChatUtil;
import me.wobble.wauctionhouse.util.TextStyleUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class AuctionGUI {

    public enum SortType {
        NEWEST,
        LOWEST_PRICE,
        HIGHEST_PRICE
    }

    public static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public static final int PREVIOUS_SLOT = 45;
    public static final int INFO_SLOT = 4;
    public static final int MY_LISTINGS_SLOT = 48;
    public static final int CLOSE_SLOT = 49;
    public static final int SORT_SLOT = 50;
    public static final int SEARCH_SLOT = 51;
    public static final int RESET_SLOT = 52;
    public static final int NEXT_SLOT = 53;

    private final WAuctionHouse plugin;
    private final Pagination<AuctionListing> pagination;

    public AuctionGUI(WAuctionHouse plugin) {
        this.plugin = plugin;
        this.pagination = new Pagination<>();
    }

    public void open(Player player) {
        open(player, 1, SortType.NEWEST, null);
    }

    public void open(Player player, int page, SortType sortType, String searchQuery) {
        String title = plugin.getConfig().getString("gui.title", "<dark_gray>ᴀᴜᴄᴛɪᴏɴ");
        int size = plugin.getConfig().getInt("gui.size", 54);

        List<AuctionListing> listings = prepareListings(sortType, searchQuery);
        int pageSize = getPageSize();
        int maxPage = pagination.maxPage(listings, pageSize);

        if (page < 1) {
            page = 1;
        }
        if (page > maxPage) {
            page = maxPage;
        }

        Inventory inventory = ManagedGui.createInventory(ManagedGui.Type.AUCTION_MAIN, size, ChatUtil.mm(title));

        fillBackground(inventory);
        placeFrameAccents(inventory);
        placeControls(inventory, page, maxPage, listings.size(), sortType, searchQuery);

        if (listings.isEmpty()) {
            inventory.setItem(22, simpleItem(
                    Material.PAPER,
                    "<red>" + TextStyleUtil.smallCaps("No Listings"),
                    List.of(
                            ChatUtil.mm(TextStyleUtil.hint(searchQuery == null || searchQuery.isBlank()
                                    ? "there are no active listings right now"
                                    : "no listings matched your search")),
                            ChatUtil.mm("<gray>Use:</gray> <gold>/ah sell <price></gold>")
                    )
            ));

            player.openInventory(inventory);
            return;
        }

        List<AuctionListing> pageListings = pagination.page(listings, page, pageSize);

        for (int i = 0; i < Math.min(pageListings.size(), CONTENT_SLOTS.length); i++) {
            AuctionListing listing = pageListings.get(i);
            OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.getSellerId());
            String sellerName = seller.getName() == null ? "Unknown" : seller.getName();

            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();

            List<Component> lore = new ArrayList<>();
            lore.add(ChatUtil.mm("<gray>Seller:</gray> <yellow>" + sellerName + "</yellow>"));
            lore.add(ChatUtil.mm("<gray>Price:</gray> <gold>" + plugin.getAuctionService().format(listing.getPrice()) + "</gold>"));
            lore.add(ChatUtil.mm("<gray>Listed:</gray> <yellow>" + formatSortDate(listing.getCreatedAt()) + "</yellow>"));
            lore.add(ChatUtil.mm("<gray>ID:</gray> <dark_gray>" + shortId(listing.getListingId()) + "</dark_gray>"));
            lore.add(ChatUtil.mm("<dark_gray>—"));
            lore.add(ChatUtil.mm(TextStyleUtil.hint("click to buy this item")));

            if (meta != null) {
                meta.lore(lore);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
                display.setItemMeta(meta);
            }

            inventory.setItem(CONTENT_SLOTS[i], display);
        }

        player.openInventory(inventory);
    }

    public AuctionListing getListingBySlot(int slot, int page, SortType sortType, String searchQuery) {
        List<AuctionListing> listings = prepareListings(sortType, searchQuery);
        List<AuctionListing> pageListings = pagination.page(listings, page, getPageSize());

        for (int i = 0; i < CONTENT_SLOTS.length && i < pageListings.size(); i++) {
            if (CONTENT_SLOTS[i] == slot) {
                return pageListings.get(i);
            }
        }

        return null;
    }

    public int getMaxPage(SortType sortType, String searchQuery) {
        return pagination.maxPage(prepareListings(sortType, searchQuery), getPageSize());
    }

    public int getResultCount(SortType sortType, String searchQuery) {
        return prepareListings(sortType, searchQuery).size();
    }

    public int getPageSize() {
        int configured = plugin.getConfig().getInt("gui.items-per-page", CONTENT_SLOTS.length);
        return Math.min(configured, CONTENT_SLOTS.length);
    }

    private List<AuctionListing> prepareListings(SortType sortType, String searchQuery) {
        List<AuctionListing> listings = new ArrayList<>(plugin.getAuctionService().getActiveListings());

        if (searchQuery != null && !searchQuery.isBlank()) {
            String lower = searchQuery.toLowerCase(Locale.ROOT);
            listings.removeIf(listing -> {
                OfflinePlayer seller = Bukkit.getOfflinePlayer(listing.getSellerId());
                String sellerName = seller.getName();
                String itemName = listing.getItem().getType().name().toLowerCase(Locale.ROOT);
                boolean sellerMatch = sellerName != null && sellerName.toLowerCase(Locale.ROOT).contains(lower);
                boolean itemMatch = itemName.contains(lower);
                return !sellerMatch && !itemMatch;
            });
        }

        switch (sortType) {
            case LOWEST_PRICE -> listings.sort(Comparator.comparingDouble(AuctionListing::getPrice));
            case HIGHEST_PRICE -> listings.sort(Comparator.comparingDouble(AuctionListing::getPrice).reversed());
            case NEWEST -> listings.sort(Comparator.comparingLong(AuctionListing::getCreatedAt).reversed());
        }

        return listings;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = simpleItem(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> ", List.of());
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private void placeFrameAccents(Inventory inventory) {
        ItemStack accent = simpleItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ", List.of());
        int[] slots = {
                0, 1, 2, 3, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35, 36, 44,
                46, 47
        };

        for (int slot : slots) {
            inventory.setItem(slot, accent);
        }
    }

    private void placeControls(Inventory inventory, int page, int maxPage, int totalEntries, SortType sortType, String searchQuery) {
        boolean hasPrevious = page > 1;
        boolean hasNext = page < maxPage;
        boolean hasSearch = searchQuery != null && !searchQuery.isBlank();

        inventory.setItem(PREVIOUS_SLOT, simpleItem(
                hasPrevious ? Material.ARROW : Material.GRAY_DYE,
                hasPrevious ? "<yellow>" + TextStyleUtil.smallCaps("Previous Page")
                        : "<gray>" + TextStyleUtil.smallCaps("Previous Page"),
                List.of(
                        ChatUtil.mm(TextStyleUtil.hint("go to previous page")),
                        ChatUtil.mm("<gray>Page:</gray> <yellow>" + page + "</yellow><gray>/</gray><yellow>" + maxPage + "</yellow>")
                )
        ));

        inventory.setItem(NEXT_SLOT, simpleItem(
                hasNext ? Material.ARROW : Material.GRAY_DYE,
                hasNext ? "<yellow>" + TextStyleUtil.smallCaps("Next Page")
                        : "<gray>" + TextStyleUtil.smallCaps("Next Page"),
                List.of(
                        ChatUtil.mm(TextStyleUtil.hint("go to next page")),
                        ChatUtil.mm("<gray>Page:</gray> <yellow>" + page + "</yellow><gray>/</gray><yellow>" + maxPage + "</yellow>")
                )
        ));

        inventory.setItem(CLOSE_SLOT, simpleItem(
                Material.BARRIER,
                "<red>" + TextStyleUtil.smallCaps("Close"),
                List.of(ChatUtil.mm(TextStyleUtil.hint("click to close the menu")))
        ));

        inventory.setItem(MY_LISTINGS_SLOT, simpleItem(
                Material.CHEST,
                "<gold>" + TextStyleUtil.smallCaps("My Listings"),
                List.of(ChatUtil.mm(TextStyleUtil.hint("click to view your active listings")))
        ));

        inventory.setItem(SORT_SLOT, simpleItem(
                Material.HOPPER,
                "<gold>" + TextStyleUtil.smallCaps("Sort"),
                List.of(
                        ChatUtil.mm("<gray>Current:</gray> <yellow>" + sortLabel(sortType) + "</yellow>"),
                        ChatUtil.mm(TextStyleUtil.hint("click to change sorting"))
                )
        ));

        inventory.setItem(SEARCH_SLOT, simpleItem(
                hasSearch ? Material.WRITABLE_BOOK : Material.NAME_TAG,
                "<gold>" + TextStyleUtil.smallCaps("Search"),
                List.of(
                        ChatUtil.mm("<gray>Current:</gray> <yellow>" + (hasSearch ? searchQuery : "None") + "</yellow>"),
                        ChatUtil.mm(TextStyleUtil.hint("click to search seller or item")),
                        ChatUtil.mm(TextStyleUtil.hint("type cancel in chat to abort"))
                )
        ));

        inventory.setItem(RESET_SLOT, simpleItem(
                hasSearch ? Material.REDSTONE : Material.GRAY_DYE,
                hasSearch ? "<red>" + TextStyleUtil.smallCaps("Reset Filter")
                        : "<gray>" + TextStyleUtil.smallCaps("Reset Filter"),
                List.of(
                        ChatUtil.mm("<gray>Results:</gray> <gold>" + totalEntries + "</gold>"),
                        ChatUtil.mm(TextStyleUtil.hint("click to clear search"))
                )
        ));

        inventory.setItem(INFO_SLOT, simpleItem(
                Material.BOOK,
                "<gold>" + TextStyleUtil.smallCaps("WAuctionHouse"),
                List.of(
                        ChatUtil.mm("<gray>Page:</gray> <yellow>" + page + "</yellow><gray>/</gray><yellow>" + maxPage + "</yellow>"),
                        ChatUtil.mm("<gray>Total entries:</gray> <gold>" + totalEntries + "</gold>"),
                        ChatUtil.mm("<gray>Sort:</gray> <yellow>" + sortLabel(sortType) + "</yellow>"),
                        ChatUtil.mm("<gray>Search:</gray> <yellow>" + (hasSearch ? searchQuery : "None") + "</yellow>"),
                        ChatUtil.mm("<dark_gray>—"),
                        ChatUtil.mm(TextStyleUtil.hint("browse and buy listed items"))
                )
        ));
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

    private String shortId(UUID uuid) {
        String raw = uuid.toString();
        return raw.substring(0, 8);
    }

    private String sortLabel(SortType sortType) {
        return switch (sortType) {
            case NEWEST -> "Newest";
            case LOWEST_PRICE -> "Lowest Price";
            case HIGHEST_PRICE -> "Highest Price";
        };
    }

    private String formatSortDate(long millis) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - millis) / 1000L);
        long minutes = seconds / 60L;
        long hours = minutes / 60L;

        if (hours > 0) {
            return hours + "h ago";
        }
        if (minutes > 0) {
            return minutes + "m ago";
        }
        return seconds + "s ago";
    }
}
