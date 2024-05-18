package io.github.rysefoxx.listener;

import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestModel;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Rysefoxx
 * @since 18.05.2024
 */
@RequiredArgsConstructor
public class SignChangeListener implements Listener {

    private static final PlainTextComponentSerializer SERIALIZER = PlainTextComponentSerializer.plainText();
    private final QuestUserProgressService questUserProgressService;
    private final LanguageService languageService;

    @EventHandler
    private void onSignChange(@NotNull SignChangeEvent event) {
        Player player = event.getPlayer();
        List<Component> lines = event.lines();

        for (Component line : lines) {
            if (line == null) continue;

            String text = SERIALIZER.serialize(line);
            Player target = Bukkit.getPlayerExact(text);
            if (target == null) continue;

            event.line(0, Component.text("Quest"));
            event.line(1, Component.text("Loading..."));
            event.line(2, Component.text(this.languageService.getTranslatedMessage(player, "sign_change_progress")));
            event.line(3, Component.text("Loading..."));

            this.questUserProgressService.findByUuid(target.getUniqueId()).thenAccept(questUserProgressModels -> {
                if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
                    event.line(1, Component.text(this.languageService.getTranslatedMessage(player, "quest_no_active")));
                    return;
                }

                QuestModel questModel = questUserProgressModels.getFirst().getQuest();
                event.line(1, Component.text(questModel.getName()));
                event.line(3, Component.text(questModel.getCompletedRequirementsCount(questUserProgressModels) + "/" + questModel.getRequirements().size()));
            });
            break;
        }
    }
}