package io.github.rysefoxx.command;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.AbstractQuestRequirement;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.reward.QuestRewardService;
import io.github.rysefoxx.util.Maths;
import io.github.rysefoxx.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@RequiredArgsConstructor
public class CommandQuest implements CommandExecutor {

    private final QuestService questService;
    private final QuestRewardService questRewardService;
    private final QuestUserProgressService questUserProgressService;
    private final LanguageService languageService;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(commandSender instanceof Player player)) return false;

        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            questInfo(player);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            create(player, args);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            delete(player, args);
            return true;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("requirement") && args[1].equalsIgnoreCase("info")) {
            requirementInfo(player, args);
            return true;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("update") && args[1].equalsIgnoreCase("displayname")) {
            updateDisplayName(player, args);
            return true;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("update") && args[1].equalsIgnoreCase("description")) {
            updateDescription(player, args);
            return true;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("update") && args[1].equalsIgnoreCase("duration")) {
            updateDuration(player, args);
            return true;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("update") && args[1].equalsIgnoreCase("permission")) {
            updatePermission(player, args);
            return true;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("reward") && args[1].equalsIgnoreCase("add")) {
            addReward(player, args);
            return true;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("reward") && args[1].equalsIgnoreCase("remove")) {
            removeReward(player, args);
            return true;
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("requirement") && args[1].equalsIgnoreCase("remove")) {
            removeRequirement(player, args);
            return true;
        }

        if (args.length == 6 && args[0].equalsIgnoreCase("requirement") && args[1].equalsIgnoreCase("add")) {
            addRequirement(player, args);
            return true;
        }

        sendHelpMessage(player);
        return true;

        /**
         * Quest create <Name>
         * Quest delete <Name>
         * Quest update displayname <Name> <Displayname>
         * Quest update description <Name> <Description>
         * Quest update duration <Name> <Duration>
         * Quest update permission <Name> <Permission>
         * Quest info
         * Quest reward add <Name> <RewardId>
         * Quest reward remove <Name> <RewardId>
         * Quest requirement add <Name> <Type> <RequiredAmount> <Material/EntityType>
         * Quest requirement remove <Name> <Id>
         * quest requirement info <Id>
         */
    }

    private void requirementInfo(@NotNull Player player, String @NotNull [] args) {
        if (!Maths.isDataType(args[2], Long.class)) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        long requirementId = Long.parseLong(args[2]);
        this.questService.findRequirementById(requirementId).thenAccept(abstractQuestRequirement -> {
            if (abstractQuestRequirement == null) {
                this.languageService.sendTranslatedMessage(player, "quest_requirement_not_exist");
                return;
            }

            abstractQuestRequirement.sendInfo(player, this.languageService);
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest requirement: " + e.getMessage(), e);
            return null;
        });

    }

    private void removeRequirement(@NotNull Player player, String @NotNull [] args) {
        if (!Maths.isDataType(args[3], Long.class)) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        String name = args[2];
        long requirementId = Long.parseLong(args[3]);

        this.questService.findByName(name).thenCompose(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return CompletableFuture.completedFuture(null);
            }

            AbstractQuestRequirement requirement = questModel.getRequirements().stream()
                    .filter(req -> req.getId().equals(requirementId))
                    .findFirst()
                    .orElse(null);

            if (requirement == null) {
                this.languageService.sendTranslatedMessage(player, "quest_requirement_not_exist");
                return CompletableFuture.completedFuture(null);
            }

            questModel.getRequirements().remove(requirement);
            return this.questService.save(questModel).thenAccept(resultType -> {
                this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
            }).exceptionally(e -> {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Error removing requirement from quest: " + e.getMessage(), e);
                return null;
            });
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error removing requirement from quest: " + e.getMessage(), e);
            return null;
        });
    }

    private void addRequirement(@NotNull Player player, String @NotNull [] args) {
        if (!Maths.isDataType(args[4], Integer.class)) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        QuestRequirementType requirementType = QuestRequirementType.getQuestRequirementType(args[3]);
        if (requirementType == null) {
            this.languageService.sendTranslatedMessage(player, "quest_invalid_requirement_type");
            return;
        }

        String name = args[2];
        this.questService.findByName(name).thenCompose(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return CompletableFuture.completedFuture(null);
            }
            int requiredAmount = Integer.parseInt(args[4]);
            AbstractQuestRequirement requirement = this.questService.createRequirement(requirementType, requiredAmount, args);

            if (requirement == null) {
                this.languageService.sendTranslatedMessage(player, "quest_requirement_creation_failed");
                return CompletableFuture.completedFuture(null);
            }

            requirement.setQuest(questModel);
            questModel.getRequirements().add(requirement);

            return this.questService.save(questModel).thenAccept(resultType -> {
                this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
            });
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error adding requirement to quest: " + e.getMessage(), e);
            return null;
        });
    }

    private void questInfo(@NotNull Player player) {
        this.questUserProgressService.findByUuid(player.getUniqueId()).thenAccept(questUserProgressModels -> {
            if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
                this.languageService.sendTranslatedMessage(player, "quest_no_active");
                return;
            }

            QuestModel questModel = questUserProgressModels.getFirst().getQuest();
            int completedRequirements = 0;
            StringBuilder progressDetails = new StringBuilder();

            String requirementTranslation = this.languageService.getTranslatedMessage(player, "quest_info_requirement");

            for (QuestUserProgressModel questUserProgressModel : questUserProgressModels) {
                AbstractQuestRequirement requirement = questModel.getRequirements().stream()
                        .filter(req -> req.getId().equals(questUserProgressModel.getRequirement().getId()))
                        .findFirst()
                        .orElse(null);

                if (requirement != null) {
                    progressDetails.append(requirementTranslation).append(" ")
                            .append(requirement.getId())
                            .append(": ")
                            .append(questUserProgressModel.getProgress())
                            .append("/")
                            .append(requirement.getRequiredAmount())
                            .append("\n");
                    if (questUserProgressModel.getProgress() >= requirement.getRequiredAmount()) {
                        completedRequirements++;
                    }
                }
            }

            this.languageService.sendTranslatedMessage(player, "quest_info");
            this.languageService.sendTranslatedMessage(player, "quest_info_description",
                    questModel.getDescription() != null ? questModel.getDescription() : this.languageService.getTranslatedMessage(player, "quest_info_no_description"));
            this.languageService.sendTranslatedMessage(player, "quest_info_displayname", questModel.getDisplayName());
            this.languageService.sendTranslatedMessage(player, "quest_info_duration", TimeUtils.toReadableString(questUserProgressModels.getFirst().getExpiration()));
            this.languageService.sendTranslatedMessage(player, "quest_info_requirements", String.valueOf(completedRequirements), String.valueOf(questModel.getRequirements().size()));
            this.languageService.sendTranslatedMessage(player, "quest_info_progress_details", progressDetails.toString());

        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest user progress: " + e.getMessage(), e);
            return null;
        });
    }

    private void create(@NotNull Player player, String @NotNull [] args) {
        String name = args[1];
        if (name.length() > 40) {
            this.languageService.sendTranslatedMessage(player, "quest_name_too_long");
            return;
        }

        this.questService.findByName(name).thenAccept(questModel -> {
            if (questModel != null) {
                this.languageService.sendTranslatedMessage(player, "quest_exist");
                return;
            }

            questModel = new QuestModel(name);
            this.questService.save(questModel).thenAccept(resultType -> {
                if (resultType == ResultType.SUCCESS) {
                    this.languageService.sendTranslatedMessage(player, "quest_created");
                    return;
                }

                this.languageService.sendTranslatedMessage(player, "quest_create_error");
            }).exceptionally(e -> {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Error saving quest: " + e.getMessage(), e);
                return null;
            });
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest " + e.getMessage(), e);
            return null;
        });
    }

    private void delete(@NotNull Player player, String @NotNull [] args) {
        String name = args[1];
        this.questService.delete(name).thenAccept(resultType -> {
            this.languageService.sendTranslatedMessage(player, "quest_deleted_" + resultType.toString().toLowerCase());
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error deleting quest: " + e.getMessage(), e);
            return null;
        });
    }

    private void updatePermission(@NotNull Player player, String @NotNull [] args) {
        String name = args[2];
        String permission = args[3];

        this.questService.findByName(name).thenAccept(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return;
            }

            questModel.setPermission(permission);
            this.questService.save(questModel).thenAccept(resultType -> {
                this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
            });
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error updating permission for quest: " + e.getMessage(), e);
            return null;
        });
    }

    private void updateDisplayName(@NotNull Player player, String @NotNull [] args) {
        String name = args[2];
        String displayName = args[3];

        this.questService.findByName(name).thenAccept(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return;
            }

            questModel.setDisplayName(displayName);
            this.questService.save(questModel).thenAccept(resultType -> {
                this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
            });
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error updating displayname for quest: " + e.getMessage(), e);
            return null;
        });
    }

    private void updateDescription(@NotNull Player player, String @NotNull [] args) {
        String name = args[2];
        String description = args[3];

        this.questService.findByName(name).thenAccept(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return;
            }

            questModel.setDescription(description);
            this.questService.save(questModel).thenAccept(resultType -> {
                this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
            });
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error updating description for quest: " + e.getMessage(), e);
            return null;
        });
    }

    private void updateDuration(@NotNull Player player, String @NotNull [] args) {
        String durationString = args[3];
        long seconds = TimeUtils.parseDurationToSeconds(durationString);

        if (seconds == 0) {
            this.languageService.sendTranslatedMessage(player, "quest_duration_invalid");
            return;
        }

        String name = args[2];
        this.questService.findByName(name).thenAccept(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return;
            }

            questModel.setDuration(seconds);
            this.questService.save(questModel).thenAccept(resultType -> {
                this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
            });
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error updating duration for quest: " + e.getMessage(), e);
            return null;
        });

    }

    private void addReward(@NotNull Player player, String @NotNull [] args) {
        if (!Maths.isDataType(args[3], Long.class)) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        String name = args[2];
        long rewardId = Long.parseLong(args[3]);

        this.questService.findByName(name).thenCompose(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return CompletableFuture.completedFuture(null);
            }

            return this.questRewardService.findById(rewardId).thenCompose(questRewardModel -> {
                if (questRewardModel == null) {
                    this.languageService.sendTranslatedMessage(player, "quest_reward_not_exist");
                    return CompletableFuture.completedFuture(null);
                }

                if (questModel.hasReward(rewardId)) {
                    this.languageService.sendTranslatedMessage(player, "quest_reward_already_added");
                    return CompletableFuture.completedFuture(null);
                }

                questModel.getRewards().add(questRewardModel);
                return this.questService.save(questModel).thenAccept(resultType -> {
                    this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
                });
            });
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error adding reward to quest: " + e.getMessage(), e);
            return null;
        });
    }

    private void removeReward(@NotNull Player player, String @NotNull [] args) {
        if (!Maths.isDataType(args[3], Long.class)) {
            this.languageService.sendTranslatedMessage(player, "invalid_quest_input");
            return;
        }

        String name = args[2];
        long rewardId = Long.parseLong(args[3]);

        this.questService.findByName(name).thenCompose(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return CompletableFuture.completedFuture(null);
            }

            return this.questRewardService.findById(rewardId).thenCompose(questRewardModel -> {
                if (questRewardModel == null) {
                    this.languageService.sendTranslatedMessage(player, "quest_reward_not_exist");
                    return CompletableFuture.completedFuture(null);
                }

                if (!questModel.hasReward(rewardId)) {
                    this.languageService.sendTranslatedMessage(player, "quest_reward_not_added");
                    return CompletableFuture.completedFuture(null);
                }

                questModel.getRewards().remove(questRewardModel);
                return this.questService.save(questModel).thenAccept(resultType -> {
                    this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
                });
            });
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error removing reward to quest: " + e.getMessage(), e);
            return null;
        });
    }

    private void sendHelpMessage(@NotNull Player player) {
        player.sendRichMessage("Quest create <Name>");
        player.sendRichMessage("Quest delete <Name>");
        player.sendRichMessage("Quest update displayname <Name> <Displayname>");
        player.sendRichMessage("Quest update description <Name> <Description>");
        player.sendRichMessage("Quest update duration <Name> <Duration>");
        player.sendRichMessage("Quest update permission <Name> <Permission>");
        player.sendRichMessage("Quest reward add <Name> <RewardId>");
        player.sendRichMessage("Quest reward remove <Name> <RewardId>");
        player.sendRichMessage("Quest reward remove <Name> <RewardId>");
        player.sendRichMessage("Quest requirement add <Name> <Type> <RequiredAmount> <Material/EntityType>");
        player.sendRichMessage("Quest requirement remove <Name> <Id>");
        player.sendRichMessage("Quest requirement info <Id>");
        player.sendRichMessage("Quest info");
    }
}