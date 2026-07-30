package com.thatguyjack.bannerfallCharters.abilities;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.charters.Charter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public final class CharterPassiveTicker extends BukkitRunnable {
    private final BannerfallCharters plugin;

    public CharterPassiveTicker(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String charterId = plugin.charterManager()
                    .getCharter(player.getUniqueId())
                    .orElse(null);

            if (charterId == null) {
                continue;
            }

            Charter charter = plugin.charterRegistry()
                    .getCharter(charterId)
                    .orElse(null);

            if (charter == null) {
                continue;
            }

            for (CharterAbility ability : charter.abilities()) {
                if (ability instanceof TickingCharterAbility tickingAbility) {
                    tickingAbility.tick(player);
                }
            }
        }
    }
}
