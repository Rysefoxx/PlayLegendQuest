package io.github.rysefoxx.user;

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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 19.05.2024
 */
public class QuestUserService implements IDatabaseOperation<QuestUserModel, Long> {

    private final SessionFactory sessionFactory;
    @Getter
    private final AsyncLoadingCache<Long, QuestUserModel> cache;

    public QuestUserService() {
        this.sessionFactory = ConnectionService.getSessionFactory();
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getQuestUserModel);
    }

    @Override
    public CompletableFuture<@NotNull ResultType> save(QuestUserModel toSave) {
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
                            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to refresh QuestUserModel cache: " + e.getMessage(), e);
                            return ResultType.ERROR;
                        }).get();
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save QuestUserModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    @Override
    public CompletableFuture<@NotNull ResultType> delete(Long toDelete) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                QuestUserModel questUserModel = session.get(QuestUserModel.class, toDelete);
                if (questUserModel == null) return ResultType.NO_ROWS_AFFECTED;

                session.remove(questUserModel);
                transaction.commit();
                cache.synchronous().invalidate(toDelete);
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete QuestUserModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    public CompletableFuture<@NotNull ResultType> deleteByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                QuestUserModel questUserModel = session.createQuery("FROM QuestUserModel WHERE uuid = :uuid", QuestUserModel.class)
                        .setParameter("uuid", uuid)
                        .uniqueResult();

                questUserModel.getQuest().getUserQuests().remove(questUserModel);
                session.remove(questUserModel);

                transaction.commit();
                cache.synchronous().invalidate(questUserModel.getId());
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete QuestUserModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }


    private CompletableFuture<QuestUserModel> getQuestUserModel(@NotNull Long id, @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.get(QuestUserModel.class, id);
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to get QuestUserModel: " + e.getMessage(), e);
                return null;
            }
        }, executor);
    }
}
