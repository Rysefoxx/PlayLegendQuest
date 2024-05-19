package io.github.rysefoxx.reward.impl;

import io.github.rysefoxx.PlayLegendQuest;
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

    public ExperienceQuestReward(@NotNull PlayLegendQuest plugin) {
        super(plugin);
    }

    @Override
    public @NotNull QuestRewardModel buildQuestRewardModel(@NotNull Player player, @NotNull String[] args) {
        double experience = Double.parseDouble(args[2]);
        return new QuestRewardModel(QuestRewardType.EXPERIENCE, String.valueOf(experience));
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
    public void rewardPlayer(@NotNull Player player, @Nullable Double reward) {
        if (reward == null) {
            getLanguageService().sendTranslatedMessage(player, "quest_reward_null");
            return;
        }

        player.giveExp((int) Math.round(reward));
    }
}