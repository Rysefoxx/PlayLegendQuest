package io.github.rysefoxx.progress;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionService;
import io.github.rysefoxx.database.IDatabaseOperation;
import io.github.rysefoxx.enums.ResultType;
import lombok.Getter;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

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
    @Getter
    private final AsyncLoadingCache<UUID, List<QuestUserProgressModel>> cache;

    public QuestUserProgressService() {
        this.sessionFactory = ConnectionService.getSessionFactory();
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getQuestUserProgressModels);
    }

    @Override
    public CompletableFuture<@NotNull ResultType> save(QuestUserProgressModel toSave) {
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
                return this.cache.synchronous().refresh(toSave.getUuid())
                        .thenCompose(v -> CompletableFuture.completedFuture(ResultType.SUCCESS))
                        .exceptionally(e -> {
                            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to refresh QuestUserProgressModel cache: " + e.getMessage(), e);
                            return ResultType.ERROR;
                        }).get();
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save QuestUserProgressModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    @Override
    public CompletableFuture<@NotNull ResultType> delete(UUID toDelete) {
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

    public CompletableFuture<@NotNull ResultType> deleteQuest(@NotNull UUID uuid, @NotNull String questName) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                List<QuestUserProgressModel> questUserProgressModels = session.createQuery("FROM QuestUserProgressModel WHERE uuid = :uuid AND quest.name = :questName", QuestUserProgressModel.class)
                        .setParameter("uuid", uuid)
                        .setParameter("questName", questName)
                        .list();
                if (questUserProgressModels.isEmpty()) return ResultType.NO_ROWS_AFFECTED;

                for (QuestUserProgressModel questUserProgressModel : questUserProgressModels) {
                    questUserProgressModel.getQuest().getUserProgress().remove(questUserProgressModel);
                    session.remove(questUserProgressModel);
                }
                transaction.commit();

                return this.cache.synchronous().refresh(uuid)
                        .thenCompose(v -> CompletableFuture.completedFuture(ResultType.SUCCESS))
                        .exceptionally(e -> {
                            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to refresh QuestUserProgressModel cache: " + e.getMessage(), e);
                            return ResultType.ERROR;
                        }).get();
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete QuestUserProgressModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    private CompletableFuture<List<QuestUserProgressModel>> getQuestUserProgressModels(@NotNull UUID uuid, @NotNull Executor executor) {
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

    public CompletableFuture<List<QuestUserProgressModel>> findByUuid(@NotNull UUID uuid) {
        return this.cache.get(uuid);
    }

    public CompletableFuture<Boolean> hasQuest(@NotNull UUID uuid) {
        return findByUuid(uuid).thenApply(questUserProgressModels -> !questUserProgressModels.isEmpty());
    }

    public CompletableFuture<Boolean> isQuestCompleted(@NotNull UUID uuid, @NotNull String questName) {
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
