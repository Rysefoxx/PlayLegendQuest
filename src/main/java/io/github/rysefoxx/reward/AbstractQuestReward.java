package io.github.rysefoxx.reward;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.stats.PlayerStatisticsService;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@Getter
@Setter
public abstract class AbstractQuestReward<T> {

    private transient PlayLegendQuest plugin;
    private transient QuestRewardService questRewardService;
    private transient LanguageService languageService;
    private transient PlayerStatisticsService playerStatisticsService;

    /**
     * Creates a new AbstractQuestReward and registers all services
     */
    public AbstractQuestReward(@NotNull PlayLegendQuest plugin) {
        this.plugin = plugin;
        register();
    }

    /**
     * Build the quest reward model.
     *
     * @param player The player to build the quest reward model for
     * @param args   The arguments
     * @return The quest reward model
     */
    public abstract @NotNull QuestRewardModel buildQuestRewardModel(@NotNull Player player, @NotNull String[] args);

    /**
     * Convert the reward to a string.
     *
     * @param player The player for the reward type ITEMS
     * @param args   The arguments
     * @return The reward string
     */
    public abstract @NotNull String genericRewardToString(@NotNull Player player, @NotNull String[] args);

    /**
     * Convert the reward string to a generic reward.
     *
     * @param reward The reward string
     * @return The generic reward
     */
    public abstract @Nullable T rewardStringToGeneric(@NotNull String reward);

    /**
     * Reward the player with the reward
     *
     * @param player The player to reward
     * @param reward The reward to give
     */
    public abstract void rewardPlayer(@NotNull Player player, @Nullable T reward);

    /**
     * Register all services when the plugin is enabled
     */
    public void register() {
        this.languageService = this.plugin.getLanguageService();
        this.questRewardService = this.plugin.getQuestRewardService();
        this.playerStatisticsService = this.plugin.getPlayerStatisticsService();
    }
}