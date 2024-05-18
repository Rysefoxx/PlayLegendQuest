package io.github.rysefoxx.stats;

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

    public PlayerStatisticsService() {
        this.sessionFactory = ConnectionManager.getSessionFactory();
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(15, TimeUnit.MINUTES)
                .buildAsync(this::getOrCreatePlayerStats);
    }

    @Override
    public CompletableFuture<@NotNull ResultType> save(PlayerStatisticsModel toSave) {
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

                this.cache.put(toSave.getUuid(), CompletableFuture.completedFuture(toSave));
                PlayLegendQuest.getLog().info("PlayerStatisticsModel successfully saved.");
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save PlayerStatisticsModel: " + e.getMessage(), e);
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
                PlayerStatisticsModel playerStats = session.get(PlayerStatisticsModel.class, toDelete);
                if (playerStats == null) return ResultType.NO_ROWS_AFFECTED;

                session.remove(playerStats);
                transaction.commit();
                this.cache.synchronous().invalidate(toDelete);

                PlayLegendQuest.getLog().info("PlayerStatisticsModel successfully deleted.");
                return ResultType.SUCCESS;
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to delete PlayerStatisticsModel: " + e.getMessage(), e);
                return ResultType.ERROR;
            }
        });
    }

    private CompletableFuture<@Nullable PlayerStatisticsModel> getOrCreatePlayerStats(@NotNull UUID uuid, @NotNull Executor executor) {
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

    public CompletableFuture<@Nullable PlayerStatisticsModel> getPlayerStats(@NotNull UUID uuid) {
        return this.cache.get(uuid);
    }
}