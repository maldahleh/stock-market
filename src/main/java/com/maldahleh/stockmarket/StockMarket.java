package com.maldahleh.stockmarket;

import com.maldahleh.stockmarket.api.StockMarketAPI;
import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.CommandManager;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.Settings;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.placeholder.StockPlaceholderManager;
import com.maldahleh.stockmarket.placeholder.StockPlaceholder;
import com.maldahleh.stockmarket.players.PlayerManager;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
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
  private CommandManager commandManager;

  private Economy econ;

  @Override
  public void onEnable() {
    if (!setupEconomy()) {
      Logger.severe("Vault/economy plugin not found.");
      return;
    }

    Settings settings = new Settings(this);
    Storage storage = Storage.buildStorage(settings);
    Messages messages = new Messages(this, settings);
    StockManager stockManager = new StockManager(settings, messages);
    PlayerManager playerManager = new PlayerManager(this, stockManager, storage, settings);
    this.api = new StockMarketAPI(playerManager);
    PurchaseProcessor purchaseProcessor =
        new PurchaseProcessor(this, stockManager, playerManager, storage, settings, messages);
    SaleProcessor saleProcessor =
        new SaleProcessor(this, stockManager, playerManager, storage, settings, messages);
    InventoryManager inventoryManager =
        new InventoryManager(
            this,
            playerManager,
            stockManager,
            purchaseProcessor,
            saleProcessor,
            messages,
            storage,
            settings);
    BrokerManager brokerManager =
        new BrokerManager(this, settings.getBrokerSettings(), inventoryManager);

    this.commandManager =
        new CommandManager(this, brokerManager, inventoryManager, purchaseProcessor, saleProcessor,
            messages);

    if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
      StockPlaceholderManager stockPlaceholderManager = new StockPlaceholderManager(this,
          stockManager, settings);
      new StockPlaceholder(settings, playerManager, stockPlaceholderManager).register();
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
