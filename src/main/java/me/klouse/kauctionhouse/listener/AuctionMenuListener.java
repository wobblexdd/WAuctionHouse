package me.klouse.kauctionhouse.listener;

import me.klouse.kauctionhouse.KAuctionHouse;
import me.klouse.kauctionhouse.gui.AuctionGUI;
import me.klouse.kauctionhouse.gui.ManagedGui;
import me.klouse.kauctionhouse.gui.MyListingsGUI;
import me.klouse.kauctionhouse.model.AuctionListing;
import me.klouse.kauctionhouse.service.AuctionService;
import me.klouse.kauctionhouse.util.ChatUtil;
import me.klouse.kauctionhouse.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AuctionMenuListener implements Listener {

    private final KAuctionHouse plugin;
    private final AuctionGUI auctionGUI;
    private final MyListingsGUI myListingsGUI;

    private final Map<UUID, Integer> auctionPages = new HashMap<>();
    private final Map<UUID, Integer> myPages = new HashMap<>();
    private final Map<UUID, AuctionGUI.SortType> sortTypes = new HashMap<>();
    private final Map<UUID, String> searchQueries = new HashMap<>();
    private final Map<UUID, Boolean> awaitingSearch = new HashMap<>();

    public AuctionMenuListener(KAuctionHouse plugin) {
        this.plugin = plugin;
        this.auctionGUI = new AuctionGUI(plugin);
        this.myListingsGUI = new MyListingsGUI(plugin);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClickedInventory() == null) {
            return;
        }

        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (!(event.getClick().isLeftClick() || event.getClick().isRightClick())) {
            event.setCancelled(true);
            return;
        }

        ManagedGui.Type guiType = ManagedGui.getType(event.getView());
        if (guiType == null) {
            return;
        }

        switch (guiType) {
            case AUCTION_MAIN -> handleAuctionMenu(event, player);
            case MY_LISTINGS -> handleMyListingsMenu(event, player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearState(event.getPlayer().getUniqueId());
    }

    private void handleAuctionMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        int slot = event.getRawSlot();
        int currentPage = auctionPages.getOrDefault(playerId, 1);
        AuctionGUI.SortType sortType = sortTypes.getOrDefault(playerId, AuctionGUI.SortType.NEWEST);
        String searchQuery = searchQueries.get(playerId);

        if (slot == AuctionGUI.CLOSE_SLOT) {
            SoundUtil.playClick(plugin, player);
            player.closeInventory();
            return;
        }

        if (slot == AuctionGUI.MY_LISTINGS_SLOT) {
            SoundUtil.playClick(plugin, player);
            myPages.put(playerId, 1);
            myListingsGUI.open(player, 1);
            return;
        }

        if (slot == AuctionGUI.SORT_SLOT) {
            AuctionGUI.SortType next = nextSort(sortType);
            sortTypes.put(playerId, next);
            auctionPages.put(playerId, 1);
            SoundUtil.playClick(plugin, player);
            player.sendMessage(ChatUtil.mm("<gray>Sort:</gray> <yellow>" + sortName(next) + "</yellow>"));
            auctionGUI.open(player, 1, next, searchQuery);
            return;
        }

        if (slot == AuctionGUI.SEARCH_SLOT) {
            SoundUtil.playClick(plugin, player);
            awaitingSearch.put(playerId, true);
            player.closeInventory();
            player.sendMessage(ChatUtil.mm("<gray>Type seller or item name in chat.</gray>"));
            player.sendMessage(ChatUtil.mm("<gray>Type:</gray> <yellow>cancel</yellow> <gray>to abort.</gray>"));
            return;
        }

        if (slot == AuctionGUI.RESET_SLOT) {
            if (searchQuery == null || searchQuery.isBlank()) {
                SoundUtil.playError(plugin, player);
                return;
            }

            searchQueries.remove(playerId);
            auctionPages.put(playerId, 1);
            SoundUtil.playClick(plugin, player);
            player.sendMessage(ChatUtil.mm("<gray>Search reset.</gray>"));
            auctionGUI.open(player, 1, sortType, null);
            return;
        }

        if (slot == AuctionGUI.PREVIOUS_SLOT) {
            if (currentPage <= 1) {
                SoundUtil.playError(plugin, player);
                return;
            }

            int targetPage = currentPage - 1;
            auctionPages.put(playerId, targetPage);
            SoundUtil.playClick(plugin, player);
            auctionGUI.open(player, targetPage, sortType, searchQuery);
            return;
        }

        if (slot == AuctionGUI.NEXT_SLOT) {
            int maxPage = auctionGUI.getMaxPage(sortType, searchQuery);
            if (currentPage >= maxPage) {
                SoundUtil.playError(plugin, player);
                return;
            }

            int targetPage = currentPage + 1;
            auctionPages.put(playerId, targetPage);
            SoundUtil.playClick(plugin, player);
            auctionGUI.open(player, targetPage, sortType, searchQuery);
            return;
        }

        AuctionListing clicked = auctionGUI.getListingBySlot(slot, currentPage, sortType, searchQuery);
        if (clicked == null) {
            SoundUtil.playError(plugin, player);
            return;
        }

        AuctionService.BuyResult result = plugin.getAuctionService().buyListing(player, clicked.getListingId());

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(ChatUtil.message(
                        plugin,
                        "listing-bought",
                        "{price}", plugin.getAuctionService().format(clicked.getPrice())
                ));
                SoundUtil.playSuccess(plugin, player);

                int pageToOpen = Math.min(currentPage, auctionGUI.getMaxPage(sortType, searchQuery));
                auctionPages.put(playerId, pageToOpen);
                auctionGUI.open(player, pageToOpen, sortType, searchQuery);
            }
            case NOT_FOUND, NOT_ACTIVE -> {
                player.sendMessage(ChatUtil.mm("<red>This listing is no longer available.</red>"));
                SoundUtil.playError(plugin, player);
                auctionGUI.open(player, currentPage, sortType, searchQuery);
            }
            case OWN_LISTING -> {
                player.sendMessage(ChatUtil.mm("<red>You cannot buy your own listing.</red>"));
                SoundUtil.playError(plugin, player);
            }
            case NOT_ENOUGH_MONEY -> {
                player.sendMessage(ChatUtil.mm("<red>You do not have enough money.</red>"));
                SoundUtil.playError(plugin, player);
            }
            case INVENTORY_FULL -> {
                player.sendMessage(ChatUtil.mm("<red>Your inventory is full.</red>"));
                SoundUtil.playError(plugin, player);
            }
        }
    }

    private void handleMyListingsMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);

        UUID playerId = player.getUniqueId();
        int slot = event.getRawSlot();
        int currentPage = myPages.getOrDefault(playerId, 1);

        if (slot == MyListingsGUI.CLOSE_SLOT) {
            SoundUtil.playClick(plugin, player);
            player.closeInventory();
            return;
        }

        if (slot == MyListingsGUI.BACK_SLOT) {
            SoundUtil.playClick(plugin, player);
            int page = auctionPages.getOrDefault(playerId, 1);
            AuctionGUI.SortType sortType = sortTypes.getOrDefault(playerId, AuctionGUI.SortType.NEWEST);
            String searchQuery = searchQueries.get(playerId);
            auctionGUI.open(player, page, sortType, searchQuery);
            return;
        }

        if (slot == MyListingsGUI.PREVIOUS_SLOT) {
            if (currentPage <= 1) {
                SoundUtil.playError(plugin, player);
                return;
            }

            int targetPage = currentPage - 1;
            myPages.put(playerId, targetPage);
            SoundUtil.playClick(plugin, player);
            myListingsGUI.open(player, targetPage);
            return;
        }

        if (slot == MyListingsGUI.NEXT_SLOT) {
            int maxPage = maxMyPage(player);
            if (currentPage >= maxPage) {
                SoundUtil.playError(plugin, player);
                return;
            }

            int targetPage = currentPage + 1;
            myPages.put(playerId, targetPage);
            SoundUtil.playClick(plugin, player);
            myListingsGUI.open(player, targetPage);
        }
    }

    public boolean isAwaitingSearch(UUID playerId) {
        return awaitingSearch.getOrDefault(playerId, false);
    }

    public void cancelAwaitingSearch(UUID playerId) {
        awaitingSearch.remove(playerId);
    }

    public void clearState(UUID playerId) {
        auctionPages.remove(playerId);
        myPages.remove(playerId);
        sortTypes.remove(playerId);
        searchQueries.remove(playerId);
        awaitingSearch.remove(playerId);
    }

    public void applySearch(Player player, String query) {
        UUID playerId = player.getUniqueId();
        awaitingSearch.remove(playerId);

        AuctionGUI.SortType sortType = sortTypes.getOrDefault(playerId, AuctionGUI.SortType.NEWEST);

        if (query == null || query.isBlank()) {
            searchQueries.remove(playerId);
            auctionPages.put(playerId, 1);
            auctionGUI.open(player, 1, sortType, null);
            return;
        }

        searchQueries.put(playerId, query);
        auctionPages.put(playerId, 1);

        int results = auctionGUI.getResultCount(sortType, query);
        player.sendMessage(ChatUtil.mm("<gray>Search:</gray> <yellow>" + query + "</yellow>"));
        player.sendMessage(ChatUtil.mm("<gray>Results:</gray> <gold>" + results + "</gold>"));

        auctionGUI.open(player, 1, sortType, query);
    }

    private AuctionGUI.SortType nextSort(AuctionGUI.SortType current) {
        return switch (current) {
            case NEWEST -> AuctionGUI.SortType.LOWEST_PRICE;
            case LOWEST_PRICE -> AuctionGUI.SortType.HIGHEST_PRICE;
            case HIGHEST_PRICE -> AuctionGUI.SortType.NEWEST;
        };
    }

    private String sortName(AuctionGUI.SortType sortType) {
        return switch (sortType) {
            case NEWEST -> "Newest";
            case LOWEST_PRICE -> "Lowest Price";
            case HIGHEST_PRICE -> "Highest Price";
        };
    }

    private int maxMyPage(Player player) {
        int total = plugin.getAuctionService().getActiveListingsBySeller(player.getUniqueId()).size();
        return Math.max(1, (int) Math.ceil(total / 28.0));
    }
}
