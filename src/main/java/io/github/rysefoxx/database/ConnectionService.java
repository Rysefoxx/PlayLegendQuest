package io.github.rysefoxx.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.rysefoxx.PlayLegendQuest;
import io.github.rysefoxx.progress.QuestUserProgressModel;
import io.github.rysefoxx.quest.AbstractQuestRequirement;
import io.github.rysefoxx.quest.QuestModel;
import io.github.rysefoxx.quest.impl.QuestCollectRequirement;
import io.github.rysefoxx.quest.impl.QuestKillRequirement;
import io.github.rysefoxx.reward.QuestRewardModel;
import io.github.rysefoxx.stats.PlayerStatisticsModel;
import io.github.rysefoxx.user.QuestUserModel;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

/**
 * @author Rysefoxx
 * @since 02.01.2024
 */
public class ConnectionService {

    @Getter
    private static SessionFactory sessionFactory;
    private final PlayLegendQuest plugin;
    @Getter
    private HikariDataSource dataSource;

    /**
     * Loads the database.yml and sets up the HikariCP datasource.
     *
     * @param plugin The plugin instance.
     */
    public ConnectionService(@NotNull PlayLegendQuest plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
        setupHikariCP();
    }

    /**
     * Closes the connection to the database.
     */
    public void closeConnection() {
        if (this.dataSource == null) return;
        if (this.dataSource.isClosed()) return;
        this.dataSource.close();
    }

    /**
     * Saves the default config to the plugin folder.
     */
    private void saveDefaultConfig() {
        this.plugin.saveResource("database.yml", false);
    }

    /**
     * Sets up the HikariCP datasource.
     */
    private void setupHikariCP() {
        File file = new File(this.plugin.getDataFolder(), "database.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!isValidConfig(config)) {
            this.plugin.getLogger().severe("Failed to load database.yml! Shutting down the server.");
            // Without a database, nothing works, so we shut down the server.
            Bukkit.shutdown();
            return;
        }

        String url = buildJdbcUrl(config);
        setupDataSource(url, config);
        setupHibernate();
    }

    /**
     * Checks if the database config is valid. Its valid when all required fields are set.
     *
     * @param config The config to check.
     * @return true if valid, false if not.
     */
    private boolean isValidConfig(@NotNull YamlConfiguration config) {
        return config.getString("host") != null
                && config.getString("port") != null
                && config.getString("database") != null
                && config.getString("username") != null
                && config.getString("password") != null;
    }

    /**
     * Builds the jdbc url from the config.
     *
     * @param config The config to build the url from.
     * @return The jdbc url.
     */
    private @NotNull String buildJdbcUrl(@NotNull YamlConfiguration config) {
        String host = config.getString("host");
        String port = config.getString("port");
        String database = config.getString("database");
        return String.format("jdbc:mariadb://%s:%s/%s?useSSL=false", host, port, database);
    }

    /**
     * Sets up the datasource with the given hikari config.
     *
     * @param url    The jdbc url.
     * @param config The config to build the datasource from.
     */
    private void setupDataSource(@NotNull String url, @NotNull YamlConfiguration config) {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(config.getString("username"));
            hikariConfig.setPassword(config.getString("password"));
            hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
            hikariConfig.setMaximumPoolSize(20);

            this.dataSource = new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error setting up the datasource: " + e.getMessage(), e);
            Bukkit.shutdown();
        }
    }

    /**
     * Sets up the hibernate session factory.
     */
    private void setupHibernate() {
        Configuration configuration = new Configuration();

        Properties settings = new Properties();
        settings.put("hibernate.connection.url", this.dataSource.getJdbcUrl());
        settings.put("hibernate.connection.username", this.dataSource.getUsername());
        settings.put("hibernate.connection.password", this.dataSource.getPassword());
        settings.put("hibernate.show_sql", "true");
        settings.put("hibernate.format_sql", "true");
        settings.put("hibernate.hbm2ddl.auto", "update");
        settings.put("hibernate.hikari.dataSource", this.dataSource);

        configuration.setProperties(settings);

        getMappedClasses().forEach(configuration::addAnnotatedClass);

        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties()).build();

        sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    }

    /**
     * Gets the mapped classes for hibernate.
     *
     * @return The mapped classes.
     */
    private List<Class<?>> getMappedClasses() {
        return List.of(
                QuestModel.class,
                QuestRewardModel.class,
                QuestUserProgressModel.class,
                AbstractQuestRequirement.class,
                QuestKillRequirement.class,
                QuestCollectRequirement.class,
                PlayerStatisticsModel.class,
                QuestUserModel.class
        );
    }

    /**
     * Gets a connection from the datasource.
     *
     * @return The connection or null if an error occurred.
     */
    public @Nullable Connection getConnection() {
        if (this.dataSource == null) {
            this.plugin.getLogger().severe("Datasource is not initialized!");
            return null;
        }

        try {
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Failed to get connection from datasource: " + e.getMessage(), e);
            return null;
        }
    }
}