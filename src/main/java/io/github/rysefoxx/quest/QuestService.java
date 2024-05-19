package io.github.rysefoxx.quest;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionService;
import io.github.rysefoxx.database.IDatabaseOperation;
import io.github.rysefoxx.enums.QuestRequirementType;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.quest.impl.QuestCollectRequirement;
import io.github.rysefoxx.quest.impl.QuestKillRequirement;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnegative;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 17.05.2024
 */
public class QuestService implements IDatabaseOperation<QuestModel, String> {

    private final SessionFactory sessionFactory;
    @Getter
    private final AsyncLoadingCache<String, QuestModel> cache;

    public QuestService() {
        this.sessionFactory = ConnectionService.getSessionFactory();
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getQuestModel);
    }

    @Override
    public CompletableFuture<@NotNull ResultType> save(QuestModel toSave) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                if (session.get(QuestModel.class, toSave.getName()) == null) {
                    session.persist(toSave);
                } else {
                    session.merge(toSave);
                }
                transaction.commit();
                return this.cache.synchronous().refresh(toSave.getName())
                        .thenCompose(v -> CompletableFuture.completedFuture(ResultType.SUCCESS))
                        .exceptionally(e -> {
                            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to refresh QuestModel cache: " + e.getMessage(), e);
                            return ResultType.ERROR;
                        }).get();
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save QuestModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    @Override
    public CompletableFuture<@NotNull ResultType> delete(String toDelete) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                QuestModel questModel = session.get(QuestModel.class, toDelete);
                if (questModel == null) return ResultType.NO_ROWS_AFFECTED;

                session.remove(questModel);
                transaction.commit();
                cache.synchronous().invalidate(toDelete);
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete QuestModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    public CompletableFuture<ResultType> removeRequirement(@NotNull QuestModel questModel, @NotNull AbstractQuestRequirement requirement) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();

                questModel.getRequirements().remove(requirement);
                session.merge(questModel);
                session.remove(session.contains(requirement) ? requirement : session.merge(requirement));

                transaction.commit();
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to remove requirement from QuestModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    private CompletableFuture<QuestModel> getQuestModel(@NotNull String questName, @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.get(QuestModel.class, questName);
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to get QuestModel: " + e.getMessage(), e);
                return null;
            }
        }, executor);
    }

    public CompletableFuture<QuestModel> findByName(@NotNull String questName) {
        return this.cache.get(questName);
    }

    public @NotNull CompletableFuture<@Nullable AbstractQuestRequirement> findRequirementById(@Nonnegative long requirementId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.get(AbstractQuestRequirement.class, requirementId);
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to get AbstractQuestRequirement: " + e.getMessage(), e);
                return null;
            }
        });
    }

    public @Nullable AbstractQuestRequirement createRequirement(@NotNull PlayLegendQuest plugin, @NotNull QuestRequirementType requirementType, @Nonnegative int requirementAmount, @NotNull String[] args) {
        String data = args[5];
        return switch (requirementType) {
            case COLLECT -> {
                Material material = Material.getMaterial(data);
                if (material == null) yield null;

                yield new QuestCollectRequirement(plugin, requirementAmount, material);
            }
            case KILL -> {
                EntityType entityType = EntityType.fromName(data);
                if (entityType == null) yield null;

                yield new QuestKillRequirement(plugin, requirementAmount, entityType);
            }
        };
    }
}