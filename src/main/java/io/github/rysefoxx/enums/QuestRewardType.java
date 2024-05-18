package io.github.rysefoxx.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@RequiredArgsConstructor
@Getter
public enum QuestRewardType {

    COINS(Long.class),
    ITEMS(List.class),
    EXPERIENCE(Double.class);

    private final Class<?> classType;

    public static @Nullable QuestRewardType getQuestRewardType(@NotNull String type) {
        return Arrays.stream(values())
                .filter(questRewardType -> questRewardType.toString().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }

}
