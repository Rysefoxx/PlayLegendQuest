package io.github.rysefoxx.reward;

import io.github.rysefoxx.enums.QuestRewardType;
import io.github.rysefoxx.quest.QuestModel;
import jakarta.persistence.*;
import lombok.Getter;
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
@Table(name = "quest_reward")
public class QuestRewardModel<T> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "quest_reward_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuestRewardType questRewardType;

    @Column(name = "reward", nullable = false)
    private String rewardString;

    @Transient
    private final T reward;

    @ManyToMany(mappedBy = "rewards")
    private List<QuestModel> quests = new ArrayList<>();

    public QuestRewardModel(@NotNull QuestRewardType questRewardType, @NotNull T reward, @NotNull String rewardAsString) {
        this.questRewardType = questRewardType;
        this.reward = reward;
        this.rewardString = rewardAsString;
    }

    public QuestRewardModel() {
        this.reward = null;
    }
}