package io.github.rysefoxx;

import io.github.rysefoxx.command.CommandCoins;
import io.github.rysefoxx.command.CommandQuest;
import io.github.rysefoxx.command.CommandQuestReward;
import io.github.rysefoxx.database.ConnectionManager;
import io.github.rysefoxx.database.DatabaseTableManager;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.listener.ConnectionListener;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.reward.QuestRewardService;
import io.github.rysefoxx.stats.PlayerStatisticsService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
public class PlayLegendQuest extends JavaPlugin {

    private static Logger logger;

    private ConnectionManager connectionManager;
    private LanguageService languageService;

    private QuestRewardService questRewardService;
    private QuestService questService;
    private QuestUserProgressService questUserProgressService;
    private PlayerStatisticsService playerStatisticsService;

    @Override
    public void onEnable() {
        logger = getLogger();

        initializeManagers();
        initializeCommands();
        initializeListeners();
    }


    @Override
    public void onDisable() {
        this.connectionManager.closeConnection();
    }

    private void initializeManagers() {
        this.connectionManager = new ConnectionManager(this);
        new DatabaseTableManager(this, this.connectionManager);

        this.languageService = new LanguageService(this);

        this.playerStatisticsService = new PlayerStatisticsService();
        this.questRewardService = new QuestRewardService();
        this.questService = new QuestService();
        this.questUserProgressService = new QuestUserProgressService();
    }

    private void initializeCommands() {
        Objects.requireNonNull(getCommand("questreward")).setExecutor(new CommandQuestReward(this.languageService, this.questRewardService));
        Objects.requireNonNull(getCommand("coins")).setExecutor(new CommandCoins(this.languageService, this.playerStatisticsService));
        Objects.requireNonNull(getCommand("quest")).setExecutor(new CommandQuest(this.questService, this.questRewardService, this.questUserProgressService, this.languageService));
    }

    private void initializeListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ConnectionListener(this.questUserProgressService, this.languageService), this);
    }

    public static Logger getLog() {
        return logger;
    }
}