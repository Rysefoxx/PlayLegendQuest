package io.github.rysefoxx.command.operation;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.command.QuestOperation;
import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.quest.AbstractQuestRequirement;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestRequirementService;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.util.Maths;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 19.05.2024
 */
@RequiredArgsConstructor
public class QuestRequirementOperation implements QuestOperation {

    private final PlayLegendQuest plugin;
    private final QuestService questService;
    private final QuestRequirementService questRequirementService;
    private final LanguageService languageService;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return false;

        if (args.length >= 5 && args[1].equalsIgnoreCase("add")) {
            addRequirement(player, args);
            return true;
        }

        if (args.length == 4 && args[1].equalsIgnoreCase("remove")) {
            removeRequirement(player, args);
            return true;
        }

        if (args.length == 3 && args[1].equalsIgnoreCase("info")) {
            requirementInfo(player, args);
            return true;
        }

        return false;
    }

    private void requirementInfo(@NotNull Player player, String @NotNull [] args) {
        if (!Maths.isDataType(args[2], Long.class)) {
            languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        long requirementId = Long.parseLong(args[2]);
        questService.findRequirementById(requirementId)
                .thenAccept(abstractQuestRequirement -> handleRequirementInfo(player, abstractQuestRequirement))
                .exceptionally(e -> handleError(player, "Error while searching for quest requirement", e));
    }

    private void handleRequirementInfo(@NotNull Player player, @Nullable AbstractQuestRequirement abstractQuestRequirement) {
        if (abstractQuestRequirement == null) {
            languageService.sendTranslatedMessage(player, "quest_requirement_not_exist");
            return;
        }
        abstractQuestRequirement.sendInfo(player, languageService);
    }

    private void removeRequirement(@NotNull Player player, String @NotNull [] args) {
        if (!Maths.isDataType(args[3], Long.class)) {
            languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        String name = args[2];
        long requirementId = Long.parseLong(args[3]);

        questService.findByName(name)
                .thenCompose(questModel -> handleRemoveRequirement(player, questModel, requirementId))
                .exceptionally(throwable -> handleError(player, "Error while searching for quest", throwable));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleRemoveRequirement(@NotNull Player player, @Nullable QuestModel questModel, @Nonnegative long requirementId) {
        if (questModel == null) {
            languageService.sendTranslatedMessage(player, "quest_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        AbstractQuestRequirement requirement = questModel.getRequirements().stream()
                .filter(req -> req.getId().equals(requirementId))
                .findFirst()
                .orElse(null);

        if (requirement == null) {
            languageService.sendTranslatedMessage(player, "quest_requirement_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        return questService.removeRequirement(questModel, requirement)
                .thenAccept(resultType -> handleSaveResult(player, resultType, "Error while removing requirement from quest"))
                .exceptionally(e -> handleError(player, "Error removing requirement from quest", e));
    }

    private void addRequirement(@NotNull Player player, String @NotNull [] args) {
        if (!Maths.isDataType(args[4], Integer.class)) {
            languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        QuestRequirementType requirementType = QuestRequirementType.getQuestRequirementType(args[3]);
        if (requirementType == null) {
            languageService.sendTranslatedMessage(player, "quest_invalid_requirement_type");
            return;
        }

        String name = args[2];
        questService.findByName(name)
                .thenCompose(questModel -> handleAddRequirement(player, questModel, requirementType, args))
                .exceptionally(e -> handleError(player, "Error while finding quest", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleAddRequirement(@NotNull Player player, @Nullable QuestModel questModel, @NotNull QuestRequirementType requirementType, String @NotNull [] args) {
        if (questModel == null) {
            languageService.sendTranslatedMessage(player, "quest_not_exist");
            return CompletableFuture.completedFuture(null);
        }

        int requiredAmount = Integer.parseInt(args[4]);
        AbstractQuestRequirement requirement = questService.createRequirement(plugin, requirementType, requiredAmount, args);

        if (requirement == null) {
            languageService.sendTranslatedMessage(player, "quest_requirement_creation_failed");
            return CompletableFuture.completedFuture(null);
        }

        requirement.setQuest(questModel);

        return questRequirementService.save(requirement)
                .thenCompose(requirementId -> handleSaveRequirement(player, requirementId, questModel, requirement))
                .exceptionally(e -> handleError(player, "Error while saving requirement", e));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleSaveRequirement(@NotNull Player player, @Nullable Long requirementId, @NotNull QuestModel questModel, @NotNull AbstractQuestRequirement requirement) {
        if (requirementId == null) {
            languageService.sendTranslatedMessage(player, "quest_requirement_creation_failed");
            return CompletableFuture.completedFuture(null);
        }

        questModel.getRequirements().add(requirement);
        return questService.save(questModel)
                .thenAccept(resultType -> handleSaveResult(player, resultType, "Error while saving requirement to quest"))
                .exceptionally(e -> handleError(player, "Error while saving requirement to quest", e));
    }

    private void handleSaveResult(@NotNull Player player, @NotNull ResultType resultType, @NotNull String errorMessage) {
        if (resultType == ResultType.SUCCESS) {
            languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
        } else {
            player.sendRichMessage(errorMessage);
        }
    }

    private @Nullable Void handleError(@NotNull Player player, @NotNull String message, @NotNull Throwable throwable) {
        player.sendRichMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
