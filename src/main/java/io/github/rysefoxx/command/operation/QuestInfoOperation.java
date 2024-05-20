package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.util.LogUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while finding user progress for user " + player.getName(), throwable));

        return false;
    }

    /**
     * Handles the quest user progress. If the quest user progress is null or empty, the player will receive a message, that no quest is active.
     *
     * @param player                  The player who executed the command.
     * @param questUserProgressModels The quest user progress models.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleQuestUserProgress(@NotNull Player player, @Nullable List<QuestUserProgressModel> questUserProgressModels) {
        if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
            languageService.sendTranslatedMessage(player, "quest_no_active");
            return CompletableFuture.completedFuture(null);
        }

        QuestModel questModel = questUserProgressModels.get(0).getQuest();
        questModel.sendProgressToUser(player, languageService, questUserProgressModels);
        return CompletableFuture.completedFuture(null);
    }
}
