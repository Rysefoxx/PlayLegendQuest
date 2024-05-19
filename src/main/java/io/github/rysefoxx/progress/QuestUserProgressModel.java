package io.github.rysefoxx.progress;

import io.github.rysefoxx.quest.AbstractQuestRequirement;
import io.github.rysefoxx.quest.QuestModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * @author Rysefoxx
 * @since 17.05.2024
 */

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "quest_user_progress")
public class QuestUserProgressModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36, columnDefinition = "VARCHAR(36)")
    private UUID uuid;

    @Column(nullable = false)
    private int progress;

    @ManyToOne
    @JoinColumn(name = "quest_name")
    private QuestModel quest;

    @ManyToOne
    @JoinColumn(name = "requirement_id")
    private AbstractQuestRequirement requirement;

    @Column(nullable = false, columnDefinition = "BOOLEAN")
    private boolean completed;

    /**
     * Creates a new instance of the model.
     *
     * @param uuid        The UUID of the player.
     * @param quest       The quest.
     * @param requirement The requirement.
     */
    public QuestUserProgressModel(@NotNull UUID uuid, @NotNull QuestModel quest, @NotNull AbstractQuestRequirement requirement) {
        this.uuid = uuid;
        this.quest = quest;
        this.requirement = requirement;
    }

    /**
     * @return true if the requirement is completed.
     */
    public boolean isDone() {
        return this.progress >= this.requirement.getRequiredAmount();
    }
}