package io.github.rysefoxx.quest.impl;

import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.quest.AbstractQuestRequirement;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;

/**
 * @author Rysefoxx
 * @since 17.05.2024
 */
@Entity
@NoArgsConstructor
@DiscriminatorValue("KILL")
public class QuestKillRequirement extends AbstractQuestRequirement {

    @Column(name = "entity_type", length = 90)
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    public QuestKillRequirement(@Nonnegative int requiredAmount, @NotNull EntityType entityType) {
        super(requiredAmount, QuestRequirementType.KILL);
        this.entityType = entityType;
    }

    @Override
    public boolean isCompleted(@NotNull Player player) {
        return false;
    }

    @Override
    public void sendInfo(@NotNull Player player, @NotNull LanguageService languageService) {
        languageService.sendTranslatedMessage(player, "quest_requirement_info", String.valueOf(getId()));
        languageService.sendTranslatedMessage(player, "quest_requirement_info_type", getQuestRequirementType().toString());
        languageService.sendTranslatedMessage(player, "quest_requirement_info_required_amount", String.valueOf(getRequiredAmount()));
        languageService.sendTranslatedMessage(player, "quest_requirement_info_entity_type", this.entityType.toString());
    }
}