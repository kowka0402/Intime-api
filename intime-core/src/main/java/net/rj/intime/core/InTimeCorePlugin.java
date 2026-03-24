package net.rj.intime.core;

import net.rj.intime.api.InTimeAPI;
import net.rj.intime.core.api.InTimeAPIImpl;
import net.rj.intime.core.command.InTimeCommand;
import net.rj.intime.core.database.SQLiteManager;
import net.rj.intime.core.listener.PlayerJoinListener;
import net.rj.intime.core.listener.PreLoginListener;
import net.rj.intime.core.placeholder.InTimePlaceholderExpansion;
import net.rj.intime.core.service.TimeService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class InTimeCorePlugin extends JavaPlugin {

    private SQLiteManager sqliteManager;
    private TimeService timeService;
    private InTimeAPI api;
    private InTimePlaceholderExpansion placeholderExpansion;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.sqliteManager = new SQLiteManager(this);
        this.sqliteManager.initialize();

        this.timeService = new TimeService(this, sqliteManager);
        this.timeService.loadAllFromDatabase();

        this.api = new InTimeAPIImpl(timeService);
        getServer().getServicesManager().register(InTimeAPI.class, api, this, ServicePriority.Normal);

        registerCommands();
        registerListeners();
        registerPlaceholder();

        this.timeService.startTickTask();
        this.timeService.startAutoSaveTask();

        getLogger().info("InTimeCore enabled.");
    }

    @Override
    public void onDisable() {
        if (this.timeService != null) {
            this.timeService.shutdown();
        }

        if (this.placeholderExpansion != null) {
            this.placeholderExpansion.unregister();
        }

        getServer().getServicesManager().unregisterAll(this);

        if (this.sqliteManager != null) {
            this.sqliteManager.close();
        }

        getLogger().info("InTimeCore disabled.");
    }

    private void registerCommands() {
        PluginCommand command = getCommand("intime");
        if (command == null) {
            throw new IllegalStateException("Command 'intime' is not defined in plugin.yml");
        }

        InTimeCommand executor = new InTimeCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PreLoginListener(this), this);
    }

    private void registerPlaceholder() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI not found. Placeholder support disabled.");
            return;
        }

        this.placeholderExpansion = new InTimePlaceholderExpansion(this);
        this.placeholderExpansion.register();
        getLogger().info("PlaceholderAPI hooked.");
    }

    public SQLiteManager getSqliteManager() {
        return sqliteManager;
    }

    public TimeService getTimeService() {
        return timeService;
    }

    public InTimeAPI getApi() {
        return api;
    }
}
