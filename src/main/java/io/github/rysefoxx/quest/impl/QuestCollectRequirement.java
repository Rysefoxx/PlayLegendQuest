package io.github.rysefoxx.quest.impl;

import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.quest.AbstractQuestRequirement;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;

/**
 * @author Rysefoxx
 * @since 17.05.2024
 */
@Entity
@NoArgsConstructor
@DiscriminatorValue("COLLECT")
public class QuestCollectRequirement extends AbstractQuestRequirement {

    @Column(length = 90)
    @Enumerated(EnumType.STRING)
    private Material material;

    public QuestCollectRequirement(@Nonnegative int requiredAmount, @NotNull Material material) {
        super(requiredAmount, QuestRequirementType.COLLECT);
        this.material = material;
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
        languageService.sendTranslatedMessage(player, "quest_requirement_info_material", this.material.toString());
    }

    @Override
    public @NotNull String getProgressText(@NotNull QuestUserProgressModel questUserProgressModel) {
        return getId() + ": " + questUserProgressModel.getProgress() + "/" + getRequiredAmount() + " (" + getQuestRequirementType().toString() + " " + getRequiredAmount() + " " + this.material.toString() + ")";
    }
}