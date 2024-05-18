package io.github.rysefoxx.stats;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.util.UUID;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "player_stats")
public class PlayerStatisticsModel {

    @Id
    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(36)")
    private UUID uuid;

    @Column(nullable = false)
    private long coins;

    /**
     * Creates a new PlayerStatisticsModel with the given UUID and 0 coins.
     *
     * @param uuid The UUID of the player.
     */
    public PlayerStatisticsModel(@NotNull UUID uuid) {
        this.uuid = uuid;
        this.coins = 0;
    }

    /**
     * Adds coins to the player's balance.
     *
     * @param coins The amount of coins to add.
     */
    public void addCoins(@Nonnegative long coins) {
        this.coins += coins;
    }
}