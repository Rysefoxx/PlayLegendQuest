package io.github.rysefoxx.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;

/**
 * @author Rysefoxx
 * @since 18.05.2024
 */
@UtilityClass
public class StringUtils {

    /**
     * @param args      The arguments to join
     * @param delimiter the delimiter
     * @param offset    The offset to start at.
     * @return the arguments as a string
     */
    public @NotNull String join(String @NotNull [] args, @NotNull String delimiter, @Nonnegative int offset) {
        String[] split = new String[args.length - offset];
        System.arraycopy(args, offset, split, 0, split.length);
        return String.join(delimiter, split);
    }

}