package io.github.rysefoxx.reward.impl;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRewardType;
import io.github.rysefoxx.reward.AbstractQuestReward;
import io.github.rysefoxx.reward.QuestRewardModel;
import io.github.rysefoxx.util.ItemStackSerializer;
import io.github.rysefoxx.util.ItemUtils;
import io.github.rysefoxx.util.PlayerUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class ItemQuestReward extends AbstractQuestReward<List<ItemStack>> {

    public ItemQuestReward(@NotNull PlayLegendQuest plugin) {
        super(plugin);
    }

    @Override
    public @NotNull QuestRewardModel buildQuestRewardModel(@NotNull Player player, @NotNull String[] args) {
        List<ItemStack> itemStacks = ItemUtils.getFilteredInventory(player);
        return new QuestRewardModel(QuestRewardType.ITEMS, ItemStackSerializer.itemStackListToBase64(itemStacks));
    }

    @Override
    public @NotNull String genericRewardToString(@NotNull Player player, @NotNull String[] args) {
        List<ItemStack> itemStacks = ItemUtils.getFilteredInventory(player);
        return ItemStackSerializer.itemStackListToBase64(itemStacks);
    }

    @Override
    public @Nullable List<ItemStack> rewardStringToGeneric(@NotNull String reward) {
        try {
            return ItemStackSerializer.itemStackListFromBase64(reward);
        } catch (IOException e) {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to deserialize itemstack list from base64 string!", e);
            return null;
        }
    }

    @Override
    public void rewardPlayer(@NotNull Player player, @Nullable List<ItemStack> reward) {
        if (reward == null) {
            getLanguageService().sendTranslatedMessage(player, "quest_reward_null");
            return;
        }

        for (ItemStack itemStack : reward) {
            PlayerUtils.addItem(player, itemStack);
        }
    }
}