package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestModel;
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
public class QuestInfoOperation implements QuestOperation {

    private final QuestUserProgressService questUserProgressService;
    private final LanguageService languageService;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        questUserProgressService.findByUuid(player.getUniqueId())
                .thenCompose(questUserProgressModels -> handleQuestUserProgress(player, questUserProgressModels))
                .exceptionally(throwable -> handleError(player, throwable));

        return false;
    }

    private @NotNull CompletableFuture<@Nullable Void> handleQuestUserProgress(@NotNull Player player, @Nullable List<QuestUserProgressModel> questUserProgressModels) {
        if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
            languageService.sendTranslatedMessage(player, "quest_no_active");
            return CompletableFuture.completedFuture(null);
        }

        QuestModel questModel = questUserProgressModels.getFirst().getQuest();
        questModel.sendProgressToUser(player, languageService, questUserProgressModels);
        return CompletableFuture.completedFuture(null);
    }

    private @Nullable Void handleError(@NotNull Player player, @NotNull Throwable throwable) {
        player.sendRichMessage("Error while searching for user quest progress");
        PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for user quest progress" + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
