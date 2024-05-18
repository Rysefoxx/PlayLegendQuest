package io.github.rysefoxx.database;

import io.github.rysefoxx.PlayLegendQuest;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class DatabaseTableService {

    private final PlayLegendQuest plugin;
    private final ConnectionService connectionService;

    /**
     * Create the tables for the database.
     *
     * @param plugin            The plugin instance.
     * @param connectionService The connection manager.
     */
    public DatabaseTableService(@NotNull PlayLegendQuest plugin, @NotNull ConnectionService connectionService) {
        this.plugin = plugin;
        this.connectionService = connectionService;
        createDefaultTables();
    }

    /**
     * Executes the queries from tables.sql. If tables.sql is not found, the server is shut down. <br>
     * The creation takes place synchronously, as there are no users on the server when the plugin is started and the users should only join when the tables have been created.
     */
    private void createDefaultTables() {
        String[] data;

        try (InputStream inputStream = Objects.requireNonNull(getClass().getClassLoader()
                .getResourceAsStream("tables.sql"))) {
            data = IOUtils.toString(inputStream, StandardCharsets.UTF_8).split(";");
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Could not load tables.sql", e);

            // Without a database, nothing works, so we shut down the server.
            Bukkit.shutdown();
            return;
        }

        for (String query : data) {
            if (query == null || query.isEmpty() || query.isBlank()) continue;

            try (Connection connection = this.connectionService.getConnection()) {
                if (connection == null) {
                    this.plugin.getLogger().severe("Failed to get connection from datasource!");
                    break;
                }

                PreparedStatement statement = connection.prepareStatement(query);
                statement.execute();
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Failed to execute query: " + query, e);
            }
        }
    }
}