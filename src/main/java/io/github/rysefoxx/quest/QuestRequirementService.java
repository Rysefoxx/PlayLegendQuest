package io.github.rysefoxx.quest;

import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.database.ConnectionService;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 18.05.2024
 */
public class QuestRequirementService {

    private final SessionFactory sessionFactory;

    public QuestRequirementService() {
        this.sessionFactory = ConnectionService.getSessionFactory();
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

}