package com.maldahleh.stockmarket.inventories.utils.paged;

import com.maldahleh.stockmarket.inventories.utils.paged.data.PaginatedPlayer;
import com.maldahleh.stockmarket.inventories.utils.paged.listeners.PagedInventoryListener;
import com.maldahleh.stockmarket.inventories.utils.paged.provider.ContentProvider;
import com.maldahleh.stockmarket.utils.Utils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class PagedInventory<L, K, V, T, TV> {

  private final Plugin plugin;

  private final ContentProvider<L, K, V, T, TV> contentProvider;
  private final Map<UUID, PaginatedPlayer> playerMap;

  private final String inventoryName;
  private final int inventorySize;
  private final ItemStack baseItem;

  private final int previousPageSlot;
  private final ItemStack previousPageStack;
  private final ItemStack noPreviousPageStack;

  private final int nextPageSlot;
  private final ItemStack nextPageStack;
  private final ItemStack noNextPageStack;

  private final List<Integer> contentSlots;
  private final int contentPerPage;

  private final Map<Integer, ItemStack> extraItems;

  private final String noContentMessage;

  public PagedInventory(
      Plugin plugin, ContentProvider<L, K, V, T, TV> provider, ConfigurationSection section) {
    this.plugin = plugin;

    this.contentProvider = provider;
    this.playerMap = new HashMap<>();

    this.inventoryName = Utils.color(section.getString("name"));
    this.inventorySize = section.getInt("size");
    this.baseItem = Utils.createItemStack(section.getConfigurationSection("base-item"));

    this.previousPageSlot = section.getInt("previous-page.slot");
    this.previousPageStack =
        Utils.createItemStack(section.getConfigurationSection("previous-page.previous-page"));
    this.noPreviousPageStack =
        Utils.createItemStack(section.getConfigurationSection("previous-page.no-previous"));

    this.nextPageSlot = section.getInt("next-page.slot");
    this.nextPageStack =
        Utils.createItemStack(section.getConfigurationSection("next-page.next-page"));
    this.noNextPageStack =
        Utils.createItemStack(section.getConfigurationSection("next-page.no-next"));

    this.contentSlots = section.getIntegerList("content-slots");
    this.contentPerPage = contentSlots.size();

    this.extraItems = new HashMap<>();

    ConfigurationSection extraSection = section.getConfigurationSection("extra-items");
    if (extraSection != null) {
      for (String key : section.getConfigurationSection("extra-items").getKeys(false)) {
        extraItems.put(
            Integer.valueOf(key),
            Utils.createItemStack(section.getConfigurationSection("extra-items." + key)));
      }
    }

    this.noContentMessage = Utils.color(section.getString("messages.no-content"));

    Bukkit.getPluginManager().registerEvents(new PagedInventoryListener(this), plugin);
  }

  public void displayInventory(Player player, L target) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              Map<K, V> data = contentProvider.getContent(target);
              if (data == null || data.isEmpty()) {
                player.sendMessage(noContentMessage);
                return;
              }

              Map<T, TV> transformedData = contentProvider.applyTransformations(data);
              if (transformedData.isEmpty()) {
                player.sendMessage(noContentMessage);
                return;
              }

              Map<String, Object> extraData = contentProvider.getExtraData(target);
              Bukkit.getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        PaginatedPlayer paginatedPlayer = new PaginatedPlayer();

                        int currentPage = 1;
                        int currentIndex = 0;
                        int totalDisplayed = 0;
                        Inventory i = Bukkit.createInventory(null, inventorySize, inventoryName);
                        for (Map.Entry<T, TV> e : transformedData.entrySet()) {
                          int position = ((currentPage - 1) * contentPerPage) + (currentIndex + 1);
                          i.setItem(
                              contentSlots.get(currentIndex),
                              contentProvider.getContentStack(
                                  baseItem, position, e.getKey(), e.getValue()));
                          totalDisplayed++;

                          if ((currentIndex + 1) == contentPerPage
                              || totalDisplayed == transformedData.size()) {
                            i.setItem(
                                previousPageSlot,
                                currentPage == 1 ? noPreviousPageStack : previousPageStack);
                            i.setItem(
                                nextPageSlot,
                                totalDisplayed < transformedData.size()
                                    ? nextPageStack
                                    : noNextPageStack);

                            for (Map.Entry<Integer, ItemStack> extraEntry : extraItems.entrySet()) {
                              i.setItem(
                                  extraEntry.getKey(),
                                  contentProvider.getExtraItem(extraEntry.getValue(), extraData));
                            }

                            paginatedPlayer.addInventory(currentPage, i);
                            if (currentPage == 1) {
                              player.openInventory(i);
                              playerMap.put(player.getUniqueId(), paginatedPlayer);
                            }

                            i = Bukkit.createInventory(null, inventorySize, inventoryName);

                            currentPage++;
                            currentIndex = 0;
                            continue;
                          }

                          currentIndex++;
                        }
                      });
            });
  }

  public void handleClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player clicker) || e.getClickedInventory() == null) {
      return;
    }

    PaginatedPlayer player = playerMap.get(clicker.getUniqueId());
    if (player == null) {
      return;
    }

    e.setCancelled(true);
    if (e.getRawSlot() >= inventorySize) {
      return;
    }

    ItemStack selectedStack = e.getClickedInventory().getItem(e.getSlot());
    if (selectedStack == null
        || selectedStack.isSimilar(noNextPageStack)
        || selectedStack.isSimilar(noPreviousPageStack)) {
      return;
    }

    if (e.getSlot() == previousPageSlot) {
      clicker.openInventory(player.getPreviousPage());
      playerMap.put(clicker.getUniqueId(), player);
    } else if (e.getSlot() == nextPageSlot) {
      clicker.openInventory(player.getNextPage());
      playerMap.put(clicker.getUniqueId(), player);
    }
  }

  public boolean hasInventory(HumanEntity entity) {
    return playerMap.containsKey(entity.getUniqueId());
  }

  public void remove(HumanEntity entity) {
    playerMap.remove(entity.getUniqueId());
  }
}
