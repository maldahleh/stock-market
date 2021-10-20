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
    registerSubcommand(new HelpCommand(messages));
    registerSubcommand(new BuyCommand(stockProcessor, messages));
    registerSubcommand(new SellCommand(stockProcessor, messages));
    registerSubcommand(new TutorialCommand(inventoryManager));
    registerSubcommand(new TransactionsCommand(plugin, inventoryManager, messages));
    registerSubcommand(new PortfolioCommand(plugin, inventoryManager, messages));
    registerSubcommand(new LookupCommand(inventoryManager, messages));
    registerSubcommand(new ListCommand(inventoryManager));
    registerSubcommand(new HistoryCommand(plugin, inventoryManager, messages));
    registerSubcommand(new CompareCommand(inventoryManager, messages));
    registerSubcommand(new SpawnSimpleBrokerCommand(brokerManager, messages));

    PluginCommand pluginCommand = Bukkit.getPluginCommand(ROOT_COMMAND);
    if (pluginCommand == null) {
      Bukkit.getLogger().severe("StockMarket - failed to find command");
      return;
    }

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

  private void registerSubcommand(Subcommand subcommand) {
    subcommandMap.put(subcommand.commandName(), subcommand);
  }
}
