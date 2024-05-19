package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.quest.QuestService;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 19.05.2024
 */
@RequiredArgsConstructor
public class QuestDeleteOperation implements QuestOperation {

    private final QuestService questService;
    private final LanguageService languageService;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        String name = args[1];
        this.questService.delete(name).thenAccept(resultType -> {
            this.languageService.sendTranslatedMessage(player, "quest_deleted_" + resultType.toString().toLowerCase());
        }).exceptionally(throwable -> {
            player.sendRichMessage("Error while deleting quest");
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error deleting quest: " + throwable.getMessage(), throwable);
            return null;
        });
        return false;
    }
}