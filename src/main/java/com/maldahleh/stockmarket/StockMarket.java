package com.maldahleh.stockmarket;

import com.maldahleh.stockmarket.commands.StockMarketCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.stocks.StockManager;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class StockMarket extends JavaPlugin {
  @Getter private Economy econ;

  @Override
  public void onEnable() {
    if (!setupEconomy()) {
      getLogger().severe("Vault/economy plugin not found.");
      return;
    }

    saveDefaultConfig();
    StockManager stockManager = new StockManager(getConfig().getConfigurationSection("stocks"));
    Messages messages = new Messages(getConfig().getConfigurationSection("messages"));
    Settings settings = new Settings(getConfig().getConfigurationSection("settings"));
    InventoryManager inventoryManager = new InventoryManager(this, stockManager,
        getConfig(), messages, settings);

    getCommand("stockmarket").setExecutor(new StockMarketCommand(inventoryManager, messages));
  }

  private boolean setupEconomy() {
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
      return false;
    }

    RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager()
        .getRegistration(Economy.class);
    if (rsp == null) {
      return false;
    }

    econ = rsp.getProvider();
    return true;
  }
}