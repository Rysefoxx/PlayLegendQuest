package io.github.rysefoxx.reward;

import io.github.rysefoxx.enums.QuestRewardType;
import io.github.rysefoxx.quest.QuestModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "quest_reward")
public class QuestRewardModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "quest_reward_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuestRewardType questRewardType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reward;

    @ManyToMany(mappedBy = "rewards", fetch = FetchType.EAGER)
    private List<QuestModel> quests = new ArrayList<>();

    /**
     * Creates a new QuestRewardModel
     *
     * @param questRewardType The type of the reward
     * @param reward          The reward
     */
    public QuestRewardModel(@NotNull QuestRewardType questRewardType, @NotNull String reward) {
        this.questRewardType = questRewardType;
        this.reward = reward;
    }
}