package io.github.rysefoxx.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@UtilityClass
public class Maths {

    /**
     * Checks if the given value is a number of the given class.
     *
     * @param value The value to check.
     * @param clazz The class to check for.
     * @return True if the value is a number of the given class, false otherwise.
     */
    public boolean isDataType(@NotNull String value, @NotNull Class<?> clazz) {
        try {
            if (clazz == Long.class) {
                Long.parseLong(value);
            } else if (clazz == Double.class) {
                Double.parseDouble(value);
            } else if(clazz == Integer.class) {
                Integer.parseInt(value);
            }
        } catch (NumberFormatException exception) {
            return false;
        }
        return true;
    }

}