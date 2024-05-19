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
                .exceptionally(throwable -> handleError(player, "Error while searching for quest", throwable));

        return false;
    }

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
                .exceptionally(e -> handleError(player, "Error while searching for quest user progress", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleHasQuest(@NotNull Player player, boolean hasQuest, @NotNull String name, @NotNull QuestModel questModel) {
        if (hasQuest) {
            languageService.sendTranslatedMessage(player, "quest_already_active");
            return CompletableFuture.completedFuture(null);
        }

        return questUserProgressService.isQuestCompleted(player.getUniqueId(), name)
                .thenCompose(isCompleted -> handleIsQuestCompleted(player, isCompleted, questModel))
                .exceptionally(e -> handleError(player, "Error while checking if quest is completed", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleIsQuestCompleted(@NotNull Player player, boolean isCompleted, @NotNull QuestModel questModel) {
        if (isCompleted) {
            languageService.sendTranslatedMessage(player, "quest_already_completed");
            return CompletableFuture.completedFuture(null);
        }

        QuestUserModel questUserModel = new QuestUserModel(player.getUniqueId(), questModel);
        return questUserService.save(questUserModel)
                .thenCompose(userResultType -> handleSaveUser(player, userResultType, questModel))
                .exceptionally(e -> handleError(player, "Error while accepting quest", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleSaveUser(@NotNull Player player, @NotNull ResultType userResultType, @NotNull QuestModel questModel) {
        if (userResultType != ResultType.SUCCESS) {
            languageService.sendTranslatedMessage(player, "quest_save_failed");
            return CompletableFuture.completedFuture(null);
        }

        return questService.save(questModel)
                .thenCompose(questResultType -> handleSaveQuest(player, questResultType, questModel))
                .exceptionally(e -> handleError(player, "Error while accepting quest", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleSaveQuest(@NotNull Player player, @NotNull ResultType questResultType, @NotNull QuestModel questModel) {
        List<CompletableFuture<ResultType>> futures = new ArrayList<>();
        for (AbstractQuestRequirement requirement : questModel.getRequirements()) {
            QuestUserProgressModel questUserProgressModel = new QuestUserProgressModel(player.getUniqueId(), questModel, requirement);
            futures.add(questUserProgressService.save(questUserProgressModel));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    scoreboardService.update(player);
                    languageService.sendTranslatedMessage(player, "quest_accepted_" + questResultType.toString().toLowerCase());
                })
                .exceptionally(e -> handleError(player, "Error while accepting quest", e));
    }

    private @Nullable Void handleError(@NotNull Player player, @NotNull String message, @NotNull Throwable throwable) {
        player.sendMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
