package io.github.rysefoxx.scoreboard;


import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.scoreboard.enums.ScoreboardType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * @author Rysefoxx
 * @since 18.05.2024
 */
public abstract class AbstractScoreboard {

    /**
     * The type of the scoreboard. This is used to check what scoreboard the player has.
     *
     * @return The type of the scoreboard.
     */
    public abstract ScoreboardType getType();

    /**
     * The title of the scoreboard.
     *
     * @return The title of the scoreboard.
     */
    public abstract String getTitle();

    /**
     * The lines of the scoreboard.
     *
     * @param player          The player. His locale is used to translate the lines.
     * @param languageService The {@link LanguageService} to translate the lines.
     * @return The lines of the scoreboard.
     */
    public abstract HashMap<String, ScoreboardEntry> getLines(@NotNull Player player, @NotNull LanguageService languageService);
}
