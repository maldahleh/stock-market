package com.maldahleh.stockmarket;

import com.maldahleh.stockmarket.api.StockMarketAPI;
import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.placeholder.StocksPlaceholder;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.stocks.StockManager;
import com.maldahleh.stockmarket.storage.Storage;
import com.maldahleh.stockmarket.utils.Logger;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class StockMarket extends JavaPlugin {

  private StockMarketAPI api;
  private Economy econ;

  private CommandManager commandManager;
  private StockManager stockManager;
  private PlayerManager playerManager;

  @Override
  public void onEnable() {
    if (!setupEconomy()) {
      Logger.severe("Vault/economy plugin not found.");
      return;
    }

    Settings settings = new Settings(this);
    Storage storage = Storage.buildStorage(settings);
    Messages messages = new Messages(this, settings);
    this.stockManager =
        new StockManager(this, getConfig().getConfigurationSection("stocks"), settings);
    this.playerManager = new PlayerManager(this, stockManager, storage, settings);
    this.api = new StockMarketAPI(playerManager);
    StockProcessor stockProcessor =
        new StockProcessor(this, stockManager, playerManager, storage, settings, messages);
    InventoryManager inventoryManager =
        new InventoryManager(
            this,
            playerManager,
            stockManager,
            stockProcessor,
            getConfig(),
            messages,
            storage,
            settings);
    BrokerManager brokerManager =
        new BrokerManager(this, getConfig().getConfigurationSection("brokers"), inventoryManager);

    this.commandManager =
        new CommandManager(this, brokerManager, inventoryManager, stockProcessor, messages);

    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      new StocksPlaceholder(playerManager, stockManager).register();
    }
  }

  private boolean setupEconomy() {
    if (getServer().getPluginManager().getPlugin("Vault") == null) {
      return false;
    }

    RegisteredServiceProvider<Economy> rsp =
        getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
      return false;
    }

    econ = rsp.getProvider();
    return true;
  }
}
