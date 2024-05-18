package io.github.rysefoxx.scoreboard;

import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.scoreboard.enums.ScoreboardPredefinedValue;
import io.github.rysefoxx.scoreboard.impl.QuestScoreboard;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Rysefoxx
 * @since 18.05.2024
 */
public class ScoreboardService {

    private final HashMap<UUID, AbstractScoreboard> playerScoreboard = new HashMap<>();

    private final QuestUserProgressService questUserProgressService;
    private final LanguageService languageService;

    public ScoreboardService(@NotNull QuestUserProgressService questUserProgressService, @NotNull LanguageService languageService) {
        this.questUserProgressService = questUserProgressService;
        this.languageService = languageService;
    }

    /**
     * Creates a scoreboard for a player. When the player already has a scoreboard, nothing will happen.
     *
     * @param player The player to create the scoreboard for.
     */
    public void create(@NotNull Player player) {
        if (hasScoreboard(player)) return;

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.playerScoreboard.put(player.getUniqueId(), new QuestScoreboard());

        player.setScoreboard(scoreboard);
        createSidebar(player);
    }

    /**
     * Creates the sidebar for a player.
     *
     * @param player The player to create the sidebar for.
     */
    private void createSidebar(@NotNull Player player) {
        AbstractScoreboard abstractScoreboard = this.playerScoreboard.get(player.getUniqueId());
        Scoreboard bukkitScoreboard = player.getScoreboard();

        Objective sidebar = bukkitScoreboard.registerNewObjective("Sidebar", Criteria.DUMMY, Component.text(abstractScoreboard.getTitle()));
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);

        for (Map.Entry<String, ScoreboardEntry> entry : abstractScoreboard.getLines(player, this.languageService).entrySet()) {
            ScoreboardEntry scoreboardEntry = entry.getValue();

            Team team = bukkitScoreboard.registerNewTeam(entry.getKey());
            if (scoreboardEntry.entry() != null) {
                String entryName = scoreboardEntry.entry().substring(1);
                team.addEntry(entryName);
            }

            sidebar.getScore(scoreboardEntry.display()).setScore(scoreboardEntry.displaySlot());

            if (scoreboardEntry.entry() != null) {
                String entryName = scoreboardEntry.entry().substring(1);
                sidebar.getScore(entryName).setScore(scoreboardEntry.entrySlot());
            }
        }
        updateSidebar(player);
    }

    /**
     * Destroys the scoreboard for a player. When the player does not have a scoreboard, nothing will happen.
     *
     * @param player The player to destroy the scoreboard for.
     */
    public void destroy(@NotNull Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        scoreboard.getObjectives().forEach(Objective::unregister);
        scoreboard.getTeams().forEach(Team::unregister);

        this.playerScoreboard.remove(player.getUniqueId());
    }

    /**
     * Updates the scoreboard for a player.
     *
     * @param player The player to update the scoreboard for.
     */
    public void update(@NotNull Player player) {
//        if (PlayLegendQuest.isUnitTest()) return;

        updateSidebar(player);
    }

    /**
     * Updates the sidebar for a player. When the player does not have a scoreboard, it will be created.
     *
     * @param player The player to update the sidebar for.
     */
    private void updateSidebar(@NotNull Player player) {
        if (!hasScoreboard(player)) {
            create(player);
        }

        AbstractScoreboard abstractScoreboard = this.playerScoreboard.get(player.getUniqueId());
        Scoreboard scoreboard = player.getScoreboard();
        Objective sidebar = scoreboard.getObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return;
        }

        this.questUserProgressService.findByUuid(player.getUniqueId()).thenAccept(questUserProgressModels -> {
            QuestModel questModel = !questUserProgressModels.isEmpty() ? questUserProgressModels.getFirst().getQuest() : null;
            for (Map.Entry<String, ScoreboardEntry> entry : abstractScoreboard.getLines(player, this.languageService).entrySet()) {
                ScoreboardEntry scoreboardEntry = entry.getValue();

                Team team = scoreboard.getTeam(entry.getKey());
                if (team == null || scoreboardEntry.predefinedValue() == null) {
                    continue;
                }

                if (scoreboardEntry.predefinedValue() == ScoreboardPredefinedValue.QUEST_NAME) {
                    String questName = questModel != null ? questModel.getDisplayName() : this.languageService.getTranslatedMessage(player, "quest_no_active");
                    Component progress = Component.text(questName);
                    team.suffix(progress);
                    continue;
                }

                if (scoreboardEntry.predefinedValue() == ScoreboardPredefinedValue.QUEST_DESCRIPTION) {
                    String questDescription = (questModel != null && questModel.getDescription() != null) ? questModel.getDescription() : this.languageService.getTranslatedMessage(player, "quest_info_no_description");
                    Component description = Component.text(questDescription);
                    team.suffix(description);
                }
            }
        });
    }

    /**
     * Checks if a player has a scoreboard.
     *
     * @param player The player to check.
     * @return {@code true} if the player has a scoreboard, otherwise {@code false}.
     */
    private boolean hasScoreboard(@NotNull Player player) {
        return this.playerScoreboard.containsKey(player.getUniqueId());
    }
}
