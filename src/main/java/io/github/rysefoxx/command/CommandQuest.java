package io.github.rysefoxx.command;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.operation.*;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestRequirementService;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.reward.QuestRewardService;
import io.github.rysefoxx.scoreboard.ScoreboardService;
import io.github.rysefoxx.user.QuestUserService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class CommandQuest implements CommandExecutor {

    private final HashMap<String, QuestOperation> operations = new HashMap<>();

    public CommandQuest(@NotNull PlayLegendQuest plugin,
                        @NotNull QuestService questService,
                        @NotNull QuestRewardService questRewardService,
                        @NotNull QuestUserProgressService questUserProgressService,
                        @NotNull QuestRequirementService questRequirementService,
                        @NotNull QuestUserService questUserService,
                        @NotNull ScoreboardService scoreboardService,
                        @NotNull LanguageService languageService) {
        this.operations.put("accept", new QuestAcceptOperation(questService, languageService, questUserProgressService, questUserService, scoreboardService));
        this.operations.put("cancel", new QuestCancelOperation(questService, languageService, questUserProgressService, scoreboardService));
        this.operations.put("create", new QuestCreateOperation(questService, languageService));
        this.operations.put("delete", new QuestDeleteOperation(questService, languageService));
        this.operations.put("info", new QuestInfoOperation(questUserProgressService, languageService));
        this.operations.put("update_displayname", new QuestDisplayNameOperation(questService, languageService));
        this.operations.put("update_description", new QuestDescriptionOperation(questService, languageService, scoreboardService));
        this.operations.put("update_duration", new QuestDurationOperation(questService, languageService));
        this.operations.put("update_permission", new QuestPermissionOperation(questService, languageService));

        QuestRewardOperation questRewardOperation = new QuestRewardOperation(questService, questRewardService, languageService);
        this.operations.put("reward_add", questRewardOperation);
        this.operations.put("reward_remove", questRewardOperation);

        QuestRequirementOperation questRequirementOperation = new QuestRequirementOperation(plugin, questService, questRequirementService, languageService);
        this.operations.put("requirement_add", questRequirementOperation);
        this.operations.put("requirement_remove", questRequirementOperation);
        this.operations.put("requirement_info", questRequirementOperation);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) return false;

        try {
            QuestOperation questOperation = this.operations.get(args[0].toLowerCase());
            if (questOperation == null) {
                questOperation = this.operations.get(args[0].toLowerCase() + "_" + args[1].toLowerCase());
                if (questOperation == null) {
                    sendHelpMessage(player);
                    return false;
                }
            }
            return questOperation.onCommand(player, command, label, args);
        } catch (IndexOutOfBoundsException ignored) {
            sendHelpMessage(player);
            return false;
        }
    }

    private void sendHelpMessage(@NotNull Player player) {
        player.sendMessage("Quest create <Name>",
                "Quest delete <Name>",
                "Quest accept <Name>",
                "Quest cancel <Name>",
                "Quest update displayname <Name> <Displayname>",
                "Quest update description <Name> <Description>",
                "Quest update duration <Name> <Duration>",
                "Quest update permission <Name> <Permission>",
                "Quest reward add <Name> <RewardId>",
                "Quest reward remove <Name> <RewardId>",
                "Quest requirement add <Name> <Type> <RequiredAmount> <Material/EntityType>",
                "Quest requirement remove <Name> <Id>",
                "Quest requirement info <Id>",
                "Quest info");
    }
}