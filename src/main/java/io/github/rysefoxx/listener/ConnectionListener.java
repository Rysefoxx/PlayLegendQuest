package io.github.rysefoxx.listener;

import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rysefoxx
 * @since 17.05.2024
 */
@RequiredArgsConstructor
public class ConnectionListener implements Listener {

    private final QuestUserProgressService questUserProgressService;
    private final LanguageService languageService;

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        this.questUserProgressService.findByUuid(player.getUniqueId()).thenAccept(questUserProgressModel -> {
            if(questUserProgressModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_no_active");
                return;
            }

        });
    }
}