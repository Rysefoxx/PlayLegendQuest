package io.github.rysefoxx.quest;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.reward.QuestRewardService;
import io.github.rysefoxx.scoreboard.ScoreboardService;
import io.github.rysefoxx.user.QuestUserService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;


@Getter
@Setter
@Entity
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "quest_requirement_type")
@Table(name = "quest_requirement")
public abstract class AbstractQuestRequirement implements Listener {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "required_amount", nullable = false)
    private int requiredAmount;

    @ManyToOne
    @JoinColumn(name = "quest_name", nullable = false)
    private QuestModel quest;

    @Column(name = "quest_requirement_type", nullable = false, insertable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private QuestRequirementType questRequirementType;

    private transient PlayLegendQuest plugin;
    private transient QuestService questService;
    private transient QuestUserProgressService questUserProgressService;
    private transient QuestRewardService questRewardService;
    private transient LanguageService languageService;
    private transient ScoreboardService scoreboardService;
    private transient QuestUserService questUserService;

    public AbstractQuestRequirement(@NotNull PlayLegendQuest plugin, @Nonnegative int requiredAmount, @NotNull QuestRequirementType questRequirementType) {
        this.plugin = plugin;
        this.requiredAmount = requiredAmount;
        this.questRequirementType = questRequirementType;
        register();
    }

    public abstract void sendInfo(@NotNull Player player, @NotNull LanguageService languageService);

    public abstract @NotNull String getProgressText(@NotNull QuestUserProgressModel questUserProgressModel);

    public void register() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        this.questUserProgressService = this.plugin.getQuestUserProgressService();
        this.languageService = this.plugin.getLanguageService();
        this.scoreboardService = this.plugin.getScoreboardService();
        this.questRewardService = this.plugin.getQuestRewardService();
        this.questUserService = this.plugin.getQuestUserService();
        this.questService = this.plugin.getQuestService();
    }

    protected void updateProgress(@NotNull Player player, @Nonnegative int progressIncrement) {
        getQuestUserProgressService().findByUuid(player.getUniqueId())
                .thenCompose(questUserProgressModels -> handleQuestUserProgressModels(player, questUserProgressModels, progressIncrement))
                .exceptionally(throwable -> handleError(player, throwable));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleQuestUserProgressModels(@NotNull Player player, @Nullable List<QuestUserProgressModel> questUserProgressModels, @Nonnegative int progressIncrement) {
        if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        QuestModel questModel = questUserProgressModels.getFirst().getQuest();
        if (!questModel.hasRequirement(getId())) {
            return CompletableFuture.completedFuture(null);
        }

        QuestUserProgressModel questUserProgressModel = questUserProgressModels.stream()
                .filter(model -> model.getRequirement().getId().equals(getId()))
                .findFirst()
                .orElse(null);

        if (questUserProgressModel == null || questUserProgressModel.isCompleted()) {
            return CompletableFuture.completedFuture(null);
        }

        questUserProgressModel.setProgress(Math.min(questUserProgressModel.getProgress() + progressIncrement, getRequiredAmount()));
        if (questUserProgressModel.isDone()) {
            questUserProgressModel.setCompleted(true);
        }

        getLanguageService().sendTranslatedMessage(player, "quest_progress", String.valueOf(questUserProgressModel.getProgress()), String.valueOf(getRequiredAmount()));

        return getQuestUserProgressService().save(questUserProgressModel)
                .thenCompose(resultType -> handleSaveResult(player, resultType, questUserProgressModel, questModel, questUserProgressModels));
    }

    private @NotNull CompletableFuture<@Nullable Void> handleSaveResult(@NotNull Player player, @NotNull ResultType resultType, @NotNull QuestUserProgressModel questUserProgressModel, @NotNull QuestModel questModel, @NotNull List<QuestUserProgressModel> questUserProgressModels) {
        if (resultType != ResultType.SUCCESS) {
            handleSaveError(player, "Error while saving quest progress.", resultType);
            return CompletableFuture.completedFuture(null);
        }

        if (questUserProgressModel.getProgress() < getRequiredAmount()) {
            return CompletableFuture.completedFuture(null);
        }

        getLanguageService().sendTranslatedMessage(player, "quest_requirement_done");
        getScoreboardService().update(player);

        if (!questModel.isCompleted(questUserProgressModels)) {
            return CompletableFuture.completedFuture(null);
        }

        getLanguageService().sendTranslatedMessage(player, "quest_done");
        getQuestRewardService().rewardPlayer(player, questModel);
        questModel.getUserQuests().removeIf(questUserModel -> questUserModel.getUuid().equals(player.getUniqueId()));

        return deleteQuestProgress(player, questModel);
    }

    private @NotNull CompletableFuture<@Nullable Void> deleteQuestProgress(@NotNull Player player, @NotNull QuestModel questModel) {
        return questUserService.deleteByUuid(player.getUniqueId())
                .thenCompose(resultType -> {
                    if (resultType != ResultType.SUCCESS) {
                        handleSaveError(player, "Error while deleting quest progress.", resultType);
                        return CompletableFuture.completedFuture(null);
                    }

                    return questService.getCache().synchronous().refresh(questModel.getName()).thenApply(refreshedQuestModel -> null);
                });
    }

    private @Nullable Void handleError(@NotNull Player player, @NotNull Throwable throwable) {
        player.sendRichMessage("Error while searching for quest progress.");
        PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest progress." + ": " + throwable.getMessage(), throwable);
        return null;
    }

    private void handleSaveError(@NotNull Player player, @NotNull String message, @NotNull ResultType resultType) {
        player.sendRichMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + " for player " + player.getName() + "! | " + resultType);
    }

}