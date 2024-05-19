package io.github.rysefoxx.util;

import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rysefoxx
 * @since 18.05.2024
 */
@UtilityClass
public class PlayerUtils {

    /**
     * If the player's inventory is full, drop the item on the ground, otherwise add it to the player's inventory
     *
     * @param player The player to add the item to.
     * @param item   The item to add to the player's inventory.
     */
    public void addItem(@NotNull Player player, @NotNull ItemStack item) {
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
        } else {
            player.getInventory().addItem(item.clone());
        }
    }

}