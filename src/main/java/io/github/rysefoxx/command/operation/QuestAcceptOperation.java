package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.AbstractQuestRequirement;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.scoreboard.ScoreboardService;
import io.github.rysefoxx.user.QuestUserModel;
import io.github.rysefoxx.user.QuestUserService;
import io.github.rysefoxx.util.LogUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 19.05.2024
 */
@RequiredArgsConstructor
public class QuestAcceptOperation implements QuestOperation {

    private final QuestService questService;
    private final LanguageService languageService;
    private final QuestUserProgressService questUserProgressService;
    private final QuestUserService questUserService;
    private final ScoreboardService scoreboardService;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        String name = args[1];

        this.questService.findByName(name)
                .thenCompose(questModel -> handleQuestModel(player, questModel, name))
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while searching for quest", throwable));

        return false;
    }

    /**
     * Handles the quest model. If the quest model is null, not configured or the player has no permission, the player will receive a message.
     *
     * @param player     The player who executed the command.
     * @param questModel The quest model.
     * @param name       The name of the quest.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleQuestModel(@NotNull Player player, @Nullable QuestModel questModel, @NotNull String name) {
        if (questModel == null) {
            languageService.sendTranslatedMessage(player, "quest_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        if (!questModel.isConfigured()) {
            languageService.sendTranslatedMessage(player, "quest_not_configured");
            return CompletableFuture.completedFuture(null);
        }

        if (questModel.hasPermission() && !player.hasPermission(questModel.getPermission())) {
            languageService.sendTranslatedMessage(player, "quest_no_permission");
            return CompletableFuture.completedFuture(null);
        }

        return questUserProgressService.hasQuest(player.getUniqueId())
                .thenCompose(hasQuest -> handleHasQuest(player, hasQuest, name, questModel))
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while searching for quest user progress", throwable));
    }

    /**
     * Handles the has quest. If the player has the quest, the player will receive a message. If the player has not the quest, the quest will be accepted.
     *
     * @param player     The player who executed the command.
     * @param hasQuest   If the player has the quest.
     * @param name       The name of the quest.
     * @param questModel The quest model.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleHasQuest(@NotNull Player player, boolean hasQuest, @NotNull String name, @NotNull QuestModel questModel) {
        if (hasQuest) {
            languageService.sendTranslatedMessage(player, "quest_already_active");
            return CompletableFuture.completedFuture(null);
        }

        return questUserProgressService.isQuestCompleted(player.getUniqueId(), name)
                .thenCompose(isCompleted -> handleIsQuestCompleted(player, isCompleted, questModel))
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while checking if quest is completed", throwable));
    }

    /**
     * Handles the is quest completed. If the quest is completed, the player will receive a message. If the quest is not completed, the quest will be accepted.
     *
     * @param player      The player who executed the command.
     * @param isCompleted If the quest is completed.
     * @param questModel  The quest model.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleIsQuestCompleted(@NotNull Player player, boolean isCompleted, @NotNull QuestModel questModel) {
        if (isCompleted) {
            languageService.sendTranslatedMessage(player, "quest_already_completed");
            return CompletableFuture.completedFuture(null);
        }

        QuestUserModel questUserModel = new QuestUserModel(player.getUniqueId(), questModel);
        return questUserService.save(questUserModel)
                .thenCompose(userResultType -> handleSaveUser(player, userResultType, questModel))
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while accepting quest", throwable));
    }

    /**
     * Handles the save user. If the user result type is not success, the player will receive a message. If the user result type is success, the quest will be saved.
     *
     * @param player         The player who executed the command.
     * @param userResultType The result type of the user.
     * @param questModel     The quest model.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleSaveUser(@NotNull Player player, @NotNull ResultType userResultType, @NotNull QuestModel questModel) {
        if (userResultType != ResultType.SUCCESS) {
            languageService.sendTranslatedMessage(player, "quest_save_failed");
            return CompletableFuture.completedFuture(null);
        }

        return questService.save(questModel)
                .thenCompose(questResultType -> handleSaveQuest(player, questResultType, questModel))
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while accepting quest", throwable));
    }

    /**
     * Handles the save quest. If the quest result type is not success, the player will receive a message. If the quest result type is success, the player will receive a message and the scoreboard will be updated.
     *
     * @param player          The player who executed the command.
     * @param questResultType The result type of the quest.
     * @param questModel      The quest model.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleSaveQuest(@NotNull Player player, @NotNull ResultType questResultType, @NotNull QuestModel questModel) {
        List<CompletableFuture<ResultType>> futures = new ArrayList<>();
        for (AbstractQuestRequirement requirement : questModel.getRequirements()) {
            QuestUserProgressModel questUserProgressModel = new QuestUserProgressModel(player.getUniqueId(), questModel, requirement);
            futures.add(questUserProgressService.save(questUserProgressModel));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenCompose(v -> questService.getCache().synchronous().refresh(questModel.getName()))
                .thenAccept(v -> {
                    scoreboardService.update(player);
                    languageService.sendTranslatedMessage(player, "quest_accepted_" + questResultType.toString().toLowerCase());
                })
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while accepting quest", throwable));
    }
}
