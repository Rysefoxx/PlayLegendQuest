package io.github.rysefoxx.command.tabcomplete;

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
public class TabCompleteQuest implements TabCompleter {

    private static final List<String> MAIN_COMMANDS = Arrays.asList("create", "delete", "accept", "cancel", "update", "reward", "requirement", "info");
    private static final List<String> UPDATE_SUB_COMMANDS = Arrays.asList("displayname", "description", "duration", "permission");
    private static final List<String> REWARD_SUB_COMMANDS = Arrays.asList("add", "remove");
    private static final List<String> REQUIREMENT_SUB_COMMANDS = Arrays.asList("add", "remove", "info");

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        switch (args.length) {
            case 1:
                completions.addAll(MAIN_COMMANDS);
                break;
            case 2: {
                String firstArg = args[0].toLowerCase();
                switch (firstArg) {
                    case "update" -> completions.addAll(UPDATE_SUB_COMMANDS);
                    case "reward" -> completions.addAll(REWARD_SUB_COMMANDS);
                    case "requirement" -> completions.addAll(REQUIREMENT_SUB_COMMANDS);
                }
            }
            break;
            case 3: {
                String firstArg = args[0].toLowerCase();
                switch (firstArg) {
                    case "update", "requirement", "reward" -> completions.add("<Name>");
                }
                break;
            }
            case 4:
                if (args[0].equalsIgnoreCase("update")) {
                    switch (args[1].toLowerCase()) {
                        case "displayname":
                            completions.add("<Displayname>");
                            break;
                        case "description":
                            completions.add("<Description>");
                            break;
                        case "duration":
                            completions.add("<Duration>");
                            break;
                        case "permission":
                            completions.add("<Permission>");
                            break;
                    }
                    break;
                }

                if(args[0].equalsIgnoreCase("reward")) {
                    completions.add("<RewardId>");
                    break;
                }

                if(args[0].equalsIgnoreCase("requirement") && args[1].equalsIgnoreCase("add")) {
                    completions.add("<Type>");
                    break;
                }
                break;
            case 5:
                if(args[0].equalsIgnoreCase("requirement") && args[1].equalsIgnoreCase("add")) {
                    completions.add("<RequiredAmount>");
                }
                break;
            case 6:
                if(args[0].equalsIgnoreCase("requirement") && args[1].equalsIgnoreCase("add")) {
                    completions.add("<Material/EntityType>");
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