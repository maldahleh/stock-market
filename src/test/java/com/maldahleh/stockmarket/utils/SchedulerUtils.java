package com.maldahleh.stockmarket.utils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import lombok.experimental.UtilityClass;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.mockito.ArgumentCaptor;

@UtilityClass
public class SchedulerUtils {

  public void interceptAsyncRun(Plugin plugin, BukkitScheduler scheduler) {
    ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);

    verify(scheduler)
        .runTaskAsynchronously(eq(plugin), argument.capture());

    argument.getValue().run();
  }
}
