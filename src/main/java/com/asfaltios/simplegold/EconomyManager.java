package com.asfaltios.simplegold;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EconomyManager {

    private Map<UUID, Double> balances;
    private Map<UUID, List<String>> transactionHistory;
    private File dataFile;
    private FileConfiguration dataConfig;

    public EconomyManager() {
        balances = new HashMap<>();
        transactionHistory = new HashMap<>();
        dataFile = new File(SimpleGold.getInstance().getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    // Balance Methods
    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, 0.0);
    }

    public void setBalance(UUID uuid, double amount) {
        balances.put(uuid, amount);
    }

    public void addBalance(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public void subtractBalance(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) - amount);
    }

    public boolean hasBalance(UUID uuid) {
        return balances.containsKey(uuid);
    }

    // Transaction History Methods
    public void logTransaction(UUID uuid, String message) {
        transactionHistory.computeIfAbsent(uuid, k -> new ArrayList<>()).add(message);
    }

    public List<String> getTransactionHistory(UUID uuid) {
        return transactionHistory.getOrDefault(uuid, new ArrayList<>());
    }

    // Data Persistence Methods
    public void saveData() {
        // Save balances
        for (UUID uuid : balances.keySet()) {
            dataConfig.set("balances." + uuid.toString(), balances.get(uuid));
        }

        // Save transaction history
        for (UUID uuid : transactionHistory.keySet()) {
            dataConfig.set("transactions." + uuid.toString(), transactionHistory.get(uuid));
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadData() {
        if (!dataFile.exists()) {
            return;
        }

        // Load balances
        if (dataConfig.contains("balances")) {
            for (String key : dataConfig.getConfigurationSection("balances").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                double balance = dataConfig.getDouble("balances." + key);
                balances.put(uuid, balance);
            }
        }

        // Load transaction history
        if (dataConfig.contains("transactions")) {
            for (String key : dataConfig.getConfigurationSection("transactions").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                List<String> history = dataConfig.getStringList("transactions." + key);
                transactionHistory.put(uuid, history);
            }
        }
    }

    // Config Access Methods
    public double getGoldValuePerIngot() {
        return SimpleGold.getInstance().getConfig().getDouble("gold-value-per-ingot", 100.0);
    }

    public boolean isAfkEarningsEnabled() {
        return SimpleGold.getInstance().getConfig().getBoolean("afk-earnings-enabled", false);
    }
}
