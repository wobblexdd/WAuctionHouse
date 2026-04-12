package me.wobble.wobbleauction.service;

import me.wobble.wobbleauction.WobbleAuction;
import me.wobble.wobbleauction.economy.EconomyProvider;
import me.wobble.wobbleauction.model.AuctionListing;
import me.wobble.wobbleauction.model.ListingStatus;
import me.wobble.wobbleauction.repository.AuctionRepository;
import me.wobble.wobbleauction.repository.ExpiredRepository;
import me.wobble.wobbleauction.util.NumberFormatUtil;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class AuctionService {

    public enum SellResult {
        SUCCESS,
        INVALID_PRICE,
        PRICE_TOO_LOW,
        PRICE_TOO_HIGH,
        ITEM_NOT_FOUND,
        ITEM_BLOCKED,
        LIMIT_REACHED
    }

    public enum BuyResult {
        SUCCESS,
        NOT_FOUND,
        NOT_ACTIVE,
        OWN_LISTING,
        NOT_ENOUGH_MONEY,
        INVENTORY_FULL
    }

    public enum CancelResult {
        SUCCESS,
        NOT_FOUND,
        INVENTORY_FULL
    }

    private final WobbleAuction plugin;
    private final EconomyProvider economyProvider;
    private final AuctionRepository auctionRepository;
    private final ExpiredRepository expiredRepository;

    public AuctionService(WobbleAuction plugin,
                          EconomyProvider economyProvider,
                          AuctionRepository auctionRepository,
                          ExpiredRepository expiredRepository) {
        this.plugin = plugin;
        this.economyProvider = economyProvider;
        this.auctionRepository = auctionRepository;
        this.expiredRepository = expiredRepository;
    }

    public String format(double amount) {
        return NumberFormatUtil.format(amount);
    }

    public List<AuctionListing> getActiveListings() {
        return auctionRepository.findActive();
    }

    public List<AuctionListing> getActiveListingsBySeller(UUID sellerId) {
        return auctionRepository.findActiveBySeller(sellerId);
    }

    public int getMaxActiveListings(Player player) {
        if (player.hasPermission("wobble.auction.limit.vip")) {
            return plugin.getConfig().getInt("auction.max-active-listings-vip", 10);
        }

        return plugin.getConfig().getInt("auction.max-active-listings-default", 3);
    }

    public double getTaxPercent() {
        return Math.max(0.0, plugin.getConfig().getDouble("auction.tax-percent", 0.0));
    }

    public SellResult sellItem(Player player, double price) {
        if (price <= 0) {
            return SellResult.INVALID_PRICE;
        }

        double minPrice = plugin.getConfig().getDouble("auction.min-price", 100.0);
        double maxPrice = plugin.getConfig().getDouble("auction.max-price", 1_000_000_000.0);

        if (price < minPrice) {
            return SellResult.PRICE_TOO_LOW;
        }

        if (price > maxPrice) {
            return SellResult.PRICE_TOO_HIGH;
        }

        if (auctionRepository.countActiveBySeller(player.getUniqueId()) >= getMaxActiveListings(player)) {
            return SellResult.LIMIT_REACHED;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
            return SellResult.ITEM_NOT_FOUND;
        }

        if (isBlocked(item.getType())) {
            return SellResult.ITEM_BLOCKED;
        }

        ItemStack listedItem = item.clone();
        player.getInventory().setItemInMainHand(null);

        long createdAt = System.currentTimeMillis();
        long durationHours = plugin.getConfig().getLong("auction.listing-duration-hours", 24L);
        long expiresAt = createdAt + (durationHours * 60L * 60L * 1000L);

        AuctionListing listing = new AuctionListing(
                UUID.randomUUID(),
                player.getUniqueId(),
                listedItem,
                price,
                createdAt,
                expiresAt,
                ListingStatus.ACTIVE,
                null,
                null
        );

        try {
            auctionRepository.insert(listing);
        } catch (RuntimeException exception) {
            player.getInventory().setItemInMainHand(listedItem);
            throw exception;
        }

        return SellResult.SUCCESS;
    }

    public BuyResult buyListing(Player buyer, UUID listingId) {
        Optional<AuctionListing> optionalListing = auctionRepository.findById(listingId);
        if (optionalListing.isEmpty()) {
            return BuyResult.NOT_FOUND;
        }

        AuctionListing listing = optionalListing.get();
        if (!listing.isActive()) {
            return BuyResult.NOT_ACTIVE;
        }

        if (listing.getSellerId().equals(buyer.getUniqueId())) {
            return BuyResult.OWN_LISTING;
        }

        if (buyer.getInventory().firstEmpty() == -1) {
            return BuyResult.INVENTORY_FULL;
        }

        if (economyProvider.getEconomy().getBalance(buyer) < listing.getPrice()) {
            return BuyResult.NOT_ENOUGH_MONEY;
        }

        EconomyResponse withdraw = economyProvider.getEconomy().withdrawPlayer(buyer, listing.getPrice());
        if (!withdraw.transactionSuccess()) {
            return BuyResult.NOT_ENOUGH_MONEY;
        }

        long soldAt = System.currentTimeMillis();
        double sellerAmount = calculateSellerProceeds(listing.getPrice());

        boolean completed = auctionRepository.completePurchaseIfActive(
                listing.getListingId(),
                listing.getSellerId(),
                buyer.getUniqueId(),
                listing.getPrice(),
                sellerAmount,
                soldAt
        );

        if (!completed) {
            economyProvider.getEconomy().depositPlayer(buyer, listing.getPrice());
            return BuyResult.NOT_ACTIVE;
        }

        buyer.getInventory().addItem(listing.getItem().clone());
        return BuyResult.SUCCESS;
    }

    public CancelResult cancelListing(Player seller, String listingIdPrefix) {
        if (listingIdPrefix == null || listingIdPrefix.isBlank()) {
            return CancelResult.NOT_FOUND;
        }

        Optional<AuctionListing> optionalListing = auctionRepository.findActiveBySellerPrefix(
                seller.getUniqueId(),
                listingIdPrefix.trim()
        );

        if (optionalListing.isEmpty()) {
            return CancelResult.NOT_FOUND;
        }

        AuctionListing listing = optionalListing.get();
        if (seller.getInventory().firstEmpty() == -1) {
            return CancelResult.INVENTORY_FULL;
        }

        boolean cancelled = auctionRepository.cancelListingIfActiveAndSeller(listing.getListingId(), seller.getUniqueId());
        if (!cancelled) {
            return CancelResult.NOT_FOUND;
        }

        seller.getInventory().addItem(listing.getItem().clone());
        return CancelResult.SUCCESS;
    }

    public void expireListing(AuctionListing listing) {
        if (!listing.isActive()) {
            return;
        }

        boolean expired = auctionRepository.expireListingIfActive(listing.getListingId());
        if (expired) {
            expiredRepository.add(listing.getSellerId(), listing.getItem(), System.currentTimeMillis());
        }
    }

    private double calculateSellerProceeds(double price) {
        double taxPercent = getTaxPercent();
        double taxed = price - (price * (taxPercent / 100.0));
        return Math.max(0.0, taxed);
    }

    private boolean isBlocked(Material material) {
        List<String> blocked = plugin.getConfig().getStringList("auction.blocked-materials");
        String name = material.name().toUpperCase(Locale.ROOT);

        for (String entry : blocked) {
            if (name.equals(entry.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }
}
