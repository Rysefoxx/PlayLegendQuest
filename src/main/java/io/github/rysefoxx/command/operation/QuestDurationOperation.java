package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.util.TimeUtils;
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
public class QuestDurationOperation implements QuestOperation {

    private final QuestService questService;
    private final LanguageService languageService;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        String durationString = args[3];
        long seconds = TimeUtils.parseDurationToSeconds(durationString);

        if (seconds == 0) {
            languageService.sendTranslatedMessage(player, "quest_duration_invalid");
            return false;
        }

        String name = args[2];
        questService.findByName(name)
                .thenCompose(questModel -> handleQuestModel(player, questModel, seconds))
                .exceptionally(throwable -> handleError(player, "Error while searching for quest", throwable));

        return false;
    }

    /**
     * Handles the quest model. If the quest model is null the player will receive a message, that the quest does not exist.
     *
     * @param player     The player who executed the command.
     * @param questModel The quest model.
     * @param seconds    The duration of the quest.
     * @return A completable future.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleQuestModel(@NotNull Player player, @Nullable QuestModel questModel, @Nonnegative long seconds) {
        if (questModel == null) {
            languageService.sendTranslatedMessage(player, "quest_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        questModel.setDuration(seconds);
        return questService.save(questModel)
                .thenAccept(resultType -> handleSaveResult(player, resultType))
                .exceptionally(e -> handleError(player, "Error while saving duration for quest", e));
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
     * @param throwable The throwable that occurred.
     * @return null
     */
    private @Nullable Void handleError(@NotNull Player player, @NotNull String message, @NotNull Throwable throwable) {
        player.sendRichMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
