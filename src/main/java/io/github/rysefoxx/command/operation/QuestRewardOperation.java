package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.reward.QuestRewardModel;
import io.github.rysefoxx.reward.QuestRewardService;
import io.github.rysefoxx.util.Maths;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 19.05.2024
 */
@RequiredArgsConstructor
public class QuestRewardOperation implements QuestOperation {

    private final QuestService questService;
    private final QuestRewardService questRewardService;
    private final LanguageService languageService;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
            handleRewardOperation(player, args, true);
            return true;
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("remove")) {
            handleRewardOperation(player, args, false);
            return true;
        }

        return false;
    }

    private void handleRewardOperation(@NotNull Player player, String @NotNull [] args, boolean isAddOperation) {
        if (!Maths.isDataType(args[3], Long.class)) {
            languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        String name = args[2];
        long rewardId = Long.parseLong(args[3]);

        questService.findByName(name).thenCompose(questModel -> handleQuestModel(player, questModel, rewardId, isAddOperation)).exceptionally(e -> handleError(player, "Error while searching for quest", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleQuestModel(@NotNull Player player, @Nullable QuestModel questModel, @Nonnegative long rewardId, boolean isAddOperation) {
        if (questModel == null) {
            languageService.sendTranslatedMessage(player, "quest_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        return questRewardService.findById(rewardId).thenCompose(questRewardModel -> {
            if (isAddOperation) {
                return handleAddReward(player, questModel, questRewardModel, rewardId);
            } else {
                return handleRemoveReward(player, questModel, questRewardModel, rewardId);
            }
        }).exceptionally(e -> handleError(player, "Error while searching for a reward", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleAddReward(@NotNull Player player, @NotNull QuestModel questModel, @Nullable QuestRewardModel questRewardModel, @Nonnegative long rewardId) {
        if (questRewardModel == null) {
            languageService.sendTranslatedMessage(player, "quest_reward_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        if (questModel.hasReward(rewardId)) {
            languageService.sendTranslatedMessage(player, "quest_reward_already_added");
            return CompletableFuture.completedFuture(null);
        }

        questModel.getRewards().add(questRewardModel);
        return saveQuestModel(player, questModel);
    }

    private @NotNull CompletableFuture<@Nullable Void> handleRemoveReward(@NotNull Player player, @NotNull QuestModel questModel, @Nullable QuestRewardModel questRewardModel, @Nonnegative long rewardId) {
        if (questRewardModel == null) {
            languageService.sendTranslatedMessage(player, "quest_reward_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        if (!questModel.hasReward(rewardId)) {
            languageService.sendTranslatedMessage(player, "quest_reward_not_added");
            return CompletableFuture.completedFuture(null);
        }

        questModel.getRewards().removeIf(reward -> reward.getId().equals(rewardId));
        return saveQuestModel(player, questModel);
    }

    private @NotNull CompletableFuture<@Nullable Void> saveQuestModel(@NotNull Player player, @NotNull QuestModel questModel) {
        return questService.save(questModel).thenAccept(resultType -> {
            languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
        }).exceptionally(e -> handleError(player, "Error while saving reward to quest", e));
    }

    private @Nullable Void handleError(@NotNull Player player, @NotNull String message, @NotNull Throwable throwable) {
        player.sendRichMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
