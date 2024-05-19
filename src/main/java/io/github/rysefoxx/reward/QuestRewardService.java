package io.github.rysefoxx.reward;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionService;
import io.github.rysefoxx.database.IDatabaseOperation;
import io.github.rysefoxx.enums.QuestRewardType;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.quest.QuestModel;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class QuestRewardService implements IDatabaseOperation<QuestRewardModel, Long> {

    private final SessionFactory sessionFactory;
    private final HashMap<QuestRewardType, AbstractQuestReward<?>> rewards = new HashMap<>();
    private final AsyncLoadingCache<Long, QuestRewardModel> cache;

    public QuestRewardService(@NotNull PlayLegendQuest plugin) {
        this.sessionFactory = ConnectionService.getSessionFactory();

        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getQuestReward);

        loadAll(plugin);
        register();
    }

    private void loadAll(@NotNull PlayLegendQuest plugin) {
        this.rewards.put(QuestRewardType.ITEMS, new ItemQuestReward(plugin));
        this.rewards.put(QuestRewardType.COINS, new CoinQuestReward(plugin));
        this.rewards.put(QuestRewardType.EXPERIENCE, new ExperienceQuestReward(plugin));
    }

    @Override
    public CompletableFuture<@NotNull ResultType> save(@NotNull QuestRewardModel questRewardModel) {
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
                return this.cache.synchronous().refresh(questRewardModel.getId())
                        .thenCompose(v -> CompletableFuture.completedFuture(ResultType.SUCCESS))
                        .exceptionally(e -> {
                            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to refresh QuestRewardModel cache: " + e.getMessage(), e);
                            return ResultType.ERROR;
                        }).get();
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

                QuestRewardModel questRewardModel = session.get(QuestRewardModel.class, id);
                if (questRewardModel == null) return ResultType.NO_ROWS_AFFECTED;

                session.remove(questRewardModel);
                transaction.commit();
                this.cache.synchronous().invalidate(id);
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

                QuestRewardModel questRewardModel = session.get(QuestRewardModel.class, id);
                if (questRewardModel == null) return ResultType.NO_ROWS_AFFECTED;

                questRewardModel.setReward(newReward);
                questRewardModel.setQuestRewardType(type);
                session.merge(questRewardModel);
                transaction.commit();
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to update QuestRewardModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    private CompletableFuture<@Nullable QuestRewardModel> getQuestReward(@NotNull Long id, @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.get(QuestRewardModel.class, id);
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to find QuestRewardModel: " + e.getMessage(), e);
                return null;
            }
        }, executor);
    }

    public CompletableFuture<@Nullable QuestRewardModel> findById(long id) {
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
    public @Nullable <T> QuestRewardModel buildQuestRewardModel(@NotNull QuestRewardType type, @NotNull Player player, @NotNull String[] args) {
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
     * Rewards the player with the given rewards.
     *
     * @param player     The player to reward.
     * @param questModel The quest model to reward the player with.
     * @param <T>        The type of the reward.
     */
    public <T> void rewardPlayer(@NotNull Player player, @NotNull QuestModel questModel) {
        for (QuestRewardModel reward : questModel.getRewards()) {
            QuestRewardType type = reward.getQuestRewardType();
            AbstractQuestReward<?> questReward = rewards.get(type);
            if (questReward == null) continue;

            AbstractQuestReward<T> typedReward = (AbstractQuestReward<T>) questReward;
            typedReward.rewardPlayer(player, typedReward.rewardStringToGeneric(reward.getReward()));
        }
    }

    private CompletableFuture<List<QuestRewardModel>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.createQuery("FROM QuestRewardModel", QuestRewardModel.class).list();
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to find all QuestRewardModel: " + e.getMessage(), e);
                return null;
            }
        });
    }

    private void register() {
        findAll().thenAccept(questRewardModels -> {
            if (questRewardModels == null) return;

            for (QuestRewardModel questRewardModel : questRewardModels) {
                if (questRewardModel == null) continue;

                AbstractQuestReward<?> abstractQuestReward = rewards.get(questRewardModel.getQuestRewardType());
                if (abstractQuestReward == null) continue;

                abstractQuestReward.register();
            }
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to register listeners: " + e.getMessage(), e);
            return null;
        });
    }
}