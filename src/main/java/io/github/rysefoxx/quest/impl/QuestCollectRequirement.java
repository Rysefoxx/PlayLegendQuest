package io.github.rysefoxx.quest.impl;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.quest.AbstractQuestRequirement;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;

/**
 * @author Rysefoxx
 * @since 17.05.2024
 */
@Entity
@NoArgsConstructor
@DiscriminatorValue("COLLECT")
public class QuestCollectRequirement extends AbstractQuestRequirement implements Listener {

    @Column(length = 90)
    @Enumerated(EnumType.STRING)
    private Material material;

    public QuestCollectRequirement(@NotNull PlayLegendQuest plugin, @Nonnegative int requiredAmount, @NotNull Material material) {
        super(plugin, requiredAmount, QuestRequirementType.COLLECT);
        this.material = material;
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
        return questUserProgressModel.getProgress() + "/" + getRequiredAmount() + " (" + getQuestRequirementType().toString() + " " + getRequiredAmount() + " " + this.material.toString() + ")";
    }

    @EventHandler
    private void onItemPickup(@NotNull EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack itemStack = event.getItem().getItemStack();
        if (itemStack.getType() != this.material) return;

        updateProgress(player, itemStack.getAmount());
    }
}