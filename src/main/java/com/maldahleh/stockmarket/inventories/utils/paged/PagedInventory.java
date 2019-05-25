package com.maldahleh.stockmarket.inventories.utils.paged;

import com.maldahleh.stockmarket.inventories.utils.paged.listeners.PagedInventoryListener;
import com.maldahleh.stockmarket.inventories.utils.paged.provider.IContentProvider;
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

public class PagedInventory<K, V, T> {
  private final Plugin plugin;

  private final IContentProvider<K, V, T> contentProvider;
  private final Map<UUID, Integer> currentPageMap;

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

  private final String noContentMessage;

  public PagedInventory(Plugin plugin, IContentProvider<K, V, T> provider,
      ConfigurationSection section) {
    this.plugin = plugin;

    this.contentProvider = provider;
    this.currentPageMap = new HashMap<>();

    this.inventoryName = Utils.color(section.getString("name"));
    this.inventorySize = section.getInt("size");
    this.baseItem = Utils.createItemStack(section.getConfigurationSection("base-item"));

    this.previousPageSlot = section.getInt("previous-page.slot");
    this.previousPageStack = Utils.createItemStack(section
        .getConfigurationSection("previous-page.previous-page"));
    this.noPreviousPageStack = Utils.createItemStack(section
        .getConfigurationSection("previous-page.no-previous"));

    this.nextPageSlot = section.getInt("next-page.slot");
    this.nextPageStack = Utils.createItemStack(section
        .getConfigurationSection("next-page.next-page"));
    this.noNextPageStack = Utils.createItemStack(section
        .getConfigurationSection("next-page.no-next"));

    this.contentSlots = section.getIntegerList("content-slots");
    this.contentPerPage = contentSlots.size();

    this.noContentMessage = Utils.color(section.getString("messages.no-content"));

    Bukkit.getPluginManager().registerEvents(new PagedInventoryListener(this), plugin);
  }

  public void displayInventory(Player player) {
    displayInventory(player, 1);
  }

  private void displayInventory(Player player, int page) {
    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
      Map<K, V> data = contentProvider.getContent(player.getUniqueId());
      if (data == null || data.isEmpty()) {
        player.sendMessage(noContentMessage);
        return;
      }

      Map<T, V> transformedData = contentProvider.applyTransformations(data);
      Bukkit.getScheduler().runTask(plugin, () -> {
        Inventory i = Bukkit.createInventory(null, inventorySize, inventoryName);

        int currentPage = page;
        int currentIndex = 0;
        int totalDisplayed = 0;
        for (Map.Entry<T, V> e : transformedData.entrySet()) {
          int position = ((currentPage - 1) * contentPerPage) + (currentIndex + 1);
          i.setItem(contentSlots.get(currentIndex), contentProvider.getContentStack(baseItem,
              position, e.getKey(), e.getValue()));
          totalDisplayed++;

          if ((currentIndex + 1) == contentPerPage || totalDisplayed == transformedData.size()) {
            i.setItem(previousPageSlot, currentPage == 1 ? noPreviousPageStack : previousPageStack);
            if (transformedData.size() > contentPerPage && currentPage == page) {
              i.setItem(nextPageSlot, nextPageStack);
            } else {
              i.setItem(nextPageSlot, noNextPageStack);
            }

            if (currentPage == page) {
              player.openInventory(i);
              currentPageMap.put(player.getUniqueId(), page);
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
    if (!(e.getWhoClicked() instanceof Player)
        || e.getClickedInventory() == null) {
      return;
    }

    Player clicker = (Player) e.getWhoClicked();
    Integer page = currentPageMap.get(clicker.getUniqueId());
    if (page == null) {
      return;
    }

    e.setCancelled(true);
    if (e.getRawSlot() >= inventorySize) {
      return;
    }

    ItemStack selectedStack = e.getClickedInventory().getItem(e.getSlot());
    if (selectedStack == null || selectedStack.isSimilar(noNextPageStack)
        || selectedStack.isSimilar(noPreviousPageStack)) {
      return;
    }

    if (e.getSlot() == previousPageSlot) {
      displayInventory(clicker, page - 1);
    } else if (e.getSlot() == nextPageSlot) {
      displayInventory(clicker, page + 1);
    }
  }

  public boolean hasInventory(HumanEntity entity) {
    return currentPageMap.containsKey(entity.getUniqueId());
  }

  public void remove(HumanEntity entity) {
    currentPageMap.remove(entity.getUniqueId());
  }
}
