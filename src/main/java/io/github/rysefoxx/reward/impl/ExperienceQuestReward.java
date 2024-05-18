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
public class ExperienceQuestReward extends AbstractQuestReward<Double> {

    @Override
    public @NotNull QuestRewardModel<Double> buildQuestRewardModel(@NotNull Player player, @NotNull String[] args) {
        double experience = Double.parseDouble(args[2]);
        String convertedData = String.valueOf(experience);

        return new QuestRewardModel<>(QuestRewardType.EXPERIENCE, experience, convertedData);
    }

    @Override
    public @NotNull String genericRewardToString(@NotNull Player player, @NotNull String[] args) {
        double experience = Double.parseDouble(args[3]);
        return String.valueOf(experience);
    }

    @Override
    public @Nullable Double rewardStringToGeneric(@NotNull String reward) {
        return Double.parseDouble(reward);
    }

    @Override
    public void rewardPlayer(@NotNull Player player, @NotNull Double reward) {

    }
}