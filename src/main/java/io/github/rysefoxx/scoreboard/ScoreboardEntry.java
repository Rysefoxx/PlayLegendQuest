package io.github.rysefoxx.scoreboard;

import io.github.rysefoxx.scoreboard.enums.ScoreboardPredefinedValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;

/**
 * @author Rysefoxx
 * @since 18.05.2024
 */
public record ScoreboardEntry(@Nullable String entry, int entrySlot, String display, int displaySlot,
                              @Nullable ScoreboardPredefinedValue predefinedValue) {

    /**
     * Creates a new scoreboard entry. This is used to create a new line for the scoreboard.
     *
     * @param entry           The entry of the scoreboard. This is used to identify the line.
     * @param entrySlot       The slot of the entry. There the value gets displayed.
     * @param display         The display of the scoreboard. This is used to display a text above the entry line.
     * @param displaySlot     The slot of the display. There the value gets displayed.
     * @param predefinedValue The predefined value of the scoreboard. This is used to fill the entry line with a predefined value.
     */
    public ScoreboardEntry(@Nullable String entry, int entrySlot, @NotNull String display, @Nonnegative int displaySlot, @Nullable ScoreboardPredefinedValue predefinedValue) {
        this.entry = entry;
        this.entrySlot = entrySlot;
        this.display = display;
        this.displaySlot = displaySlot;
        this.predefinedValue = predefinedValue;
    }
}
