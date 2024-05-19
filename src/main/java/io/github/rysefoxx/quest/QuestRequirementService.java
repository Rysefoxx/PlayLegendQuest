package io.github.rysefoxx.quest;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 18.05.2024
 */
public class QuestRequirementService {

    private final SessionFactory sessionFactory;

    public QuestRequirementService(@NotNull PlayLegendQuest plugin) {
        this.sessionFactory = ConnectionService.getSessionFactory();

        registerListener(plugin);
    }

    public CompletableFuture<@Nullable Long> save(AbstractQuestRequirement toSave) {
        return CompletableFuture.supplyAsync(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                session.persist(toSave);
                transaction.commit();
                return toSave.getId();
            } catch (Exception e) {
                if (transaction != null) transaction.rollback();
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to save requirement: " + e.getMessage(), e);
                return null;
            }
        });
    }

    private CompletableFuture<List<AbstractQuestRequirement>> findAll() {
        return CompletableFuture.supplyAsync(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.createQuery("FROM AbstractQuestRequirement", AbstractQuestRequirement.class).list();
            } catch (Exception e) {
                PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to find all requirements: " + e.getMessage(), e);
                return null;
            }
        });
    }

    /**
     * Registers all listeners for the implemented {@link AbstractQuestRequirement}
     *
     * @param plugin the plugin instance
     */
    private void registerListener(@NotNull PlayLegendQuest plugin) {
        findAll().thenAccept(abstractQuestRequirements -> {
            if (abstractQuestRequirements == null) return;

            for (AbstractQuestRequirement abstractQuestRequirement : abstractQuestRequirements) {
                if (abstractQuestRequirement == null) continue;
                abstractQuestRequirement.setPlugin(plugin);
                abstractQuestRequirement.register();
            }
        }).exceptionally(e -> {
            PlayLegendQuest.getLog().log(Level.SEVERE, "Failed to register listeners: " + e.getMessage(), e);
            return null;
        });
    }

}