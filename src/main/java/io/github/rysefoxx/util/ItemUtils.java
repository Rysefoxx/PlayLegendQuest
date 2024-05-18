package io.github.rysefoxx.util;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@UtilityClass
public class ItemUtils {

    /**
     * Returns a filtered inventory of the player. All items that are null or air will be removed.
     *
     * @param player The player to get the inventory from.
     * @return A list of all items in the player's inventory that are not null or air.
     */
    public @NotNull List<ItemStack> getFilteredInventory(@NotNull Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        return new ArrayList<>(Arrays.stream(contents).filter(itemStack -> !isNullOrAir(itemStack)).toList());
    }

    /**
     * Checks if the item stack is null or air.
     *
     * @param itemStack The item stack to check.
     * @return True if the item stack is null or air, false otherwise.
     */
    private boolean isNullOrAir(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.getType().isAir();
    }
}