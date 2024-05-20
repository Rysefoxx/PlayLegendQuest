package io.github.rysefoxx.util;

import io.github.rysefoxx.PlayLegendQuest;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 20.05.2024
 */
@UtilityClass
public class LogUtils {

    /**
     * Handles the error. The player will receive a message and the error will be logged.
     *
     * @param player    The player who executed the command.
     * @param message   The message to send to the player.
     * @param throwable The throwable to log.
     * @return null.
     */
    public @Nullable Void handleError(@Nullable Player player, @NotNull String message, @NotNull Throwable throwable) {
        if (player != null) player.sendMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + ": " + throwable.getMessage(), throwable);
        return null;
    }

}