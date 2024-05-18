package io.github.rysefoxx.enums;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author Rysefoxx
 * @since 17.05.2024
 */
public enum QuestRequirementType {

    KILL,
    COLLECT;


    public static @Nullable QuestRequirementType getQuestRequirementType(@NotNull String type) {
        return Arrays.stream(values())
                .filter(questRewardType -> questRewardType.toString().equalsIgnoreCase(type))
                .findFirst()
                .orElse(null);
    }

}
