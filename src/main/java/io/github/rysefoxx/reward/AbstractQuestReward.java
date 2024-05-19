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

    public AbstractQuestReward(@NotNull PlayLegendQuest plugin) {
        this.plugin = plugin;
        register();
    }

    public abstract @NotNull QuestRewardModel buildQuestRewardModel(@NotNull Player player, @NotNull String[] args);

    public abstract @NotNull String genericRewardToString(@NotNull Player player, @NotNull String[] args);

    public abstract @Nullable T rewardStringToGeneric(@NotNull String reward);

    public abstract void rewardPlayer(@NotNull Player player, @Nullable T reward);

    public void register() {
        this.languageService = this.plugin.getLanguageService();
        this.questRewardService = this.plugin.getQuestRewardService();
        this.playerStatisticsService = this.plugin.getPlayerStatisticsService();
    }
}