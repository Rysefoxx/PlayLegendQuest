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

    /**
     * Creates a new service instance and initializes the cache. The cache will expire after 15 minutes of inactivity. It also loads all rewards and registers them.
     */
    public QuestRewardService(@NotNull PlayLegendQuest plugin) {
        this.sessionFactory = ConnectionService.getSessionFactory();

        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getQuestReward);

        loadAll(plugin);
        register();
    }

    /**
     * Loads all possible rewards. Currently only items, coins and experience are supported. You can add more rewards by adding them here.
     *
     * @param plugin The plugin instance.
     */
    private void loadAll(@NotNull PlayLegendQuest plugin) {
        this.rewards.put(QuestRewardType.ITEMS, new ItemQuestReward(plugin));
        this.rewards.put(QuestRewardType.COINS, new CoinQuestReward(plugin));
        this.rewards.put(QuestRewardType.EXPERIENCE, new ExperienceQuestReward(plugin));
    }

    /**
     * Saves the object to the database.
     *
     * @param toSave The object to save.
     * @return The result of the operation.
     */
    @Override
    public @NotNull CompletableFuture<@NotNull ResultType> save(@NotNull QuestRewardModel toSave) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                if (toSave.getId() == null) {
                    session.persist(toSave);
                } else {
                    session.merge(toSave);
                }
                transaction.commit();
                return this.cache.synchronous().refresh(toSave.getId())
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

    /**
     * Deletes the object from the database by the given identifier.
     *
     * @param toDelete The id to delete.
     * @return The result of the operation.
     */
    @Override
    public @NotNull CompletableFuture<@NotNull ResultType> delete(@NotNull Long toDelete) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                QuestRewardModel questRewardModel = session.get(QuestRewardModel.class, toDelete);
                if (questRewardModel == null) return ResultType.NO_ROWS_AFFECTED;

                session.remove(questRewardModel);
                transaction.commit();
                this.cache.synchronous().invalidate(toDelete);
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete QuestRewardModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    /**
     * Updates the object in the database by the given identifier.
     *
     * @param id        The id to update.
     * @param newReward The new reward.
     * @param type      The new type.
     * @return The result of the operation.
     */
    public @NotNull CompletableFuture<@NotNull ResultType> update(long id, @NotNull String newReward, @NotNull QuestRewardType type) {
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

    /**
     * Gets the QuestRewardModel from the database by the given identifier.
     *
     * @param id       The id to get.
     * @param executor The executor to run the operation on.
     * @return The QuestRewardModel or null if an error occurred.
     */
    private @NotNull CompletableFuture<@Nullable QuestRewardModel> getQuestReward(@NotNull Long id, @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.get(QuestRewardModel.class, id);
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to find QuestRewardModel: " + e.getMessage(), e);
                return null;
            }
        }, executor);
    }

    /**
     * Finds the QuestRewardModel by the given identifier in the cache. If the object is not in the cache, it will be loaded from the database.
     *
     * @param id The id to find.
     * @return The QuestRewardModel or null if an error occurred.
     */
    public @NotNull CompletableFuture<@Nullable QuestRewardModel> findById(long id) {
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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public <T> void rewardPlayer(@NotNull Player player, @NotNull QuestModel questModel) {
        for (QuestRewardModel reward : questModel.getRewards()) {
            QuestRewardType type = reward.getQuestRewardType();
            AbstractQuestReward<?> questReward = rewards.get(type);
            if (questReward == null) continue;

            AbstractQuestReward<T> typedReward = (AbstractQuestReward<T>) questReward;
            typedReward.rewardPlayer(player, typedReward.rewardStringToGeneric(reward.getReward()));
        }
    }

    /**
     * Finds all QuestRewardModels in the database.
     *
     * @return The list of QuestRewardModels or null if an error occurred.
     */
    private @NotNull CompletableFuture<@Nullable List<QuestRewardModel>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.createQuery("FROM QuestRewardModel", QuestRewardModel.class).list();
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to find all QuestRewardModel: " + e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * Registers all rewards and their services.
     */
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