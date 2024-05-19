package io.github.rysefoxx.quest.impl;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.quest.AbstractQuestRequirement;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;

/**
 * @author Rysefoxx
 * @since 17.05.2024
 */
@jakarta.persistence.Entity
@NoArgsConstructor
@DiscriminatorValue("KILL")
public class QuestKillRequirement extends AbstractQuestRequirement implements Listener {

    @Column(name = "entity_type", length = 90)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    public QuestKillRequirement(@NotNull PlayLegendQuest plugin, @Nonnegative int requiredAmount, @NotNull EntityType entityType) {
        super(plugin, requiredAmount, QuestRequirementType.KILL);
        this.entityType = entityType;
    }

    @Override
    public void sendInfo(@NotNull Player player, @NotNull LanguageService languageService) {
        languageService.sendTranslatedMessage(player, "quest_requirement_info", String.valueOf(getId()));
        languageService.sendTranslatedMessage(player, "quest_requirement_info_type", getQuestRequirementType().toString());
        languageService.sendTranslatedMessage(player, "quest_requirement_info_required_amount", String.valueOf(getRequiredAmount()));
        languageService.sendTranslatedMessage(player, "quest_requirement_info_entity_type", this.entityType.toString());
    }

    @Override
    public @NotNull String getProgressText(@NotNull QuestUserProgressModel questUserProgressModel) {
        return questUserProgressModel.getProgress() + "/" + getRequiredAmount() + " (" + getQuestRequirementType().toString() + " " + getRequiredAmount() + " " + this.entityType.toString() + ")";
    }

    @EventHandler
    private void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (event.getEntityType() != this.entityType) return;
        Entity entity = event.getEntity();
        if(!(entity instanceof Player player)) return;

        updateProgress(player, 1);
    }
}