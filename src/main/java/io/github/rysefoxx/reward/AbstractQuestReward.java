package io.github.rysefoxx.reward;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public abstract class AbstractQuestReward<T> {

    public abstract @NotNull QuestRewardModel<T> buildQuestRewardModel(@NotNull Player player, @NotNull String[] args);

    public abstract @NotNull String genericRewardToString(@NotNull Player player, @NotNull String[] args);

    public abstract @Nullable T rewardStringToGeneric(@NotNull String reward);

    public abstract void rewardPlayer(@NotNull Player player, @NotNull T reward);

}