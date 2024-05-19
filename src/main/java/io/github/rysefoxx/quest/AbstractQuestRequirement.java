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

    /**
     * Creates a new quest requirement instance. The requirement will be registered as a listener.
     *
     * @param plugin               The plugin instance.
     * @param requiredAmount       The amount required to complete the requirement.
     * @param questRequirementType The type of the requirement.
     */
    public AbstractQuestRequirement(@NotNull PlayLegendQuest plugin, @Nonnegative int requiredAmount, @NotNull QuestRequirementType questRequirementType) {
        this.plugin = plugin;
        this.requiredAmount = requiredAmount;
        this.questRequirementType = questRequirementType;
        register();
    }

    /**
     * Sends information about the requirement to the player.
     *
     * @param player          The player to send the information to.
     * @param languageService The language service to use for translations.
     */
    public abstract void sendInfo(@NotNull Player player, @NotNull LanguageService languageService);

    /**
     * Gets the progress text for the player.
     *
     * @param questUserProgressModel The progress of the player.
     * @return The progress text.
     */
    public abstract @NotNull String getProgressText(@NotNull QuestUserProgressModel questUserProgressModel);

    /**
     * Registers the requirement as a listener and initializes the required services.
     */
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, this.plugin);
        this.questUserProgressService = this.plugin.getQuestUserProgressService();
        this.languageService = this.plugin.getLanguageService();
        this.scoreboardService = this.plugin.getScoreboardService();
        this.questRewardService = this.plugin.getQuestRewardService();
        this.questUserService = this.plugin.getQuestUserService();
        this.questService = this.plugin.getQuestService();
    }

    /**
     * Increases the progress of the player by the given amount. If the progress reaches the required amount, the requirement will be marked as completed and the player will be rewarded.
     *
     * @param player            The player to increase the progress for.
     * @param progressIncrement The amount to increase the progress by.
     */
    protected void updateProgress(@NotNull Player player, @Nonnegative int progressIncrement) {
        getQuestUserProgressService().findByUuid(player.getUniqueId())
                .thenCompose(questUserProgressModels -> handleQuestUserProgressModels(player, questUserProgressModels, progressIncrement))
                .exceptionally(throwable -> handleError(player, throwable));
    }

    /**
     * Handles the quest user progress models and increases the progress of the requirement.
     *
     * @param player                  The player to increase the progress for.
     * @param questUserProgressModels The progress of the player.
     * @param progressIncrement       The amount to increase the progress by.
     * @return A future that completes when the progress has been updated.
     */
    private @NotNull CompletableFuture<@Nullable Void> handleQuestUserProgressModels(@NotNull Player player, @Nullable List<QuestUserProgressModel> questUserProgressModels, @Nonnegative int progressIncrement) {
        if (questUserProgressModels == null || questUserProgressModels.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        QuestModel questModel = questUserProgressModels.get(0).getQuest();
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

    /**
     * Handles the result of saving the quest progress.
     *
     * @param player                  The player to handle the result for.
     * @param resultType              The result of saving the quest progress.
     * @param questUserProgressModel  The quest user progress model.
     * @param questModel              The quest model.
     * @param questUserProgressModels The quest user progress models.
     * @return A future that completes when the result has been handled.
     */
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

        return deleteUserModel(player, questModel);
    }

    /**
     * Deletes the user model for the given player.
     *
     * @param player     The player to delete the user model for.
     * @param questModel The quest model to delete the user model for.
     * @return A future that completes when the user model has been deleted.
     */
    private @NotNull CompletableFuture<@Nullable Void> deleteUserModel(@NotNull Player player, @NotNull QuestModel questModel) {
        return questUserService.deleteByUuid(player.getUniqueId())
                .thenCompose(resultType -> {
                    if (resultType != ResultType.SUCCESS) {
                        handleSaveError(player, "Error while deleting quest progress.", resultType);
                        return CompletableFuture.completedFuture(null);
                    }

                    return questService.getCache().synchronous().refresh(questModel.getName()).thenApply(refreshedQuestModel -> null);
                });
    }

    /**
     * Handles an error that occurred while updating the quest progress.
     *
     * @param player    The player to handle the error for.
     * @param throwable The error that occurred.
     * @return A future that completes when the error has been handled.
     */
    private @Nullable Void handleError(@NotNull Player player, @NotNull Throwable throwable) {
        player.sendRichMessage("Error while searching for quest progress.");
        PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest progress." + ": " + throwable.getMessage(), throwable);
        return null;
    }

    /**
     * Handles an error that occurred while saving the quest progress.
     *
     * @param player     The player to handle the error for.
     * @param message    The error message.
     * @param resultType The result type of the operation.
     */
    private void handleSaveError(@NotNull Player player, @NotNull String message, @NotNull ResultType resultType) {
        player.sendRichMessage(message);
        PlayLegendQuest.getLog().log(Level.SEVERE, message + " for player " + player.getName() + "! | " + resultType);
    }

}