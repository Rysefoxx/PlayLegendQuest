package io.github.rysefoxx.stats;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionService;
import io.github.rysefoxx.database.IDatabaseOperation;
import io.github.rysefoxx.enums.ResultType;
import io.github.rysefoxx.util.LogUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class PlayerStatisticsService implements IDatabaseOperation<PlayerStatisticsModel, UUID> {

    private final SessionFactory sessionFactory;
    private final AsyncLoadingCache<UUID, PlayerStatisticsModel> cache;

    /**
     * Creates a new service instance and initializes the cache. The cache will expire after 15 minutes of inactivity.
     */
    public PlayerStatisticsService() {
        this.sessionFactory = ConnectionService.getSessionFactory();
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getOrCreatePlayerStats);
    }

    /**
     * Saves the object to the database.
     *
     * @param toSave The object to save.
     * @return The result of the operation.
     */
    @Override
    public @NotNull CompletableFuture<@NotNull ResultType> save(@NotNull PlayerStatisticsModel toSave) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                if (session.get(PlayerStatisticsModel.class, toSave.getUuid()) == null) {
                    session.persist(toSave);
                } else {
                    session.merge(toSave);
                }
                transaction.commit();
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save PlayerStatisticsModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        }).thenCompose(result -> result == ResultType.SUCCESS ? refreshCache(toSave.getUuid()) : CompletableFuture.completedFuture(result));
    }

    /**
     * Refreshes the cache for the given identifier.
     *
     * @param uuid The identifier of the player statistics model.
     * @return The result of the operation.
     */
    private @NotNull CompletableFuture<@NotNull ResultType> refreshCache(UUID uuid) {
        return this.cache.synchronous().refresh(uuid)
                .thenApply(v -> ResultType.SUCCESS)
                .exceptionally(throwable -> {
                    LogUtils.handleError(null, "Failed to refresh PlayerStatisticsModel cache", throwable);
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
    public @NotNull CompletableFuture<@NotNull ResultType> delete(@NotNull UUID toDelete) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                PlayerStatisticsModel playerStats = session.get(PlayerStatisticsModel.class, toDelete);
                if (playerStats == null) return ResultType.NO_ROWS_AFFECTED;

                session.remove(playerStats);
                transaction.commit();
                this.cache.synchronous().invalidate(toDelete);
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete PlayerStatisticsModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    /**
     * Retrieves the player statistics from the database or creates a new one if it doesn't exist.
     *
     * @param uuid     The UUID of the player.
     * @param executor The executor to run the task on.
     * @return The player statistics model. Or null if an error occurred.
     */
    private @NotNull CompletableFuture<@Nullable PlayerStatisticsModel> getOrCreatePlayerStats(@NotNull UUID uuid, @NotNull Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                PlayerStatisticsModel playerStats = session.get(PlayerStatisticsModel.class, uuid);

                if (playerStats == null) {
                    playerStats = new PlayerStatisticsModel(uuid);
                    this.save(playerStats).thenAccept(resultType -> {
                        if (resultType != ResultType.ERROR) return;
                        PlayLegendQuest.getLog().severe("Failed to save PlayerStatisticsModel for UUID: " + uuid);
                    });
                }

                return playerStats;
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to find or create PlayerStatisticsModel: " + e.getMessage(), e);
                return null;
            }
        }, executor);
    }

    /**
     * Retrieves the player statistics from the cache. If the player statistics are not in the cache, it will be loaded from the database.
     *
     * @param uuid The UUID of the player.
     * @return The player statistics model. Or null if an error occurred.
     */
    public @NotNull CompletableFuture<@Nullable PlayerStatisticsModel> getPlayerStats(@NotNull UUID uuid) {
        return this.cache.get(uuid);
    }
}