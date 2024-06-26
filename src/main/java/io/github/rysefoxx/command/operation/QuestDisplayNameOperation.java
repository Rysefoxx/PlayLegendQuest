package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.util.LogUtils;
import io.github.rysefoxx.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 19.05.2024
 */
@RequiredArgsConstructor
public class QuestDisplayNameOperation implements QuestOperation {

    private final QuestService questService;
    private final LanguageService languageService;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        String name = args[2];
        String displayName = StringUtils.join(args, " ", 3);

        questService.findByName(name)
                .thenCompose(questModel -> handleQuestModel(player, questModel, displayName))
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while searching for quest", throwable));

        return false;
    }

    /**
     * Handles the quest model. If the quest model is null the player will receive a message, that the quest does not exist.
     *
     * @param player      The player who executed the command.
     * @param questModel  The quest model.
     * @param displayName The display name of the quest.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleQuestModel(@NotNull Player player, @Nullable QuestModel questModel, @NotNull String displayName) {
        if (questModel == null) {
            languageService.sendTranslatedMessage(player, "quest_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        questModel.setDisplayName(displayName);
        return questService.save(questModel)
                .thenAccept(resultType -> handleSaveResult(player, resultType))
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while saving display name for quest", throwable));
    }

    /**
     * Handles the result type. The player will receive a message, about the result type.
     *
     * @param player     The player who executed the command.
     * @param resultType The result type.
     */
    private void handleSaveResult(@NotNull Player player, @NotNull ResultType resultType) {
        languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
    }
}
