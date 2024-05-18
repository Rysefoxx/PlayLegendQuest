package io.github.rysefoxx.reward.impl;

import io.github.rysefoxx.enums.QuestRewardType;
import io.github.rysefoxx.reward.AbstractQuestReward;
import io.github.rysefoxx.reward.QuestRewardModel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class CoinQuestReward extends AbstractQuestReward<Long> {

    @Override
    public @NotNull QuestRewardModel<Long> buildQuestRewardModel(@NotNull Player player, @NotNull String[] args) {
        long coins = Long.parseLong(args[2]);
        String convertedData = String.valueOf(coins);

        return new QuestRewardModel<>(QuestRewardType.COINS, coins, convertedData);
    }

    @Override
    public @NotNull String genericRewardToString(@NotNull Player player, @NotNull String[] args) {
        long coins = Long.parseLong(args[3]);
        return String.valueOf(coins);
    }

    @Override
    public @Nullable Long rewardStringToGeneric(@NotNull String reward) {
        return Long.parseLong(reward);
    }

    @Override
    public void rewardPlayer(@NotNull Player player, @NotNull Long reward) {

    }
}