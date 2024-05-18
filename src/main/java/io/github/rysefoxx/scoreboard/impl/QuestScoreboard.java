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

        lines.put("LEGEND_INTERNAL_QUEST_DESCRIPTION", new ScoreboardEntry("§2", 3, languageService.getTranslatedMessage(player, "scoreboard_quest_description"), 4, ScoreboardPredefinedValue.QUEST_DESCRIPTION));

        lines.put("LEGEND_INTERNAL_QUEST_PLACEHOLDER_1", new ScoreboardEntry(null, -1, " ", 2, null));

        lines.put("LEGEND_INTERNAL_QUEST_NAME", new ScoreboardEntry("§1", 0, languageService.getTranslatedMessage(player, "scoreboard_quest_name"), 1, ScoreboardPredefinedValue.QUEST_NAME));

        return lines;
    }
}
