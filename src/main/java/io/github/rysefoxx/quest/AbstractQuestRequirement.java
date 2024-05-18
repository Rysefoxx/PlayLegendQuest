package io.github.rysefoxx.quest;

import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


@Getter
@Setter
@Entity
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "quest_requirement_type")
@Table(name = "quest_requirement")
public abstract class AbstractQuestRequirement {

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

    public AbstractQuestRequirement(int requiredAmount, QuestRequirementType questRequirementType) {
        this.requiredAmount = requiredAmount;
        this.questRequirementType = questRequirementType;
    }

    public abstract boolean isCompleted(Player player);

    public abstract void sendInfo(@NotNull Player player, @NotNull LanguageService languageService);

    public abstract @NotNull String getProgressText(@NotNull QuestUserProgressModel questUserProgressModel);
}