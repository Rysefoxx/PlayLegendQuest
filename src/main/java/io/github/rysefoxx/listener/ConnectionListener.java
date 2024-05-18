package io.github.rysefoxx.listener;

import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestModel;
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
    private void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        this.questUserProgressService.findByUuid(player.getUniqueId()).thenAccept(questUserProgressModels -> {
            if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
                this.languageService.sendTranslatedMessage(player, "quest_no_active");
                return;
            }

            QuestModel questModel = questUserProgressModels.getFirst().getQuest();
            questModel.sendProgressToUser(player, this.languageService, questUserProgressModels);
        });
    }
}