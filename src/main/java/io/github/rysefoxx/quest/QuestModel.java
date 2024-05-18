package io.github.rysefoxx.quest;

import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.reward.QuestRewardModel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "quest_model")
public class QuestModel {

    @Id
    @Column(nullable = false, length = 40)
    private String name;

    @Column(nullable = false)
    private String displayName;

    @Nullable
    @Column
    private String description;

    @Column
    private long duration;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
            name = "quest_reward_mapping",
            joinColumns = @JoinColumn(name = "quest_name", referencedColumnName = "name"),
            inverseJoinColumns = @JoinColumn(name = "reward_id", referencedColumnName = "id")
    )
    private List<QuestRewardModel<?>> rewards = new ArrayList<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<QuestUserProgressModel> userProgress = new ArrayList<>();

    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AbstractQuestRequirement> requirements = new ArrayList<>();

    public QuestModel(String name) {
        this.name = name;
        this.displayName = name;
    }

    public boolean hasReward(@Nonnegative long rewardId) {
        return this.rewards.stream().anyMatch(questRewardModel -> questRewardModel.getId().equals(rewardId));
    }
}