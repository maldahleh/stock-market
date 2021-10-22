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
import com.maldahleh.stockmarket.processor.StockProcessor;
import com.maldahleh.stockmarket.utils.Logger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

public class CommandManager {

  public static final String DEFAULT_PERM = "stockmarket.use";
  public static final String COMMAND_BYPASS_PERM = "stockmarket.commandbypass";

  private static final String ROOT_COMMAND = "stockmarket";

  private final Map<String, Subcommand> subcommandMap = new HashMap<>();

  public CommandManager(
      Plugin plugin,
      BrokerManager brokerManager,
      InventoryManager inventoryManager,
      StockProcessor stockProcessor,
      Messages messages) {
    PluginCommand pluginCommand = Bukkit.getPluginCommand(ROOT_COMMAND);
    if (pluginCommand == null) {
      Logger.severe("failed to find command");
      return;
    }

    registerSubcommands(new HelpCommand(messages),
        new BuyCommand(stockProcessor, messages),
        new SellCommand(stockProcessor, messages),
        new TutorialCommand(inventoryManager),
        new TransactionsCommand(plugin, inventoryManager, messages),
        new PortfolioCommand(plugin, inventoryManager, messages),
        new LookupCommand(inventoryManager, messages),
        new ListCommand(inventoryManager),
        new HistoryCommand(plugin, inventoryManager, messages),
        new CompareCommand(inventoryManager, messages),
        new SpawnSimpleBrokerCommand(brokerManager, messages));

    pluginCommand.setExecutor(new StockMarketCommand(this, brokerManager, messages));
    pluginCommand.setTabCompleter(new StockMarketTabCompleter(this));
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