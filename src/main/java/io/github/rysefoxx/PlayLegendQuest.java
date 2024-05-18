package io.github.rysefoxx;

import io.github.rysefoxx.command.CommandCoins;
import io.github.rysefoxx.command.CommandQuest;
import io.github.rysefoxx.command.CommandQuestReward;
import io.github.rysefoxx.database.ConnectionService;
import io.github.rysefoxx.database.DatabaseTableService;
import io.github.rysefoxx.language.LanguageService;
import io.github.rysefoxx.listener.ConnectionListener;
import io.github.rysefoxx.listener.SignChangeListener;
import io.github.rysefoxx.progress.QuestUserProgressService;
import io.github.rysefoxx.quest.QuestRequirementService;
import io.github.rysefoxx.quest.QuestService;
import io.github.rysefoxx.reward.QuestRewardService;
import io.github.rysefoxx.scoreboard.ScoreboardService;
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

    private ConnectionService connectionService;
    private LanguageService languageService;

    private QuestRewardService questRewardService;
    private QuestService questService;
    private ScoreboardService scoreboardService;
    private QuestRequirementService questRequirementService;
    private QuestUserProgressService questUserProgressService;
    private PlayerStatisticsService playerStatisticsService;

    public static Logger getLog() {
        return logger;
    }

    @Override
    public void onEnable() {
        logger = getLogger();

        initializeManagers();
        initializeCommands();
        initializeListeners();
    }

    @Override
    public void onDisable() {
        this.connectionService.closeConnection();
    }

    private void initializeManagers() {
        this.connectionService = new ConnectionService(this);
        new DatabaseTableService(this, this.connectionService);

        this.languageService = new LanguageService(this);

        this.playerStatisticsService = new PlayerStatisticsService();
        this.questRewardService = new QuestRewardService();
        this.questService = new QuestService();
        this.questUserProgressService = new QuestUserProgressService();
        this.questRequirementService = new QuestRequirementService();
        this.scoreboardService = new ScoreboardService(this.questUserProgressService, this.languageService);
    }

    private void initializeCommands() {
        Objects.requireNonNull(getCommand("questreward")).setExecutor(new CommandQuestReward(this.languageService, this.questRewardService));
        Objects.requireNonNull(getCommand("coins")).setExecutor(new CommandCoins(this.languageService, this.playerStatisticsService));
        Objects.requireNonNull(getCommand("quest")).setExecutor(new CommandQuest(this.questService, this.questRewardService, this.questUserProgressService, this.questRequirementService, this.scoreboardService, this.languageService));
    }

    private void initializeListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ConnectionListener(this.questUserProgressService, this.scoreboardService, this.languageService), this);
        pluginManager.registerEvents(new SignChangeListener(this.questUserProgressService, this.languageService), this);
    }
}