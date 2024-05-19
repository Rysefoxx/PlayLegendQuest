package io.github.rysefoxx.user;

import io.github.rysefoxx.quest.QuestModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    public QuestUserModel(UUID uuid, QuestModel quest) {
        this.uuid = uuid;
        this.expiration = LocalDateTime.now().plusSeconds(quest.getDuration());
        this.quest = quest;
    }
}
