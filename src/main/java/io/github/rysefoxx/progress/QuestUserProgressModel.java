package io.github.rysefoxx.progress;

import io.github.rysefoxx.quest.AbstractQuestRequirement;
import io.github.rysefoxx.quest.QuestModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnegative;
import java.time.LocalDateTime;
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

    @Column(nullable = false, length = 36)
    private UUID uuid;

    @Column(nullable = false)
    private int progress;

    @Column(nullable = false)
    private LocalDateTime expiration;

    @ManyToOne
    @JoinColumn(name = "quest_name")
    private QuestModel quest;

    @ManyToOne
    @JoinColumn(name = "requirement_id")
    private AbstractQuestRequirement requirement;

    public QuestUserProgressModel(@NotNull UUID uuid, @Nonnegative int progress) {
        this.uuid = uuid;
        this.progress = progress;
    }
}