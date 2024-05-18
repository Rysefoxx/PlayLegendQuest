package io.github.rysefoxx.quest;

import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.reward.QuestRewardModel;
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
    @Column
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
    private List<QuestRewardModel<?>> rewards = new ArrayList<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QuestUserProgressModel> userProgress = new ArrayList<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AbstractQuestRequirement> requirements = new ArrayList<>();

    public QuestModel(String name) {
        this.name = name;
        this.displayName = name;
    }

    public boolean hasReward(@Nonnegative long rewardId) {
        return this.rewards.stream().anyMatch(questRewardModel -> questRewardModel.getId().equals(rewardId));
    }

    public boolean isConfigured() {
        return !this.requirements.isEmpty() && this.duration > 0;
    }

    public void sendProgressToUser(@NotNull Player player, @NotNull LanguageService languageService, @NotNull List<QuestUserProgressModel> questUserProgressModels) {
        int completedRequirements = getCompletedRequirementsCount(questUserProgressModels);
        String requirementTranslation = languageService.getTranslatedMessage(player, "quest_info_requirement");

        languageService.sendTranslatedMessage(player, "quest_info");
        languageService.sendTranslatedMessage(player, "quest_info_description",
                getDescription() != null ? getDescription() : languageService.getTranslatedMessage(player, "quest_info_no_description"));
        languageService.sendTranslatedMessage(player, "quest_info_displayname", getDisplayName());
        languageService.sendTranslatedMessage(player, "quest_info_duration", TimeUtils.toReadableString(questUserProgressModels.getFirst().getExpiration()));
        languageService.sendTranslatedMessage(player, "quest_info_requirements", String.valueOf(completedRequirements), String.valueOf(getRequirements().size()));

        for (String progressDetail : getProgressDetails(requirementTranslation, questUserProgressModels)) {
            languageService.sendTranslatedMessage(player, "quest_info_progress_details", progressDetail);
        }
    }

    public @Nonnegative int getCompletedRequirementsCount(@NotNull List<QuestUserProgressModel> questUserProgressModels) {
        int completedRequirements = 0;

        for (QuestUserProgressModel questUserProgressModel : questUserProgressModels) {
            AbstractQuestRequirement requirement = getRequirements().stream()
                    .filter(req -> req.getId().equals(questUserProgressModel.getRequirement().getId()))
                    .findFirst()
                    .orElse(null);

            if (requirement == null) continue;
            if (questUserProgressModel.getProgress() < requirement.getRequiredAmount()) continue;

            completedRequirements++;
        }

        return completedRequirements;
    }

    private @NotNull List<String> getProgressDetails(@NotNull String requirementTranslation, @NotNull List<QuestUserProgressModel> questUserProgressModels) {
        List<String> progressDetails = new ArrayList<>();

        for (QuestUserProgressModel questUserProgressModel : questUserProgressModels) {
            AbstractQuestRequirement requirement = getRequirements().stream()
                    .filter(req -> req.getId().equals(questUserProgressModel.getRequirement().getId()))
                    .findFirst()
                    .orElse(null);

            if (requirement == null) continue;

            progressDetails.add(requirementTranslation + " " + requirement.getProgressText(questUserProgressModel));
        }

        return progressDetails;
    }

}