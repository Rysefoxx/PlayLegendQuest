package io.github.rysefoxx.command.tabcomplete;

import io.github.rysefoxx.enums.QuestRewardType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Rysefoxx
 * @since 19.05.2024
 */
public class TabCompleteQuestReward implements TabCompleter {

    private static final List<String> MAIN_COMMANDS = Arrays.asList("create", "delete", "update");
    private static final List<String> REWARD_TYPES = Arrays.stream(QuestRewardType.values()).map(Enum::toString).toList();

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1:
                completions.addAll(MAIN_COMMANDS);
                break;
            case 2:
                if (args[0].equalsIgnoreCase("create")) {
                    completions.addAll(REWARD_TYPES);
                    break;
                }

                if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("update")) {
                    completions.add("<ID>");
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("update")) {
                    completions.addAll(REWARD_TYPES);
                    break;
                }

                if (args[0].equalsIgnoreCase("create")) {
                    completions.add("(<Reward>)");
                }
                break;
            case 4:
                if (args[0].equalsIgnoreCase("update")) {
                    completions.add("(<Reward>)");
                }
                break;
        }

        if (args.length > 0) {
            String currentArg = args[args.length - 1].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(currentArg));
        }
        return completions;
    }
}