package com.maldahleh.stockmarket.inventories.utils.paged;

import com.maldahleh.stockmarket.config.Messages;
import com.maldahleh.stockmarket.config.common.ConfigSection;
import com.maldahleh.stockmarket.inventories.utils.paged.data.PaginatedPlayer;
import com.maldahleh.stockmarket.inventories.utils.paged.listeners.PagedInventoryListener;
import com.maldahleh.stockmarket.inventories.utils.paged.provider.ContentProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class PagedInventory<L, K, V, T, U> {

  private final Plugin plugin;
  private final Messages messages;
  private final ContentProvider<L, K, V, T, U> contentProvider;

  private final Map<UUID, PaginatedPlayer> playerMap = new HashMap<>();

  private final String name;
  private final int size;
  private final ItemStack baseItem;

  private final int previousPageSlot;
  private final ItemStack previousPageStack;
  private final ItemStack noPreviousPageStack;

  private final int nextPageSlot;
  private final ItemStack nextPageStack;
  private final ItemStack noNextPageStack;

  private final List<Integer> contentSlots;
  private final int contentPerPage;

  private final Map<Integer, ItemStack> extraItems = new HashMap<>();

  public PagedInventory(Plugin plugin, Messages messages, ContentProvider<L, K, V, T, U> provider,
      ConfigSection section) {
    this.plugin = plugin;
    this.messages = messages;
    this.contentProvider = provider;

    this.name = section.getString("name");
    this.size = section.getInt("size");
    this.baseItem = section.getItemStack("base-item");

    this.previousPageSlot = section.getInt("previous-page.slot");
    this.previousPageStack = section.getItemStack("previous-page.previous-page");
    this.noPreviousPageStack = section.getItemStack("previous-page.no-previous");

    this.nextPageSlot = section.getInt("next-page.slot");
    this.nextPageStack = section.getItemStack("next-page.next-page");
    this.noNextPageStack = section.getItemStack("next-page.no-next");

    this.contentSlots = section.getIntegerList("content-slots");
    this.contentPerPage = contentSlots.size();

    ConfigSection extraSection = section.getSection("extra-items");
    if (extraSection != null) {
      for (String key : extraSection.getKeys()) {
        extraItems.put(Integer.valueOf(key), extraSection.getItemStack(key));
      }
    }

    Bukkit.getPluginManager().registerEvents(new PagedInventoryListener(this), plugin);
  }

  public void displayInventory(Player player, L target) {
    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              Map<K, V> data = contentProvider.getContent(target);
              if (data.isEmpty()) {
                messages.sendNoContent(player);
                return;
              }

              Map<T, U> transformedData = contentProvider.applyTransformations(data);
              if (transformedData.isEmpty()) {
                messages.sendNoContent(player);
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
                        Inventory i = Bukkit.createInventory(null, size, name);
                        for (Map.Entry<T, U> e : transformedData.entrySet()) {
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

                            i = Bukkit.createInventory(null, size, name);

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
    if (e.getRawSlot() >= size) {
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
