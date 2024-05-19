package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestService;
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
public class QuestPermissionOperation implements QuestOperation {

    private final QuestService questService;
    private final LanguageService languageService;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        String name = args[2];
        String permission = args[3];

        questService.findByName(name)
                .thenCompose(questModel -> handleQuestModel(player, questModel, permission))
                .exceptionally(e -> handleError(player, "Error while searching for quest", e));
        return false;
    }

    /**
     * Handles the quest model. If the quest model is null the player will receive a message, that the quest does not exist.
     *
     * @param player     The player who executed the command.
     * @param questModel The quest model.
     * @param permission The permission of the quest.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleQuestModel(@NotNull Player player, @Nullable QuestModel questModel, @NotNull String permission) {
        if (questModel == null) {
            languageService.sendTranslatedMessage(player, "quest_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        questModel.setPermission(permission);
        return questService.save(questModel)
                .thenAccept(resultType -> handleSaveResult(player, resultType))
                .exceptionally(e -> handleError(player, "Error while saving permission for quest", e));
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

    /**
     * Handles an error. The player will receive a message and the error will be logged.
     *
     * @param player    The player who executed the command.
     * @param message   The message to send to the player.
     * @param throwable The error.
     * @return null
     */
    private @Nullable Void handleError(@NotNull Player player, @NotNull String message, @NotNull Throwable throwable) {
        player.sendRichMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
