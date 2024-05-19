package io.github.rysefoxx.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
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

    public static @Nullable String itemStackToBase64(@NotNull ItemStack itemStack) {
        String base64 = null;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitOut = new BukkitObjectOutputStream(out);
            bukkitOut.writeObject(itemStack);
            bukkitOut.close();
            base64 = Base64Coder.encodeLines(out.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return base64;
    }

    public static @Nullable ItemStack itemStackFromBase64(String base64) {
        ItemStack result = null;
        ByteArrayInputStream in = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
        try {
            BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(in);
            result = (ItemStack) bukkitIn.readObject();
            bukkitIn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

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
