package io.github.rysefoxx.command;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.AbstractQuestRequirement;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestRequirementService;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.reward.QuestRewardService;
import io.github.rysefoxx.scoreboard.ScoreboardService;
import io.github.rysefoxx.util.Maths;
import io.github.rysefoxx.util.StringUtils;
import io.github.rysefoxx.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final QuestRequirementService questRequirementService;
    private final ScoreboardService scoreboardService;
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

        if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            accept(player, args);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
            cancel(player, args);
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

        if (args.length >= 4 && args[0].equalsIgnoreCase("update") && args[1].equalsIgnoreCase("description")) {
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
         * Quest accept <Name>
         * Quest cancel <Name>
         *
         *
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

    private void accept(@NotNull Player player, String @NotNull [] args) {
        String name = args[1];
        this.questService.findByName(name).thenAccept(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return;
            }

            if(!questModel.isConfigured()) {
                this.languageService.sendTranslatedMessage(player, "quest_not_configured");
                return;
            }

            this.questUserProgressService.hasQuest(player.getUniqueId()).thenAccept(hasQuest -> {
                if (hasQuest) {
                    this.languageService.sendTranslatedMessage(player, "quest_already_active");
                    return;
                }

                this.questService.save(questModel).thenCompose(resultType -> {
                    if (resultType != ResultType.SUCCESS) {
                        this.languageService.sendTranslatedMessage(player, "quest_save_failed");
                        return CompletableFuture.completedFuture(ResultType.ERROR);
                    }

                    List<CompletableFuture<ResultType>> futures = new ArrayList<>();
                    for (AbstractQuestRequirement requirement : questModel.getRequirements()) {
                        QuestUserProgressModel questUserProgressModel = new QuestUserProgressModel(player.getUniqueId(), LocalDateTime.now().plusSeconds(questModel.getDuration()), questModel, requirement);
                        futures.add(this.questUserProgressService.save(questUserProgressModel));
                    }

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> ResultType.SUCCESS);
                }).thenAccept(resultType -> {
                    this.scoreboardService.update(player);
                    this.languageService.sendTranslatedMessage(player, "quest_accepted_" + resultType.toString().toLowerCase());
                }).exceptionally(e -> {
                    player.sendRichMessage("Error while accepting quest");
                    PlayLegendQuest.getLog().log(Level.SEVERE, "Error while accepting quest: " + e.getMessage(), e);
                    return null;
                });

            }).exceptionally(e -> {
                player.sendRichMessage("Error while searching for quest user progress");
                PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest user progress: " + e.getMessage(), e);
                return null;
            });
        }).exceptionally(e -> {
            player.sendRichMessage("Error while searching for quest");
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest: " + e.getMessage(), e);
            return null;
        });
    }

    private void cancel(@NotNull Player player, String @NotNull [] args) {
        String name = args[1];
        this.questService.findByName(name).thenAccept(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return;
            }

            this.questUserProgressService.findByUuid(player.getUniqueId()).thenAccept(questUserProgressModels -> {
                if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
                    this.languageService.sendTranslatedMessage(player, "quest_no_active");
                    return;
                }

                QuestUserProgressModel questUserProgressModel = questUserProgressModels.getFirst();
                if (!questUserProgressModel.getQuest().getName().equals(name)) {
                    this.languageService.sendTranslatedMessage(player, "quest_not_active");
                    return;
                }

                questUserProgressModel.getQuest().getUserProgress().remove(questUserProgressModel);

                this.questUserProgressService.delete(player.getUniqueId()).thenAccept(resultType -> {
                    this.scoreboardService.update(player);
                    this.languageService.sendTranslatedMessage(player, "quest_canceled_" + resultType.toString().toLowerCase());
                }).exceptionally(e -> {
                    player.sendRichMessage("Error while canceling quest");
                    PlayLegendQuest.getLog().log(Level.SEVERE, "Error while canceling quest: " + e.getMessage(), e);
                    return null;
                });
            }).exceptionally(e -> {
                player.sendRichMessage("Error while searching for quest user progress");
                PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest user progress: " + e.getMessage(), e);
                return null;
            });
        }).exceptionally(e -> {
            player.sendRichMessage("Error while searching for quest");
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest: " + e.getMessage(), e);
            return null;
        });
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

            return this.questService.removeRequirement(questModel, requirement).thenAccept(resultType -> {
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

            return this.questRequirementService.save(requirement).thenCompose(requirementId -> {
                if (requirementId == null) {
                    this.languageService.sendTranslatedMessage(player, "quest_requirement_creation_failed");
                    return CompletableFuture.completedFuture(null);
                }

                questModel.getRequirements().add(requirement);

                return this.questService.save(questModel).thenAccept(resultType -> {
                    this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
                });
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
            questModel.sendProgressToUser(player, this.languageService, questUserProgressModels);
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
        String description = StringUtils.join(args, " ", 3);

        this.questService.findByName(name).thenAccept(questModel -> {
            if (questModel == null) {
                this.languageService.sendTranslatedMessage(player, "quest_not_exist");
                return;
            }

            questModel.setDescription(description);
            this.questService.save(questModel).thenAccept(resultType -> {
                this.languageService.sendTranslatedMessage(player, "quest_updated_" + resultType.toString().toLowerCase());
                this.scoreboardService.update(player);
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

                questModel.getRewards().removeIf(reward -> reward.getId().equals(rewardId));
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
        player.sendRichMessage("Quest accept <Name>");
        player.sendRichMessage("Quest cancel <Name>");
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