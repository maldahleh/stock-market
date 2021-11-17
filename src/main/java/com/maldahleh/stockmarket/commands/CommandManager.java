package com.maldahleh.stockmarket.commands;

import com.maldahleh.stockmarket.brokers.BrokerManager;
import com.maldahleh.stockmarket.commands.impl.StockMarketCommand;
import com.maldahleh.stockmarket.commands.impl.StockMarketTabCompleter;
import com.maldahleh.stockmarket.commands.subcommands.Subcommand;
import com.maldahleh.stockmarket.commands.subcommands.types.HelpCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.broker.SpawnSimpleBrokerCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.CompareCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.HistoryCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.ListCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.LookupCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.PortfolioCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.TransactionsCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.menus.TutorialCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.transactions.BuyCommand;
import com.maldahleh.stockmarket.commands.subcommands.types.transactions.SellCommand;
import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.inventories.InventoryManager;
import com.maldahleh.stockmarket.processor.types.PurchaseProcessor;
import com.maldahleh.stockmarket.processor.types.SaleProcessor;
import com.maldahleh.stockmarket.utils.Logger;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CommandManager {

  private static final String DEFAULT_PERM = "stockmarket.use";
  public static final String COMMAND_BYPASS_PERM = "stockmarket.commandbypass";

  private static final String ROOT_COMMAND = "stockmarket";

  private final Map<String, Subcommand> subcommandMap = new LinkedHashMap<>();

  public CommandManager(Plugin plugin,
      BrokerManager brokerManager,
      InventoryManager inventoryManager,
      PurchaseProcessor purchaseProcessor,
      SaleProcessor saleProcessor,
      Messages messages) {
    PluginCommand pluginCommand = Bukkit.getPluginCommand(ROOT_COMMAND);
    if (pluginCommand == null) {
      Logger.severe("failed to find command");
      return;
    }

    registerSubcommands(
        new HelpCommand(messages),
        new TutorialCommand(inventoryManager),
        new ListCommand(inventoryManager),
        new LookupCommand(inventoryManager, messages),
        new CompareCommand(inventoryManager, messages),
        new PortfolioCommand(plugin, inventoryManager, messages),
        new TransactionsCommand(plugin, inventoryManager, messages),
        new HistoryCommand(inventoryManager, messages),
        new BuyCommand(purchaseProcessor, messages),
        new SellCommand(saleProcessor, messages),
        new SpawnSimpleBrokerCommand(brokerManager, messages)
    );

    pluginCommand.setExecutor(new StockMarketCommand(this, brokerManager, messages));
    pluginCommand.setTabCompleter(new StockMarketTabCompleter(this));
  }

  public static boolean doesNotHaveBasePermission(Player player) {
    return !player.hasPermission(DEFAULT_PERM);
  }

  public Collection<Subcommand> getRegisteredSubcommands() {
    return subcommandMap.values();
  }

  public Subcommand findSubcommand(String subcommand) {
    String lowercaseSubcommand = subcommand.toLowerCase();
    return subcommandMap.get(lowercaseSubcommand);
  }

  private void registerSubcommands(Subcommand... subcommands) {
    for (Subcommand subcommand : subcommands) {
      subcommandMap.put(subcommand.commandName(), subcommand);
    }
  }
}
