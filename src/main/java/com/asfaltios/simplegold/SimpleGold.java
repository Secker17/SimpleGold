package com.asfaltios.simplegold;

import org.bukkit.plugin.java.JavaPlugin;

public class SimpleGold extends JavaPlugin {

    private static SimpleGold instance;
    private EconomyManager economyManager;
    private GoldCommand goldCommand;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        economyManager = new EconomyManager();
        economyManager.loadData();
        goldCommand = new GoldCommand(economyManager);

        if (getCommand("gold") != null) {
            getCommand("gold").setExecutor(goldCommand);
        } else {
            getLogger().severe("Command 'gold' not found in plugin.yml!");
        }


        // Register event listeners
        getServer().getPluginManager().registerEvents(goldCommand, this);

        getLogger().info("SimpleGold has been enabled.");
    }

    @Override
    public void onDisable() {
        economyManager.saveData();
        getLogger().info("SimpleGold has been disabled.");
    }

    public static SimpleGold getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
