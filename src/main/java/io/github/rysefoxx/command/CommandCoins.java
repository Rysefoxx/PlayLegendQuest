package io.github.rysefoxx.command;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.stats.PlayerStatisticsService;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;


/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@RequiredArgsConstructor
public class CommandCoins implements CommandExecutor {

    private final LanguageService languageService;
    private final PlayerStatisticsService playerStatisticsService;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) return false;

        this.playerStatisticsService.getPlayerStats(player.getUniqueId()).thenAccept(playerStatisticsModel -> {
            if (playerStatisticsModel == null) {
                this.languageService.sendTranslatedMessage(player, "player_stats_could_not_be_loaded");
                return;
            }

            this.languageService.sendTranslatedMessage(player, "player_coins", String.valueOf(playerStatisticsModel.getCoins()));
        }).exceptionally(e -> {
            player.sendRichMessage("An error occurred while loading your stats. Please try again later.");
            PlayLegendQuest.getLog().log(Level.SEVERE, "An error occurred while loading stats for player " + player.getName(), e);
            return null;
        });

        return false;
    }
}