package io.github.rysefoxx.reward;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionManager;
import io.github.rysefoxx.database.IDatabaseOperation;
import io.github.rysefoxx.enums.QuestRewardType;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.reward.impl.CoinQuestReward;
import io.github.rysefoxx.reward.impl.ExperienceQuestReward;
import io.github.rysefoxx.reward.impl.ItemQuestReward;
import org.bukkit.entity.Player;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class QuestRewardService implements IDatabaseOperation<QuestRewardModel<?>, Long> {

    private final SessionFactory sessionFactory;
    private final HashMap<QuestRewardType, AbstractQuestReward<?>> rewards = new HashMap<>();
    private final AsyncLoadingCache<Long, QuestRewardModel<?>> cache;

    public QuestRewardService() {
        this.sessionFactory = ConnectionManager.getSessionFactory();

        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getQuestReward);

        loadAll();
    }

    private void loadAll() {
        this.rewards.put(QuestRewardType.ITEMS, new ItemQuestReward());
        this.rewards.put(QuestRewardType.COINS, new CoinQuestReward());
        this.rewards.put(QuestRewardType.EXPERIENCE, new ExperienceQuestReward());
    }

    @Override
    public CompletableFuture<@NotNull ResultType> save(@NotNull QuestRewardModel<?> questRewardModel) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                if (questRewardModel.getId() == null) {
                    session.persist(questRewardModel);
                } else {
                    session.merge(questRewardModel);
                }
                transaction.commit();
                PlayLegendQuest.getLog().info("QuestRewardModel successfully saved with ID: " + questRewardModel.getId());
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save QuestRewardModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    @Override
    public CompletableFuture<ResultType> delete(@NotNull Long id) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                QuestRewardModel<?> questRewardModel = session.get(QuestRewardModel.class, id);
                if (questRewardModel == null) return ResultType.NO_ROWS_AFFECTED;

                session.remove(questRewardModel);
                transaction.commit();
                this.cache.synchronous().invalidate(id);
                PlayLegendQuest.getLog().info("QuestRewardModel successfully deleted.");
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete QuestRewardModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    public CompletableFuture<ResultType> update(long id, @NotNull String newReward, @NotNull QuestRewardType type) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                QuestRewardModel<?> questRewardModel = session.get(QuestRewardModel.class, id);
                if (questRewardModel == null) return ResultType.NO_ROWS_AFFECTED;

                questRewardModel.setRewardString(newReward);
                questRewardModel.setQuestRewardType(type);
                session.merge(questRewardModel);
                transaction.commit();
                PlayLegendQuest.getLog().info("QuestRewardModel successfully updated.");
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to update QuestRewardModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    private CompletableFuture<@Nullable QuestRewardModel<?>> getQuestReward(@NotNull Long id, @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                QuestRewardModel<?> questRewardModel = session.get(QuestRewardModel.class, id);
                return questRewardModel;
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to find QuestRewardModel: " + e.getMessage(), e);
                return null;
            }
        }, executor);
    }

    public CompletableFuture<@Nullable QuestRewardModel<?>> findById(long id) {
        return this.cache.get(id);
    }

    /**
     * Builds the QuestRewardModel from the given type, player and args.
     *
     * @param type   The type of the reward.
     * @param player The player who created the reward.
     * @param args   The arguments for the reward.
     * @param <T>    The type of the reward.
     * @return The QuestRewardModel.
     */
    public @Nullable <T> QuestRewardModel<T> buildQuestRewardModel(@NotNull QuestRewardType type, @NotNull Player player, @NotNull String[] args) {
        AbstractQuestReward<?> questReward = rewards.get(type);
        if (questReward == null) return null;

        AbstractQuestReward<T> typedReward = (AbstractQuestReward<T>) questReward;
        return typedReward.buildQuestRewardModel(player, args);
    }

    /**
     * Builds the reward as a string from the given type, player and args.
     *
     * @param type   The type of the reward.
     * @param player The player who created the reward.
     * @param args   The arguments for the reward.
     * @param <T>    The type of the reward.
     * @return The reward as a string.
     */
    public <T> @Nullable String buildRewardAsString(@NotNull QuestRewardType type, @NotNull Player player, @NotNull String[] args) {
        AbstractQuestReward<?> questReward = rewards.get(type);
        if (questReward == null) return null;

        AbstractQuestReward<T> typedReward = (AbstractQuestReward<T>) questReward;
        return typedReward.genericRewardToString(player, args);
    }

    /**
     * Rewards the player with the given reward.
     *
     * @param type   The type of the reward.
     * @param player The player to reward.
     * @param reward The reward to give.
     * @param <T>    The type of the reward.
     * @return If the player was rewarded.
     */
    public <T> boolean rewardPlayer(@NotNull QuestRewardType type, @NotNull Player player, @NotNull T reward) {
        AbstractQuestReward<?> questReward = rewards.get(type);
        if (questReward == null) return false;
        if (!type.getClassType().isInstance(reward)) return false;

        AbstractQuestReward<T> typedReward = (AbstractQuestReward<T>) questReward;
        typedReward.rewardPlayer(player, reward);
        return true;
    }
}