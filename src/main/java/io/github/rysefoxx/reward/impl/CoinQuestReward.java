package io.github.rysefoxx.reward.impl;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRewardType;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.reward.AbstractQuestReward;
import io.github.rysefoxx.reward.QuestRewardModel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class CoinQuestReward extends AbstractQuestReward<Long> {

    public CoinQuestReward(@NotNull PlayLegendQuest plugin) {
        super(plugin);
    }

    @Override
    public @NotNull QuestRewardModel buildQuestRewardModel(@NotNull Player player, @NotNull String[] args) {
        long coins = Long.parseLong(args[2]);
        return new QuestRewardModel(QuestRewardType.COINS, String.valueOf(coins));
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
    public void rewardPlayer(@NotNull Player player, @Nullable Long reward) {
        if (reward == null) {
            getLanguageService().sendTranslatedMessage(player, "quest_reward_null");
            return;
        }

        getPlayerStatisticsService().getPlayerStats(player.getUniqueId()).thenAccept(playerStatisticsModel -> {
            if (playerStatisticsModel == null) {
                player.sendRichMessage("Failed to get player statistics model");
                getPlugin().getLogger().severe("Failed to get player statistics model");
                return;
            }

            playerStatisticsModel.addCoins(reward);
            getPlayerStatisticsService().save(playerStatisticsModel).thenAccept(resultType -> {
                if (resultType != ResultType.ERROR) return;
                getLanguageService().sendTranslatedMessage(player, "quest_reward_error");
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save player statistics model");
            }).exceptionally(e -> {
                player.sendRichMessage("Failed to save player statistics model");
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save player statistics model", e);
                return null;
            });
        }).exceptionally(e -> {
            player.sendRichMessage("Failed to get player statistics model");
            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to get player statistics model", e);
            return null;
        });
    }
}