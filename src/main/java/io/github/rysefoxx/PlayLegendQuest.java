package io.github.rysefoxx;

import io.github.rysefoxx.command.CommandCoins;
import io.github.rysefoxx.command.CommandQuest;
import io.github.rysefoxx.command.CommandQuestReward;
import io.github.rysefoxx.command.tabcomplete.TabCompleteQuest;
import io.github.rysefoxx.command.tabcomplete.TabCompleteQuestReward;
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
import io.github.rysefoxx.user.QuestUserService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * @author Rysefoxx
 * @since 16.05.2024
 */
@Getter
@NoArgsConstructor
public class PlayLegendQuest extends JavaPlugin {

    private static Logger logger;
    @Getter
    private static boolean unitTest;

    private ConnectionService connectionService;
    private LanguageService languageService;

    private QuestRewardService questRewardService;
    private QuestService questService;
    private ScoreboardService scoreboardService;
    private QuestRequirementService questRequirementService;
    private QuestUserProgressService questUserProgressService;
    private QuestUserService questUserService;
    private PlayerStatisticsService playerStatisticsService;

    public static Logger getLog() {
        return logger;
    }

    @Override
    public void onEnable() {
        logger = getLogger();

        initializeManagers();
        initializeCommands();
        initializeTabCompleter();
        initializeListeners();
    }

    @Override
    public void onDisable() {
        this.connectionService.closeConnection();
    }


    /**
     * Constructor for unit tests.
     *
     * @param loader      The plugin loader.
     * @param description The plugin description.
     * @param dataFolder  The data folder.
     * @param file        The plugin file.
     */
    @SuppressWarnings("all")
    protected PlayLegendQuest(@NotNull JavaPluginLoader loader, @NotNull PluginDescriptionFile description, @NotNull File dataFolder, @NotNull File file) {
        super(loader, description, dataFolder, file);
        unitTest = true;
    }

    private void initializeManagers() {
        this.connectionService = new ConnectionService(this);
        new DatabaseTableService(this, this.connectionService);

        this.languageService = new LanguageService(this);

        this.playerStatisticsService = new PlayerStatisticsService();
        this.questRewardService = new QuestRewardService(this);
        this.questService = new QuestService();
        this.questUserProgressService = new QuestUserProgressService();
        this.questRequirementService = new QuestRequirementService(this);
        this.scoreboardService = new ScoreboardService(this.questUserProgressService, this.languageService);
        this.questUserService = new QuestUserService(this, this.questUserProgressService, this.languageService, this.scoreboardService, this.questService);
    }

    private void initializeCommands() {
        Objects.requireNonNull(getCommand("questreward")).setExecutor(new CommandQuestReward(this.languageService, this.questRewardService));
        Objects.requireNonNull(getCommand("coins")).setExecutor(new CommandCoins(this.languageService, this.playerStatisticsService));
        Objects.requireNonNull(getCommand("quest")).setExecutor(new CommandQuest(this, this.questService, this.questRewardService, this.questUserProgressService, this.questRequirementService, this.questUserService, this.scoreboardService, this.languageService));
    }

    private void initializeTabCompleter() {
        Objects.requireNonNull(getCommand("quest")).setTabCompleter(new TabCompleteQuest());
        Objects.requireNonNull(getCommand("questreward")).setTabCompleter(new TabCompleteQuestReward());
    }

    private void initializeListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new ConnectionListener(this.questUserProgressService, this.scoreboardService, this.languageService), this);
        pluginManager.registerEvents(new SignChangeListener(this.questUserProgressService, this.languageService), this);
    }
}