package io.github.rysefoxx.database;

import io.github.rysefoxx.enums.ResultType;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public interface IDatabaseOperation<O, I> {

    /**
     * Saves the object to the database.
     *
     * @param toSave The object to save.
     * @return The result of the operation.
     */
    CompletableFuture<@NotNull ResultType> save(O toSave);

    /**
     * Deletes the object from the database by the given identifier.
     *
     * @param toDelete The id to delete.
     * @return The result of the operation.
     */
    CompletableFuture<@NotNull ResultType> delete(I toDelete);

}
