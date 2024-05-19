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

    private @NotNull CompletableFuture<@Nullable Void> handleQuestModel(@NotNull Player player, @Nullable QuestModel questModel, @NotNull String name) {
        if (questModel == null) {
            languageService.sendTranslatedMessage(player, "quest_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        return questUserProgressService.findByUuid(player.getUniqueId())
                .thenCompose(questUserProgressModels -> handleQuestUserProgress(player, questUserProgressModels, name))
                .exceptionally(e -> handleError(player, "Error while searching for quest user progress", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleQuestUserProgress(@NotNull Player player, @Nullable List<QuestUserProgressModel> questUserProgressModels, @NotNull String name) {
        if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
            languageService.sendTranslatedMessage(player, "quest_no_active");
            return CompletableFuture.completedFuture(null);
        }

        QuestUserProgressModel questUserProgressModel = questUserProgressModels.getFirst();
        QuestModel quest = questUserProgressModel.getQuest();

        if (!quest.getName().equalsIgnoreCase(name)) {
            languageService.sendTranslatedMessage(player, "quest_not_active", name);
            return CompletableFuture.completedFuture(null);
        }

        quest.getUserProgress().remove(questUserProgressModel);
        quest.getUserQuests().removeIf(questUserModel -> questUserModel.getUuid().equals(player.getUniqueId()));

        return questUserProgressService.deleteQuest(player.getUniqueId(), name)
                .thenCompose(progressResultType -> handleDeleteQuest(player, progressResultType, quest))
                .exceptionally(e -> handleError(player, "Error while canceling quest", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleDeleteQuest(@NotNull Player player, @NotNull ResultType progressResultType, @NotNull QuestModel quest) {
        return questService.getCache().synchronous().refresh(quest.getName())
                .thenRun(() -> {
                    scoreboardService.update(player);
                    languageService.sendTranslatedMessage(player, "quest_canceled_" + progressResultType.toString().toLowerCase());
                });
    }

    private @Nullable Void handleError(@NotNull Player player, @NotNull String message, @NotNull Throwable throwable) {
        player.sendRichMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
