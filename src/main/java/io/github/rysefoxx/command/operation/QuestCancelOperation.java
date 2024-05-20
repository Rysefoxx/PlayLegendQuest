package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.scoreboard.ScoreboardService;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 19.05.2024
 */
@RequiredArgsConstructor
public class QuestCancelOperation implements QuestOperation {

    private final QuestService questService;
    private final LanguageService languageService;
    private final QuestUserProgressService questUserProgressService;
    private final ScoreboardService scoreboardService;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        String name = args[1];
        this.questService.findByName(name)
                .thenCompose(questModel -> handleQuestModel(player, questModel, name))
                .exceptionally(throwable -> handleError(player, "Error while searching for quest", throwable));

        return false;
    }

    /**
     * Handles the quest model. If the quest model is null the player will receive a message.
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

        return questUserProgressService.findByUuid(player.getUniqueId())
                .thenCompose(questUserProgressModels -> handleQuestUserProgress(player, questUserProgressModels, name))
                .exceptionally(throwable -> handleError(player, "Error while searching for quest user progress", throwable));
    }

    /**
     * Handles the quest user progress. If the quest user progress is null or empty the player will receive a message, that he has no active quest.
     *
     * @param player                  The player who executed the command.
     * @param questUserProgressModels The quest user progress models.
     * @param name                    The name of the quest.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleQuestUserProgress(@NotNull Player player, @Nullable List<QuestUserProgressModel> questUserProgressModels, @NotNull String name) {
        if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
            languageService.sendTranslatedMessage(player, "quest_no_active");
            return CompletableFuture.completedFuture(null);
        }

        QuestUserProgressModel questUserProgressModel = questUserProgressModels.get(0);
        QuestModel quest = questUserProgressModel.getQuest();

        if (!quest.getName().equalsIgnoreCase(name)) {
            languageService.sendTranslatedMessage(player, "quest_not_active", name);
            return CompletableFuture.completedFuture(null);
        }

        quest.removeUserProgress(questUserProgressModel);
        quest.getUserQuests().removeIf(questUserModel -> questUserModel.getUuid().equals(player.getUniqueId()));

        return questUserProgressService.deleteQuest(player.getUniqueId(), name)
                .thenCompose(progressResultType -> handleDeleteQuest(player, progressResultType, quest))
                .exceptionally(e -> handleError(player, "Error while canceling quest", e));
    }


    /**
     * Handles the delete quest. If the quest was successfully deleted, the player will receive a message.
     *
     * @param player             The player who executed the command.
     * @param progressResultType The result type of the progress.
     * @param quest              The quest model.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleDeleteQuest(@NotNull Player player, @NotNull ResultType progressResultType, @NotNull QuestModel quest) {
        return questService.getCache().synchronous().refresh(quest.getName())
                .thenRun(() -> {
                    scoreboardService.update(player);
                    languageService.sendTranslatedMessage(player, "quest_canceled_" + progressResultType.toString().toLowerCase());
                });
    }

    /**
     * Handles the error. The player will receive a message and the error will be logged.
     *
     * @param player    The player who executed the command.
     * @param message   The message to send to the player.
     * @param throwable The throwable to log.
     * @return null.
     */
    private @Nullable Void handleError(@NotNull Player player, @NotNull String message, @NotNull Throwable throwable) {
        player.sendRichMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
