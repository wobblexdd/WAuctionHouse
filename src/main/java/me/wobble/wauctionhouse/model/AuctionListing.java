package me.wobble.wauctionhouse.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class AuctionListing {

    private final UUID listingId;
    private final UUID sellerId;
    private final ItemStack item;
    private final double price;
    private final long createdAt;
    private final long expiresAt;

    private ListingStatus status;
    private UUID buyerId;
    private Long soldAt;

    public AuctionListing(UUID listingId,
                          UUID sellerId,
                          ItemStack item,
                          double price,
                          long createdAt,
                          long expiresAt,
                          ListingStatus status,
                          UUID buyerId,
                          Long soldAt) {
        this.listingId = listingId;
        this.sellerId = sellerId;
        this.item = item;
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.buyerId = buyerId;
        this.soldAt = soldAt;
    }

    public UUID getListingId() {
        return listingId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public UUID getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(UUID buyerId) {
        this.buyerId = buyerId;
    }

    public Long getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(Long soldAt) {
        this.soldAt = soldAt;
    }

    public boolean isActive() {
        return status == ListingStatus.ACTIVE;
    }

    public boolean isExpired(long now) {
        return expiresAt <= now;
    }
}
