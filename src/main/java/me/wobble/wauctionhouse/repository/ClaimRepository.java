package me.wobble.wauctionhouse.repository;

import me.wobble.wauctionhouse.database.SQLiteManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class ClaimRepository {

    private final SQLiteManager sqliteManager;

    public ClaimRepository(SQLiteManager sqliteManager) {
        this.sqliteManager = sqliteManager;
    }

    public double getAmount(UUID playerId) {
        String sql = "SELECT amount FROM auction_claims WHERE player_uuid = ?";

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("amount");
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not get claim amount", exception);
        }

        return 0.0;
    }

    public void add(UUID playerId, double amount) {
        double existing = getAmount(playerId);
        double updated = existing + amount;

        String sql = """
                INSERT INTO auction_claims (player_uuid, amount)
                VALUES (?, ?)
                ON CONFLICT(player_uuid)
                DO UPDATE SET amount = excluded.amount
                """;

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.setDouble(2, updated);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not add claim amount", exception);
        }
    }

    public void clear(UUID playerId) {
        String sql = "DELETE FROM auction_claims WHERE player_uuid = ?";

        try (PreparedStatement statement = sqliteManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not clear claim amount", exception);
        }
    }
}
