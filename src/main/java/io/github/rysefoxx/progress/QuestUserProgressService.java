package io.github.rysefoxx.progress;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionManager;
import io.github.rysefoxx.database.IDatabaseOperation;
import io.github.rysefoxx.enums.ResultType;
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
    private final AsyncLoadingCache<UUID, List<QuestUserProgressModel>> cache;

    public QuestUserProgressService() {
        this.sessionFactory = ConnectionManager.getSessionFactory();
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
                cache.synchronous().invalidate(toSave.getUuid());
                return ResultType.SUCCESS;
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

    private CompletableFuture<List<QuestUserProgressModel>> getQuestUserProgressModels(@NotNull UUID uuid, @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.createQuery("FROM QuestUserProgressModel WHERE uuid = :uuid", QuestUserProgressModel.class)
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
}
