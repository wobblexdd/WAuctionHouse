package me.wobble.wobbleauction.repository;

import me.wobble.wobbleauction.database.SQLiteManager;
import me.wobble.wobbleauction.model.AuctionListing;
import me.wobble.wobbleauction.model.ListingStatus;
import me.wobble.wobbleauction.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AuctionRepository {

    private final SQLiteManager sqliteManager;

    public AuctionRepository(SQLiteManager sqliteManager) {
        this.sqliteManager = sqliteManager;
    }

    public void insert(AuctionListing listing) {
        String sql = """
                INSERT INTO auction_listings (
                    listing_id, seller_uuid, buyer_uuid, item_data, price, status, created_at, expires_at, sold_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, listing.getListingId().toString());
            statement.setString(2, listing.getSellerId().toString());
            statement.setString(3, listing.getBuyerId() == null ? null : listing.getBuyerId().toString());
            statement.setString(4, ItemSerializer.serialize(listing.getItem()));
            statement.setDouble(5, listing.getPrice());
            statement.setString(6, listing.getStatus().name());
            statement.setLong(7, listing.getCreatedAt());
            statement.setLong(8, listing.getExpiresAt());

            if (listing.getSoldAt() == null) {
                statement.setObject(9, null);
            } else {
                statement.setLong(9, listing.getSoldAt());
            }

            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not insert listing", exception);
        }
    }

    public Optional<AuctionListing> findById(UUID listingId) {
        String sql = "SELECT * FROM auction_listings WHERE listing_id = ?";

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, listingId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(map(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find listing by id", exception);
        }
    }

    public List<AuctionListing> findActive() {
        String sql = "SELECT * FROM auction_listings WHERE status = 'ACTIVE'";
        List<AuctionListing> listings = new ArrayList<>();

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                listings.add(map(resultSet));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load active listings", exception);
        }

        listings.sort(Comparator.comparingLong(AuctionListing::getCreatedAt).reversed());
        return listings;
    }

    public List<AuctionListing> findActiveBySeller(UUID sellerId) {
        String sql = "SELECT * FROM auction_listings WHERE seller_uuid = ? AND status = 'ACTIVE'";
        List<AuctionListing> listings = new ArrayList<>();

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, sellerId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    listings.add(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load seller listings", exception);
        }

        listings.sort(Comparator.comparingLong(AuctionListing::getCreatedAt).reversed());
        return listings;
    }

    public List<AuctionListing> findExpiredActive(long now) {
        String sql = "SELECT * FROM auction_listings WHERE status = 'ACTIVE' AND expires_at <= ?";
        List<AuctionListing> listings = new ArrayList<>();

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setLong(1, now);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    listings.add(map(resultSet));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load expired active listings", exception);
        }

        return listings;
    }

    public int countActiveBySeller(UUID sellerId) {
        String sql = "SELECT COUNT(*) FROM auction_listings WHERE seller_uuid = ? AND status = 'ACTIVE'";

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, sellerId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not count seller listings", exception);
        }

        return 0;
    }

    public void updateStatus(UUID listingId, ListingStatus status, UUID buyerId, Long soldAt) {
        String sql = """
                UPDATE auction_listings
                SET status = ?, buyer_uuid = ?, sold_at = ?
                WHERE listing_id = ?
                """;

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, status.name());
            statement.setString(2, buyerId == null ? null : buyerId.toString());

            if (soldAt == null) {
                statement.setObject(3, null);
            } else {
                statement.setLong(3, soldAt);
            }

            statement.setString(4, listingId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not update listing status", exception);
        }
    }

    public void insertHistory(UUID listingId, UUID sellerId, UUID buyerId, double price, long soldAt) {
        String sql = """
                INSERT INTO auction_history (listing_id, seller_uuid, buyer_uuid, price, sold_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, listingId.toString());
            statement.setString(2, sellerId.toString());
            statement.setString(3, buyerId.toString());
            statement.setDouble(4, price);
            statement.setLong(5, soldAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not insert auction history", exception);
        }
    }

    private AuctionListing map(ResultSet resultSet) throws SQLException {
        UUID listingId = UUID.fromString(resultSet.getString("listing_id"));
        UUID sellerId = UUID.fromString(resultSet.getString("seller_uuid"));
        String buyerRaw = resultSet.getString("buyer_uuid");
        UUID buyerId = buyerRaw == null ? null : UUID.fromString(buyerRaw);

        ItemStack item = ItemSerializer.deserialize(resultSet.getString("item_data"));
        double price = resultSet.getDouble("price");
        ListingStatus status = ListingStatus.valueOf(resultSet.getString("status"));
        long createdAt = resultSet.getLong("created_at");
        long expiresAt = resultSet.getLong("expires_at");

        Object soldAtObject = resultSet.getObject("sold_at");
        Long soldAt = soldAtObject == null ? null : resultSet.getLong("sold_at");

        return new AuctionListing(
                listingId,
                sellerId,
                item,
                price,
                createdAt,
                expiresAt,
                status,
                buyerId,
                soldAt
        );
    }
}
