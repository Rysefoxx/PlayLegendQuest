package io.github.rysefoxx.progress;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionService;
import io.github.rysefoxx.database.IDatabaseOperation;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.user.QuestUserModel;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 17.05.2024
 */
public class QuestUserProgressService implements IDatabaseOperation<QuestUserProgressModel, UUID> {

    private final SessionFactory sessionFactory;
    private final AsyncLoadingCache<UUID, List<QuestUserProgressModel>> cache;

    /**
     * Creates a new service instance and initializes the cache. The cache will expire after 15 minutes of inactivity.
     */
    public QuestUserProgressService() {
        this.sessionFactory = ConnectionService.getSessionFactory();
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getQuestUserProgressModels);
    }

    /**
     * Saves the object to the database.
     *
     * @param toSave The object to save.
     * @return The result of the operation.
     */
    @Override
    public @NotNull CompletableFuture<@NotNull ResultType> save(@NotNull QuestUserProgressModel toSave) {
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
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save QuestUserProgressModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        }).thenCompose(result -> result == ResultType.SUCCESS ? refreshCache(toSave.getUuid()) : CompletableFuture.completedFuture(result));
    }

    /**
     * Deletes the object from the database by the given identifier.
     *
     * @param toDelete The id to delete.
     * @return The result of the operation.
     */
    @Override
    public @NotNull CompletableFuture<@NotNull ResultType> delete(@NotNull UUID toDelete) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                List<QuestUserProgressModel> questUserProgressModels = session.createQuery("FROM QuestUserProgressModel WHERE uuid = :uuid", QuestUserProgressModel.class)
                        .setParameter("uuid", toDelete)
                        .list();
                if (questUserProgressModels.isEmpty()) return ResultType.NO_ROWS_AFFECTED;

                for (QuestUserProgressModel questUserProgressModel : questUserProgressModels) {
                    questUserProgressModel.getQuest().getUserProgress().remove(questUserProgressModel);
                    session.remove(questUserProgressModel);
                }
                transaction.commit();
                cache.synchronous().invalidate(toDelete);
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete QuestUserProgressModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    /**
     * Deletes the quest from the user progress.
     *
     * @param uuid      The uuid of the user.
     * @param questName The name of the quest.
     * @return The result of the operation.
     */
    public @NotNull CompletableFuture<@NotNull ResultType> deleteQuest(@NotNull UUID uuid, @NotNull String questName) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                List<QuestUserProgressModel> questUserProgressModels = session.createQuery("FROM QuestUserProgressModel WHERE uuid = :uuid AND quest.name = :questName", QuestUserProgressModel.class)
                        .setParameter("uuid", uuid)
                        .setParameter("questName", questName)
                        .list();
                if (questUserProgressModels.isEmpty()) return ResultType.NO_ROWS_AFFECTED;

                QuestModel questModel = questUserProgressModels.get(0).getQuest();
                deleteUserProgress(questUserProgressModels, questModel, session);
                deleteUserModel(session, uuid, questName, questModel);

                transaction.commit();
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete QuestUserModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        }).thenCompose(result -> result == ResultType.SUCCESS ? refreshCache(uuid) : CompletableFuture.completedFuture(result));
    }

    /**
     * Deletes the user progress from the database.
     *
     * @param questUserProgressModels The user progress models to delete.
     * @param questModel              The quest model.
     * @param session                 The session to use.
     */
    private void deleteUserProgress(@NotNull List<QuestUserProgressModel> questUserProgressModels, @NotNull QuestModel questModel, @NotNull Session session) {
        questModel.getUserProgress().removeAll(questUserProgressModels);
        for (QuestUserProgressModel questUserProgressModel : questUserProgressModels) {
            questUserProgressModel.setQuest(null);
            session.remove(questUserProgressModel);
        }
    }

    /**
     * Deletes the user model from the database.
     *
     * @param session    The session to use.
     * @param uuid       The uuid of the user.
     * @param questName  The name of the quest.
     * @param questModel The quest model.
     */
    private void deleteUserModel(@NotNull Session session, @NotNull UUID uuid, @NotNull String questName, @NotNull QuestModel questModel) {
        QuestUserModel questUserModel = session.createQuery("FROM QuestUserModel WHERE uuid = :uuid AND quest.name = :questName", QuestUserModel.class)
                .setParameter("uuid", uuid)
                .setParameter("questName", questName)
                .uniqueResult();
        if (questUserModel != null) {
            questModel.removeUserQuest(questUserModel);
            session.remove(questUserModel);
        }
    }

    /**
     * Refreshes the cache for the given identifier.
     *
     * @param uuid The identifier to refresh the cache for.
     * @return The result of the operation.
     */
    private @NotNull CompletableFuture<@NotNull ResultType> refreshCache(UUID uuid) {
        return this.cache.synchronous().refresh(uuid)
                .thenApply(v -> ResultType.SUCCESS)
                .exceptionally(e -> {
                    PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to refresh QuestUserProgressModel cache: " + e.getMessage(), e);
                    return ResultType.ERROR;
                });
    }


    /**
     * Gets the user progress models from the database.
     *
     * @param uuid     The uuid of the user.
     * @param executor The executor to run the task on.
     * @return The user progress models.
     */
    private @NotNull CompletableFuture<@Nullable List<QuestUserProgressModel>> getQuestUserProgressModels(@NotNull UUID uuid, @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.createQuery("FROM QuestUserProgressModel WHERE uuid = :uuid AND completed = false", QuestUserProgressModel.class)
                        .setParameter("uuid", uuid)
                        .list();
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to get QuestUserProgressModels: " + e.getMessage(), e);
                return null;
            }
        }, executor);
    }

    /**
     * Finds the user progress models by the given identifier from the cache. If the cache does not contain the models, they will be loaded from the database.
     *
     * @param uuid The identifier to find the models by.
     * @return The user progress models.
     */
    public @NotNull CompletableFuture<List<QuestUserProgressModel>> findByUuid(@NotNull UUID uuid) {
        return this.cache.get(uuid);
    }

    /**
     * Checks if the user has a quest.
     *
     * @param uuid The uuid of the user.
     * @return True if the user has a quest, otherwise false.
     */
    public @NotNull CompletableFuture<Boolean> hasQuest(@NotNull UUID uuid) {
        return findByUuid(uuid).thenApply(questUserProgressModels -> !questUserProgressModels.isEmpty());
    }

    /**
     * Checks if the quest is completed.
     *
     * @param uuid      The uuid of the user.
     * @param questName The name of the quest.
     * @return True if the quest is completed, otherwise false.
     */
    public @NotNull CompletableFuture<@NotNull Boolean> isQuestCompleted(@NotNull UUID uuid, @NotNull String questName) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return !session.createQuery("FROM QuestUserProgressModel WHERE uuid = :uuid AND quest.name = :questName AND completed = true", QuestUserProgressModel.class)
                        .setParameter("uuid", uuid)
                        .setParameter("questName", questName)
                        .list()
                        .isEmpty();
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to check if quest is completed: " + e.getMessage(), e);
                return false;
            }
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to check if quest is completed: " + e.getMessage(), e);
            return false;
        });
    }
}
