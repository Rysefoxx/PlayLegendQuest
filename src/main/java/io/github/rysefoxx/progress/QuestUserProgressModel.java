package io.github.rysefoxx.progress;

import io.github.rysefoxx.quest.AbstractQuestRequirement;
import io.github.rysefoxx.quest.QuestModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public QuestUserProgressModel(UUID uuid, QuestModel quest, AbstractQuestRequirement requirement) {
        this.uuid = uuid;
        this.quest = quest;
        this.requirement = requirement;
    }

    public boolean isDone() {
        return this.progress >= this.requirement.getRequiredAmount();
    }
}