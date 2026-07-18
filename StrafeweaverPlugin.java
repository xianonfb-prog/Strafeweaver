package com.strafeweaver;

import org.bukkit.plugin.java.JavaPlugin;

public class StrafeweaverPlugin extends JavaPlugin {

    private static StrafeweaverPlugin instance;
    private StrafeweaverManager manager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize the manager (handles item creation and unique tracking)
        manager = new StrafeweaverManager(this);
        
        // Register Events and Commands
        getServer().getPluginManager().registerEvents(new StrafeweaverListener(this, manager), this);
        getCommand("strafeweaver").setExecutor(new StrafeweaverCommand(this, manager));
        
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
