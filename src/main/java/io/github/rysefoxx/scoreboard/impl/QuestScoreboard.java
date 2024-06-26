package io.github.rysefoxx.scoreboard.impl;

import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.scoreboard.AbstractScoreboard;
import io.github.rysefoxx.scoreboard.ScoreboardEntry;
import io.github.rysefoxx.scoreboard.enums.ScoreboardPredefinedValue;
import io.github.rysefoxx.scoreboard.enums.ScoreboardType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * @author Rysefoxx
 * @since 18.05.2024
 */
public class QuestScoreboard extends AbstractScoreboard {

    @Override
    public ScoreboardType getType() {
        return ScoreboardType.QUEST;
    }

    @Override
    public String getTitle() {
        return "PlayLegend Quest";
    }

    @Override
    public HashMap<String, ScoreboardEntry> getLines(@NotNull Player player, @NotNull LanguageService languageService) {
        HashMap<String, ScoreboardEntry> lines = new HashMap<>();

        lines.put("LEGEND_INTERNAL_QUEST_NAME", new ScoreboardEntry("§4", 9, languageService.getTranslatedMessage(player, "scoreboard_quest_name"), 10, ScoreboardPredefinedValue.QUEST_NAME));

        lines.put("LEGEND_INTERNAL_QUEST_PLACEHOLDER_3", new ScoreboardEntry(null, -1, "   ", 8, null));

        lines.put("LEGEND_INTERNAL_QUEST_PROGRESS", new ScoreboardEntry("§3", 6, languageService.getTranslatedMessage(player, "scoreboard_quest_progress"), 7, ScoreboardPredefinedValue.QUEST_PROGRESS));

        lines.put("LEGEND_INTERNAL_QUEST_PLACEHOLDER_2", new ScoreboardEntry(null, -1, "  ", 5, null));

        lines.put("LEGEND_INTERNAL_QUEST_TIME", new ScoreboardEntry("§2", 3, languageService.getTranslatedMessage(player, "scoreboard_quest_remaining_timer"), 4, ScoreboardPredefinedValue.QUEST_REMAINING_TIME));

        lines.put("LEGEND_INTERNAL_QUEST_PLACEHOLDER_1", new ScoreboardEntry(null, -1, " ", 2, null));

        lines.put("LEGEND_INTERNAL_QUEST_DESCRIPTION", new ScoreboardEntry("§1", 0, languageService.getTranslatedMessage(player, "scoreboard_quest_description"), 1, ScoreboardPredefinedValue.QUEST_DESCRIPTION));

        return lines;
    }
}
