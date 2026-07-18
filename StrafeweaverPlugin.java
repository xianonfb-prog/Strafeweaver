package com.strafeweaver;

import org.bukkit.plugin.java.JavaPlugin;

public class StrafeweaverPlugin extends JavaPlugin {

    private static StrafeweaverPlugin instance;
    private StrafeweaverManager manager;

    @Override
    public void onEnable() {
        instance = this;

        manager = new StrafeweaverManager(this);

        StrafeweaverCommand commandExecutor = new StrafeweaverCommand(this, manager);

        getServer().getPluginManager().registerEvents(new StrafeweaverListener(this, manager), this);
        getCommand("strafeweaver").setExecutor(commandExecutor);
        getCommand("ability").setExecutor(commandExecutor);

        getLogger().info("Strafeweaver Plugin Enabled! The hunt begins...");
    }

    @Override
    public void onDisable() {
        getLogger().info("Strafeweaver Plugin Disabled.");
    }

    public static StrafeweaverPlugin getInstance() {
        return instance;
    }

    public StrafeweaverManager getManager() {
        return manager;
    }
}
