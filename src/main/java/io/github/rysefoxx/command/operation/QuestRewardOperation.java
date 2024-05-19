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

    /**
     * Handles the reward operation. If the reward operation is not valid, the player will receive a message.
     *
     * @param player         The player who executed the command.
     * @param args           The arguments of the command.
     * @param isAddOperation If the operation is an add operation.
     */
    private void handleRewardOperation(@NotNull Player player, String @NotNull [] args, boolean isAddOperation) {
        if (!Maths.isDataType(args[3], Long.class)) {
            languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        String name = args[2];
        long rewardId = Long.parseLong(args[3]);

        questService.findByName(name).thenCompose(questModel -> handleQuestModel(player, questModel, rewardId, isAddOperation)).exceptionally(e -> handleError(player, "Error while searching for quest", e));
    }

    /**
     * Handles the quest model. If the quest model is null the player will receive a message, that the quest does not exist.
     *
     * @param player         The player who executed the command.
     * @param questModel     The quest model.
     * @param rewardId       The id of the reward.
     * @param isAddOperation If the operation is an add operation.
     * @return A completable future.
     */
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

    /**
     * Handles the add reward operation. If the reward does not exist, the player will receive a message. If the reward is already added, the player will receive a message.
     *
     * @param player           The player who executed the command.
     * @param questModel       The quest model.
     * @param questRewardModel The quest reward model.
     * @param rewardId         The id of the reward.
     * @return A completable future.
     */
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

    /**
     * Handles the remove reward operation. If the reward does not exist, the player will receive a message.
     *
     * @param player           The player who executed the command.
     * @param questModel       The quest model.
     * @param questRewardModel The quest reward model.
     * @param rewardId         The id of the reward.
     * @return A completable future.
     */
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

    /**
     * Saves the quest model. The player will receive a message about the result type.
     *
     * @param player     The player who executed the command.
     * @param questModel The quest model.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> saveQuestModel(@NotNull Player player, @NotNull QuestModel questModel) {
        return questService.save(questModel).thenAccept(resultType -> {
            languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
        }).exceptionally(e -> handleError(player, "Error while saving reward to quest", e));
    }

    /**
     * Handles an error. The player will receive a message and the error will be logged.
     *
     * @param player    The player who executed the command.
     * @param message   The message to send to the player.
     * @param throwable The throwable that occurred.
     * @return null
     */
    private @Nullable Void handleError(@NotNull Player player, @NotNull String message, @NotNull Throwable throwable) {
        player.sendRichMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
