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

    public QuestUserService(@NotNull PlayLegendQuest plugin, @NotNull QuestUserProgressService questUserProgressService, @NotNull LanguageService languageService, @NotNull ScoreboardService scoreboardService, @NotNull QuestService questService) {
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

    private void expirationScheduler(@NotNull PlayLegendQuest plugin) {
        if(PlayLegendQuest.isUnitTest()) return;

        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, scheduledTask -> cache.synchronous().asMap().forEach(this::handleQuestExpiration), 0, 1, TimeUnit.SECONDS);
    }

    private void handleQuestExpiration(@NotNull Long id, @NotNull QuestUserModel questUserModel) {
        if (questUserModel.getExpiration().isAfter(LocalDateTime.now())) return;

        Player player = Bukkit.getPlayer(questUserModel.getUuid());
        questUserProgressService.findByUuid(questUserModel.getUuid())
                .thenCompose(questUserProgressModels -> {
                    if (questUserProgressModels == null || questUserProgressModels.isEmpty())
                        return CompletableFuture.completedFuture(null);

                    QuestUserProgressModel questUserProgressModel = questUserProgressModels.get(0);
                    QuestModel quest = questUserProgressModel.getQuest();

                    quest.getUserProgress().remove(questUserProgressModel);
                    quest.getUserQuests().removeIf(userModel -> userModel.getUuid().equals(questUserModel.getUuid()));

                    cache.synchronous().invalidate(id);

                    return questUserProgressService.deleteQuest(questUserModel.getUuid(), quest.getName())
                            .thenCompose(progressResultType -> questService.getCache().synchronous().refresh(quest.getName()).thenAccept(unused -> notifyPlayerOnExpiration(player, progressResultType)));
                })
                .exceptionally(throwable -> handleError(player, throwable));
    }

    private void notifyPlayerOnExpiration(@Nullable Player player, @NotNull ResultType progressResultType) {
        if (player == null) return;

        scoreboardService.update(player);
        languageService.sendTranslatedMessage(player, "quest_expired_" + progressResultType.toString().toLowerCase());
    }

    private @Nullable Void handleError(@Nullable Player player, @NotNull Throwable throwable) {
        if (player != null) {
            player.sendRichMessage("Error while searching for quest user progress");
        }
        PlayLegendQuest.getLog().log(Level.SEVERE, "Error while searching for quest user progress" + ": " + throwable.getMessage(), throwable);
        return null;
    }
}
