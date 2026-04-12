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


    public Optional<AuctionListing> findActiveBySellerPrefix(UUID sellerId, String listingIdPrefix) {
        String sql = "SELECT * FROM auction_listings WHERE seller_uuid = ? AND status = 'ACTIVE'";
        String normalized = listingIdPrefix == null ? "" : listingIdPrefix.toLowerCase();

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, sellerId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    AuctionListing listing = map(resultSet);
                    if (listing.getListingId().toString().toLowerCase().startsWith(normalized)) {
                        return Optional.of(listing);
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not find seller listing by prefix", exception);
        }

        return Optional.empty();
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

    public boolean completePurchaseIfActive(UUID listingId,
                                            UUID sellerId,
                                            UUID buyerId,
                                            double price,
                                            double sellerProceeds,
                                            long soldAt) {
        Connection connection = sqliteManager.getConnection();
        boolean previousAutoCommit;

        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            String updateListingSql = """
                    UPDATE auction_listings
                    SET status = ?, buyer_uuid = ?, sold_at = ?
                    WHERE listing_id = ? AND status = 'ACTIVE'
                    """;

            try (PreparedStatement updateListing = connection.prepareStatement(updateListingSql)) {
                updateListing.setString(1, ListingStatus.SOLD.name());
                updateListing.setString(2, buyerId.toString());
                updateListing.setLong(3, soldAt);
                updateListing.setString(4, listingId.toString());

                int updated = updateListing.executeUpdate();
                if (updated != 1) {
                    connection.rollback();
                    connection.setAutoCommit(previousAutoCommit);
                    return false;
                }
            }

            String historySql = """
                    INSERT INTO auction_history (listing_id, seller_uuid, buyer_uuid, price, sold_at)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement history = connection.prepareStatement(historySql)) {
                history.setString(1, listingId.toString());
                history.setString(2, sellerId.toString());
                history.setString(3, buyerId.toString());
                history.setDouble(4, price);
                history.setLong(5, soldAt);
                history.executeUpdate();
            }

            String claimSql = """
                    INSERT INTO auction_claims (player_uuid, amount)
                    VALUES (?, ?)
                    ON CONFLICT(player_uuid)
                    DO UPDATE SET amount = auction_claims.amount + excluded.amount
                    """;

            try (PreparedStatement claim = connection.prepareStatement(claimSql)) {
                claim.setString(1, sellerId.toString());
                claim.setDouble(2, sellerProceeds);
                claim.executeUpdate();
            }

            connection.commit();
            connection.setAutoCommit(previousAutoCommit);
            return true;
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }

            try {
                connection.setAutoCommit(true);
            } catch (SQLException resetException) {
                exception.addSuppressed(resetException);
            }

            throw new IllegalStateException("Could not complete listing purchase transaction", exception);
        }
    }

    public boolean cancelListingIfActiveAndSeller(UUID listingId, UUID sellerId) {
        String sql = """
                UPDATE auction_listings
                SET status = ?, buyer_uuid = ?, sold_at = ?
                WHERE listing_id = ? AND seller_uuid = ? AND status = 'ACTIVE'
                """;

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, ListingStatus.CANCELLED.name());
            statement.setString(2, null);
            statement.setObject(3, null);
            statement.setString(4, listingId.toString());
            statement.setString(5, sellerId.toString());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not cancel listing", exception);
        }
    }

    public boolean expireListingIfActive(UUID listingId) {
        String sql = """
                UPDATE auction_listings
                SET status = ?, buyer_uuid = ?, sold_at = ?
                WHERE listing_id = ? AND status = 'ACTIVE'
                """;

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, ListingStatus.EXPIRED.name());
            statement.setString(2, null);
            statement.setObject(3, null);
            statement.setString(4, listingId.toString());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not expire listing", exception);
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
