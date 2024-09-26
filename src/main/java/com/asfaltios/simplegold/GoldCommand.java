package com.asfaltios.simplegold;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GoldCommand implements CommandExecutor, Listener {

    private EconomyManager economyManager;
    private Map<UUID, String> pendingTransactions;

    public GoldCommand(EconomyManager economyManager) {
        this.economyManager = economyManager;
        this.pendingTransactions = new HashMap<>();
    }

    // Command Handling
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            double balance = economyManager.getBalance(player.getUniqueId());
            player.sendMessage(ChatColor.GOLD + "Your gold balance is: " + ChatColor.YELLOW + balance);
            return true;
        }

        if (args[0].equalsIgnoreCase("gui")) {
            Inventory gui = getInventory(player);
            player.openInventory(gui);
            return true;
        }

        if (args[0].equalsIgnoreCase("pay") && args.length == 3) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            try {
                double amount = Double.parseDouble(args[2]);
                if (economyManager.getBalance(player.getUniqueId()) < amount) {
                    player.sendMessage(ChatColor.RED + "Insufficient funds.");
                    return true;
                }
                economyManager.subtractBalance(player.getUniqueId(), amount);
                economyManager.addBalance(target.getUniqueId(), amount);
                economyManager.logTransaction(player.getUniqueId(), "Paid " + amount + " gold to " + target.getName());
                economyManager.logTransaction(target.getUniqueId(), "Received " + amount + " gold from " + player.getName());
                player.sendMessage(ChatColor.GREEN + "Sent " + amount + " gold to " + target.getName() + ".");
                target.sendMessage(ChatColor.GREEN + "Received " + amount + " gold from " + player.getName() + ".");
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount.");
            }
            return true;
        }

        player.sendMessage(ChatColor.RED + "Usage: /gold [gui|pay <player> <amount>]");
        return true;
    }

    // GUI Creation
    public Inventory getInventory(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.GOLD + "Your Gold Balance");

        // Fill background
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, filler);
        }

        // Display balance
        double balance = economyManager.getBalance(player.getUniqueId());
        ItemStack balanceItem = createItem(Material.GOLD_INGOT, ChatColor.YELLOW + "Your Balance: " + ChatColor.GOLD + balance);
        gui.setItem(13, balanceItem);

        // Deposit button
        ItemStack depositItem = createItem(Material.HOPPER, ChatColor.GREEN + "Deposit", ChatColor.GRAY + "Click to deposit gold ingots");
        gui.setItem(11, depositItem);

        // Withdraw button
        ItemStack withdrawItem = createItem(Material.CHEST, ChatColor.RED + "Withdraw", ChatColor.GRAY + "Click to withdraw gold ingots");
        gui.setItem(15, withdrawItem);

        // Transaction history button
        ItemStack historyItem = createItem(Material.BOOK, ChatColor.AQUA + "Transaction History", ChatColor.GRAY + "Click to view your transactions");
        gui.setItem(22, historyItem);

        return gui;
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // Inventory Click Event
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GOLD + "Your Gold Balance")) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.BLACK_STAINED_GLASS_PANE) {
                return;
            }

            Player player = (Player) event.getWhoClicked();

            switch (event.getCurrentItem().getType()) {
                case HOPPER:
                    player.closeInventory();
                    pendingTransactions.put(player.getUniqueId(), "deposit");
                    player.sendMessage(ChatColor.GREEN + "How many gold ingots would you like to deposit?");
                    break;
                case CHEST:
                    player.closeInventory();
                    pendingTransactions.put(player.getUniqueId(), "withdraw");
                    player.sendMessage(ChatColor.GREEN + "How many gold ingots would you like to withdraw?");
                    break;
                case BOOK:
                    player.closeInventory();
                    List<String> history = economyManager.getTransactionHistory(player.getUniqueId());
                    if (history.isEmpty()) {
                        player.sendMessage(ChatColor.GRAY + "No transactions found.");
                    } else {
                        player.sendMessage(ChatColor.AQUA + "Transaction History:");
                        for (String record : history) {
                            player.sendMessage(ChatColor.GRAY + "- " + record);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    // Player Chat Event
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingTransactions.containsKey(uuid)) {
            event.setCancelled(true);
            String type = pendingTransactions.get(uuid);
            String message = event.getMessage();

            try {
                int amount = Integer.parseInt(message);
                if (amount <= 0) {
                    player.sendMessage(ChatColor.RED + "The amount must be greater than 0.");
                    pendingTransactions.remove(uuid);
                    return;
                }

                if (type.equals("deposit")) {
                    int goldIngotCount = countItems(player, Material.GOLD_INGOT);
                    if (goldIngotCount < amount) {
                        player.sendMessage(ChatColor.RED + "You don't have enough gold ingots to deposit.");
                    } else {
                        removeItems(player, Material.GOLD_INGOT, amount);
                        double totalValue = amount * economyManager.getGoldValuePerIngot();
                        economyManager.addBalance(uuid, totalValue);
                        economyManager.logTransaction(uuid, "Deposited: +" + totalValue + " gold");
                        player.sendMessage(ChatColor.GREEN + "You have deposited " + amount + " gold ingots, worth " + totalValue + " gold.");
                    }
                } else if (type.equals("withdraw")) {
                    double playerBalance = economyManager.getBalance(uuid);
                    double totalValue = amount * economyManager.getGoldValuePerIngot();
                    if (playerBalance < totalValue) {
                        player.sendMessage(ChatColor.RED + "You don't have enough balance to withdraw this amount.");
                    } else {
                        economyManager.subtractBalance(uuid, totalValue);
                        economyManager.logTransaction(uuid, "Withdrew: -" + totalValue + " gold");
                        player.getInventory().addItem(new ItemStack(Material.GOLD_INGOT, amount));
                        player.sendMessage(ChatColor.GREEN + "You have withdrawn " + amount + " gold ingots, worth " + totalValue + " gold.");
                    }
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Please enter a valid number.");
            }

            pendingTransactions.remove(uuid);
        }
    }

    // Player Join Event
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        if (!economyManager.hasBalance(playerUUID)) {
            economyManager.setBalance(playerUUID, 0.0);
        }
    }

    // Utility Methods
    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material material, int amount) {
        int remaining = amount;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    player.getInventory().removeItem(item);
                    remaining -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
                if (remaining == 0) break;
            }
        }
    }
}
