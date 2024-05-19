package io.github.rysefoxx.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class ItemStackSerializer {

    /**
     * Converts an {@link ItemStack} array to a Base64 encoded string.
     *
     * @param items The item stack array to convert.
     * @return The Base64 encoded string.
     * @throws IllegalStateException If the item stack array cannot be saved.
     */
    @Contract("null -> null")
    public static String itemStackListToBase64(@Nullable List<ItemStack> items) throws IllegalStateException {
        if (items == null) return null;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.size());

            for (ItemStack item : items)
                dataOutput.writeObject(item);

            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    /**
     * Converts a Base64 encoded string to an {@link ItemStack} array.
     *
     * @param data The Base64 encoded string to convert.
     * @return The item stack array.
     * @throws IOException If the Base64 encoded string cannot be decoded.
     */
    @Contract("null -> null")
    public static List<ItemStack> itemStackListFromBase64(@Nullable String data) throws IOException {
        if (data == null) return null;

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int length = dataInput.readInt();
            List<ItemStack> items = new ArrayList<>(length);

            for (int i = 0; i < length; i++)
                items.add((ItemStack) dataInput.readObject());

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }
}
