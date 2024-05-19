package io.github.rysefoxx.quest;

import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.reward.QuestRewardModel;
import io.github.rysefoxx.user.QuestUserModel;
import io.github.rysefoxx.util.TimeUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quest_model")
public class QuestModel {

    @Id
    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false)
    private String displayName;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private long duration;

    @Column
    private String permission;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "quest_reward_relation",
            joinColumns = @JoinColumn(name = "quest_name", referencedColumnName = "name", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "reward_id", referencedColumnName = "id", nullable = false)
    )
    private List<QuestRewardModel> rewards = new ArrayList<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QuestUserModel> userQuests = new ArrayList<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QuestUserProgressModel> userProgress = new ArrayList<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AbstractQuestRequirement> requirements = new ArrayList<>();

    /**
     * Creates a new quest model with the given name.
     *
     * @param name The name of the quest.
     */
    public QuestModel(@NotNull String name) {
        this.name = name;
        this.displayName = name;
    }

    /**
     * Checks if the quest has the given reward.
     *
     * @param rewardId The identifier of the reward.
     * @return True if the quest has the reward, otherwise false.
     */
    public boolean hasReward(@Nonnegative long rewardId) {
        return this.rewards.stream().anyMatch(questRewardModel -> questRewardModel.getId().equals(rewardId));
    }

    /**
     * Checks if the quest is configured and ready to be used.
     *
     * @return True if the quest is configured, otherwise false.
     */
    public boolean isConfigured() {
        return !this.requirements.isEmpty() && this.duration > 0;
    }

    /**
     * Checks if the quest has the given requirement.
     *
     * @param requirementId The identifier of the requirement.
     * @return True if the quest has the requirement, otherwise false.
     */
    public boolean hasRequirement(@Nonnegative long requirementId) {
        return this.requirements.stream().anyMatch(abstractQuestRequirement -> abstractQuestRequirement.getId().equals(requirementId));
    }

    /**
     * Sends the quest information to the player and displays his progress.
     *
     * @param player                  The player to send the information to.
     * @param languageService         The language service to use for translations.
     * @param questUserProgressModels The progress of the player.
     */
    public void sendProgressToUser(@NotNull Player player, @NotNull LanguageService languageService, @NotNull List<QuestUserProgressModel> questUserProgressModels) {
        QuestUserModel questUserModel = getUserQuestModel(player);
        int completedRequirements = getCompletedRequirementsCount(questUserProgressModels);
        String requirementTranslation = languageService.getTranslatedMessage(player, "quest_info_requirement");

        languageService.sendTranslatedMessage(player, "quest_info");
        languageService.sendTranslatedMessage(player, "quest_info_description",
                getDescription() != null ? getDescription() : languageService.getTranslatedMessage(player, "quest_info_no_description"));
        languageService.sendTranslatedMessage(player, "quest_info_displayname", getDisplayName());
        languageService.sendTranslatedMessage(player, "quest_info_duration", questUserModel == null ? "Unknown" : TimeUtils.toReadableString(questUserModel.getExpiration()));
        languageService.sendTranslatedMessage(player, "quest_info_requirements", String.valueOf(completedRequirements), String.valueOf(getRequirements().size()));

        for (String progressDetail : getProgressDetails(requirementTranslation, questUserProgressModels)) {
            languageService.sendTranslatedMessage(player, "quest_info_progress_details", progressDetail);
        }
    }

    /**
     * Gets the number of completed requirements.
     *
     * @param questUserProgressModels The progress of the player.
     * @return The number of completed requirements.
     */
    public @Nonnegative int getCompletedRequirementsCount(@NotNull List<QuestUserProgressModel> questUserProgressModels) {
        return getRequirements().size() - questUserProgressModels.size();
    }

    /**
     * Gets the progress details for the player.
     *
     * @param requirementTranslation  The translation for the requirement.
     * @param questUserProgressModels The progress of the player.
     * @return The progress details.
     */
    private @NotNull List<String> getProgressDetails(@NotNull String requirementTranslation, @NotNull List<QuestUserProgressModel> questUserProgressModels) {
        List<String> progressDetails = new ArrayList<>();

        for (int i = 0; i < questUserProgressModels.size(); i++) {
            QuestUserProgressModel questUserProgressModel = questUserProgressModels.get(i);
            AbstractQuestRequirement requirement = getRequirements().stream()
                    .filter(req -> req.getId().equals(questUserProgressModel.getRequirement().getId()))
                    .findFirst()
                    .orElse(null);

            if (requirement == null) continue;
            progressDetails.add(requirementTranslation + " " + (i + 1) + ": " + requirement.getProgressText(questUserProgressModel));
        }

        return progressDetails;
    }

    /**
     * Checks if the quest is completed.
     *
     * @param questUserProgressModels The progress of the player.
     * @return True if the quest is completed, otherwise false.
     */
    public boolean isCompleted(@NotNull List<QuestUserProgressModel> questUserProgressModels) {
        return questUserProgressModels.isEmpty() || questUserProgressModels.stream().allMatch(QuestUserProgressModel::isCompleted);
    }

    /**
     * Checks if the quest has a permission. If the quest has a permission, the player must have the permission to start the quest.
     *
     * @return True if the quest has a permission, otherwise false.
     */
    public boolean hasPermission() {
        return this.permission != null;
    }

    /**
     * Gets the user quest model for the given player.
     *
     * @param player The player to get the user quest model for.
     * @return The user quest model or null if the player has no user quest model.
     */
    public @Nullable QuestUserModel getUserQuestModel(@NotNull Player player) {
        return this.userQuests.stream()
                .filter(questUserModel -> questUserModel.getUuid().equals(player.getUniqueId()))
                .findFirst()
                .orElse(null);
    }
}