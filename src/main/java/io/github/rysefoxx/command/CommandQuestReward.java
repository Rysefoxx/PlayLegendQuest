package io.github.rysefoxx.command;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRewardType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.reward.QuestRewardModel;
import io.github.rysefoxx.reward.QuestRewardService;
import io.github.rysefoxx.util.LogUtils;
import io.github.rysefoxx.util.Maths;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@RequiredArgsConstructor
public class CommandQuestReward implements CommandExecutor {

    private final LanguageService languageService;
    private final QuestRewardService questRewardService;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) return false;

        if(!player.hasPermission("playlegend.questreward.admin")) {
            this.languageService.sendTranslatedMessage(player, "no_permission");
            return false;
        }

        if (args.length < 2 || args.length > 4) {
            sendHelpMessage(player);
            return false;
        }

        if (args[0].equalsIgnoreCase("create")) {
            create(args, player);
            return false;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            delete(args, player);
            return false;
        }

        if (args[0].equalsIgnoreCase("update")) {
            update(args, player);
            return false;
        }

        return false;
    }

    /**
     * Updates a quest reward. We can update the type and the reward.
     *
     * @param args   The arguments provided by the player.
     * @param player The player who executed the command.
     */
    private void update(@NotNull String[] args, @NotNull Player player) {
        if (!Maths.isDataType(args[1], Long.class)) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        QuestRewardType type = QuestRewardType.getQuestRewardType(args[2]);
        if (type == null) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_reward_type");
            return;
        }

        if (!isValidUpdateInput(type, args)) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        long id = Long.parseLong(args[1]);
        String rewardString = this.questRewardService.buildRewardAsString(type, player, args);

        if (rewardString == null) {
            this.languageService.sendTranslatedMessage(player, "quest_reward_update_failed");
            return;
        }

        this.questRewardService.update(id, rewardString, type).thenAccept(resultType -> {
            this.languageService.sendTranslatedMessage(player, "quest_reward_update_" + resultType.toString().toLowerCase());
        }).exceptionally(throwable -> LogUtils.handleError(player, "An error occurred while updating the quest reward.", throwable));
    }

    /**
     * Deletes a quest reward by the id.
     *
     * @param args   The arguments provided by the player.
     * @param player The player who executed the command.
     */
    private void delete(@NotNull String @NotNull [] args, @NotNull Player player) {
        if (!Maths.isDataType(args[1], Long.class)) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        long id = Long.parseLong(args[1]);
        this.questRewardService.delete(id).thenAccept(resultType -> {
            this.languageService.sendTranslatedMessage(player, "quest_reward_delete_" + resultType.toString().toLowerCase());
        }).exceptionally(throwable -> LogUtils.handleError(player, "An error occurred while deleting the quest reward.", throwable));
    }

    /**
     * Creates a quest reward. We can create a quest reward by providing the type and the reward.
     *
     * @param args   The arguments provided by the player.
     * @param player The player who executed the command.
     */
    private void create(@NotNull String @NotNull [] args, @NotNull Player player) {
        QuestRewardType type = QuestRewardType.getQuestRewardType(args[1]);
        if (type == null) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_reward_type");
            return;
        }

        if (!isValidCreationInput(type, args)) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        QuestRewardModel questRewardModel = this.questRewardService.buildQuestRewardModel(type, player, args);

        if (questRewardModel == null) {
            this.languageService.sendTranslatedMessage(player, "quest_reward_build_failed");
            return;
        }

        this.questRewardService.save(questRewardModel).thenAccept(resultType -> {
            this.languageService.sendTranslatedMessage(player, "quest_reward_save_" + resultType.toString().toLowerCase());
        }).exceptionally(throwable -> LogUtils.handleError(player, "An error occurred while saving the quest reward.", throwable));
    }

    /**
     * Checks if the input is valid for the creation of a quest reward.
     *
     * @param type The type of the quest reward.
     * @param args The arguments provided by the player.
     * @return true if the input is valid, false if not.
     */
    private boolean isValidCreationInput(@NotNull QuestRewardType type, @NotNull String[] args) {
        if (type == QuestRewardType.COINS || type == QuestRewardType.EXPERIENCE) {
            return args.length == 3 && Maths.isDataType(args[2], type.getClassType());
        }

        return true;
    }

    /**
     * Checks if the input is valid for the update of a quest reward.
     *
     * @param type The type of the quest reward.
     * @param args The arguments provided by the player.
     * @return true if the input is valid, false if not.
     */
    private boolean isValidUpdateInput(@NotNull QuestRewardType type, @NotNull String[] args) {
        if (type == QuestRewardType.COINS || type == QuestRewardType.EXPERIENCE) {
            return args.length == 4 && Maths.isDataType(args[3], type.getClassType());
        }

        return true;
    }

    /**
     * Sends the help message to the player.
     *
     * @param player The player who executed the command.
     */
    private void sendHelpMessage(@NotNull Player player) {
        player.sendRichMessage("/Questreward create <Type> (<Reward>)");
        player.sendRichMessage("/Questreward delete <ID>");
        player.sendRichMessage("/Questreward update <ID> <Type> <Reward>");
    }
}