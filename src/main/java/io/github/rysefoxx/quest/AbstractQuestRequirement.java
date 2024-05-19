package io.github.rysefoxx.quest;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.reward.QuestRewardService;
import io.github.rysefoxx.scoreboard.ScoreboardService;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
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
    private transient QuestUserProgressService questUserProgressService;
    private transient QuestRewardService questRewardService;
    private transient LanguageService languageService;
    private transient ScoreboardService scoreboardService;

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
    }

    protected void updateProgress(@NotNull Player player, @Nonnegative int progressIncrement) {
        getQuestUserProgressService().findByUuid(player.getUniqueId()).thenAccept(questUserProgressModels -> {
            if (questUserProgressModels == null || questUserProgressModels.isEmpty()) return;

            QuestModel questModel = questUserProgressModels.getFirst().getQuest();
            if (!questModel.hasRequirement(getId())) return;

            QuestUserProgressModel questUserProgressModel = questUserProgressModels.stream()
                    .filter(model -> model.getRequirement().getId().equals(getId()))
                    .findFirst()
                    .orElse(null);
            if (questUserProgressModel == null || questUserProgressModel.isCompleted()) return;

            questUserProgressModel.setProgress(Math.min(questUserProgressModel.getProgress() + progressIncrement, getRequiredAmount()));
            if (questUserProgressModel.getProgress() >= getRequiredAmount()) questUserProgressModel.setCompleted(true);

            getLanguageService().sendTranslatedMessage(player, "quest_progress", String.valueOf(questUserProgressModel.getProgress()), String.valueOf(getRequiredAmount()));

            getQuestUserProgressService().save(questUserProgressModel).thenAccept(resultType -> {
                if (resultType != ResultType.SUCCESS) {
                    player.sendRichMessage("Error while saving quest progress.");
                    PlayLegendQuest.getLog().log(Level.SEVERE, "Error while saving quest progress for player " + player.getName() + "! | " + resultType);
                    return;
                }

                if (questUserProgressModel.getProgress() < getRequiredAmount()) return;
                getLanguageService().sendTranslatedMessage(player, "quest_requirement_done");
                getScoreboardService().update(player);

                if (!questModel.isCompleted(questUserProgressModels)) return;
                getLanguageService().sendTranslatedMessage(player, "quest_done");

                getQuestRewardService().rewardPlayer(player, questModel);
            }).exceptionally(e -> {
                player.sendRichMessage("Error while saving quest progress.");
                PlayLegendQuest.getLog().log(Level.SEVERE, "Error while saving quest progress: " + e.getMessage(), e);
                return null;
            });
        }).exceptionally(e -> {
            player.sendRichMessage("Error while searching for quest progress.");
            PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest progress: " + e.getMessage(), e);
            return null;
        });
    }
}