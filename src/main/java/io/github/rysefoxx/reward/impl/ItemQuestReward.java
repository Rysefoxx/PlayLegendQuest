package io.github.rysefoxx.reward.impl;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRewardType;
import io.github.rysefoxx.reward.AbstractQuestReward;
import io.github.rysefoxx.reward.QuestRewardModel;
import io.github.rysefoxx.util.ItemStackSerializer;
import io.github.rysefoxx.util.ItemUtils;
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

    @Override
    public @NotNull QuestRewardModel<List<ItemStack>> buildQuestRewardModel(@NotNull Player player, @NotNull String[] args) {
        List<ItemStack> itemStacks = ItemUtils.getFilteredInventory(player);
        String convertedData = ItemStackSerializer.itemStackListToBase64(itemStacks);

        return new QuestRewardModel<>(QuestRewardType.ITEMS, itemStacks, convertedData);
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
    public void rewardPlayer(@NotNull Player player, @NotNull List<ItemStack> reward) {

    }
}