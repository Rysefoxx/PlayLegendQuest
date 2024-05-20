package io.github.rysefoxx.user;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionService;
import io.github.rysefoxx.database.IDatabaseOperation;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.scoreboard.ScoreboardService;
import io.github.rysefoxx.util.LogUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
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
    private final QuestUserProgressService questUserProgressService;
    private final LanguageService languageService;
    private final ScoreboardService scoreboardService;
    private final QuestService questService;

    /**
     * Creates a new service instance and defines the AsyncCache, which stores data temporarily and deletes it 15 minutes after the last access. An asynchronous scheduler is also started, which checks whether the quest has expired.
     */
    public QuestUserService(@NotNull PlayLegendQuest plugin,
                            @NotNull QuestUserProgressService questUserProgressService,
                            @NotNull LanguageService languageService,
                            @NotNull ScoreboardService scoreboardService,
                            @NotNull QuestService questService) {
        this.questUserProgressService = questUserProgressService;
        this.languageService = languageService;
        this.scoreboardService = scoreboardService;
        this.questService = questService;
        this.sessionFactory = ConnectionService.getSessionFactory();
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getQuestUserModel);
        expirationScheduler(plugin);
    }

    /**
     * Saves the object to the database.
     *
     * @param toSave The object to save.
     * @return The result of the operation.
     */
    @Override
    public @NotNull CompletableFuture<@NotNull ResultType> save(@NotNull QuestUserModel toSave) {
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
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save QuestUserModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        }).thenCompose(result -> result == ResultType.SUCCESS ? refreshCache(toSave.getId()) : CompletableFuture.completedFuture(result));
    }

    /**
     * Refreshes the cache for the given identifier.
     *
     * @param id The identifier of the quest user model.
     * @return The result of the operation.
     */
    private @NotNull CompletableFuture<@NotNull ResultType> refreshCache(@NotNull Long id) {
        return this.cache.synchronous().refresh(id)
                .thenApply(v -> ResultType.SUCCESS)
                .exceptionally(throwable -> {
                    LogUtils.handleError(null, "Failed to refresh QuestUserModel cache", throwable);
                    return ResultType.ERROR;
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

    /**
     * Deletes the object from the database by the given uuid.
     *
     * @param uuid The uuid to delete.
     * @return The result of the operation.
     */
    public @NotNull CompletableFuture<@NotNull ResultType> deleteByUuid(@NotNull UUID uuid) {
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

    /**
     * Searches for a QuestUserModel by the given id.
     *
     * @param id The id to search for.
     * @return The QuestUserModel or null if the search failed.
     */
    private @NotNull CompletableFuture<@Nullable QuestUserModel> getQuestUserModel(@NotNull Long id, @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.get(QuestUserModel.class, id);
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to get QuestUserModel: " + e.getMessage(), e);
                return null;
            }
        }, executor);
    }

    /**
     * Starts the asynchronous scheduler, which checks every second whether the quest has expired. If the quest has expired, it will be deleted from the database.
     *
     * @param plugin The plugin instance.
     */
    private void expirationScheduler(@NotNull PlayLegendQuest plugin) {
        if (PlayLegendQuest.isUnitTest()) return;

        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> cache.synchronous().asMap().forEach(this::handleQuestExpiration), 0, 1, TimeUnit.SECONDS);
    }

    /**
     * Checks whether the quest has expired. If the quest has expired, it will be deleted from the database.
     *
     * @param id             The id of the quest.
     * @param questUserModel The quest user model.
     */
    private void handleQuestExpiration(@NotNull Long id, @NotNull QuestUserModel questUserModel) {
        if (questUserModel.getExpiration().isAfter(LocalDateTime.now())) return;

        Player player = Bukkit.getPlayer(questUserModel.getUuid());
        questUserProgressService.findByUuid(questUserModel.getUuid())
                .thenCompose(questUserProgressModels -> {
                    if (questUserProgressModels == null || questUserProgressModels.isEmpty())
                        return CompletableFuture.completedFuture(null);

                    QuestUserProgressModel questUserProgressModel = questUserProgressModels.get(0);
                    QuestModel quest = questUserProgressModel.getQuest();

                    if (quest == null) return CompletableFuture.completedFuture(null);

                    quest.removeUserProgress(questUserProgressModel);
                    quest.getUserQuests().removeIf(userModel -> userModel.getUuid().equals(questUserModel.getUuid()));

                    cache.synchronous().invalidate(id);

                    return questUserProgressService.deleteQuest(questUserModel.getUuid(), quest.getName())
                            .thenCompose(progressResultType -> questService.getCache().synchronous().refresh(quest.getName())
                                    .thenAccept(unused -> notifyPlayerOnExpiration(player, progressResultType)));
                })
                .exceptionally(throwable -> LogUtils.handleError(player, "Error while finding user progress", throwable));
    }

    /**
     * Notifies the player that the quest has expired.
     *
     * @param player             The player to notify.
     * @param progressResultType The result of the operation.
     */
    private void notifyPlayerOnExpiration(@Nullable Player player, @NotNull ResultType progressResultType) {
        if (player == null) return;

        scoreboardService.update(player);
        languageService.sendTranslatedMessage(player, "quest_expired_" + progressResultType.toString().toLowerCase());
    }
}
