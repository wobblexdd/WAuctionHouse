package me.wobble.wauctionhouse.repository;

import me.wobble.wauctionhouse.database.SQLiteManager;
import me.wobble.wauctionhouse.util.ItemSerializer;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class ExpiredRepository {

    private final SQLiteManager sqliteManager;

    public ExpiredRepository(SQLiteManager sqliteManager) {
        this.sqliteManager = sqliteManager;
    }

    public void add(UUID ownerId, ItemStack item, long expiredAt) {
        String sql = """
                INSERT INTO auction_expired_items (owner_uuid, item_data, expired_at)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, ownerId.toString());
            statement.setString(2, ItemSerializer.serialize(item));
            statement.setLong(3, expiredAt);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not add expired item", exception);
        }
    }

    public List<ExpiredEntry> findByOwner(UUID ownerId) {
        String sql = "SELECT * FROM auction_expired_items WHERE owner_uuid = ?";
        List<ExpiredEntry> entries = new ArrayList<>();

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, ownerId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new ExpiredEntry(
                            resultSet.getInt("id"),
                            UUID.fromString(resultSet.getString("owner_uuid")),
                            ItemSerializer.deserialize(resultSet.getString("item_data")),
                            resultSet.getLong("expired_at")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load expired items", exception);
        }

        entries.sort(Comparator.comparingLong(ExpiredEntry::expiredAt));
        return entries;
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM auction_expired_items WHERE id = ?";

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not delete expired item", exception);
        }
    }

    public record ExpiredEntry(int id, UUID ownerId, ItemStack item, long expiredAt) {
    }
}
