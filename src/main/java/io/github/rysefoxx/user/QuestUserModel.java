package io.github.rysefoxx.user;

import io.github.rysefoxx.quest.QuestModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Rysefoxx
 * @since 19.05.2024
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "quest_user")
public class QuestUserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36, columnDefinition = "VARCHAR(36)")
    private UUID uuid;

    @Column(nullable = false)
    private LocalDateTime expiration;

    @ManyToOne
    @JoinColumn(name = "quest_name")
    private QuestModel quest;

    /**
     * Create a new QuestUserModel. The expiration date is calculated by the current time and the duration of the quest.
     *
     * @param uuid  The UUID of the player
     * @param quest The quest the player is currently doing
     */
    public QuestUserModel(@NotNull UUID uuid, @NotNull QuestModel quest) {
        this.uuid = uuid;
        this.expiration = LocalDateTime.now().plusSeconds(quest.getDuration());
        this.quest = quest;
    }
}
