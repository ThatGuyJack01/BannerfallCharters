package com.thatguyjack.bannerfallCharters.listeners;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.core.AbilitySlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;

public final class AbilityCommandListener implements Listener {
    private final BannerfallCharters plugin;

    public AbilityCommandListener(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAbilityCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();

        if(!message.startsWith("/")) {
            return;
        }

        String[] parts = message.substring(1).trim().split("\\s+");

        if (parts.length == 0) {
            return;
        }

        String commandName = parts[0].toLowerCase(Locale.ROOT);

        if (!commandName.equals("ability") && !commandName.endsWith(":ability")) {
            return;
        }

        if (plugin.charterManager().getCharter(event.getPlayer().getUniqueId()).isEmpty()) {
            return;
        }

        event.setCancelled(true);

        AbilitySlot slot = AbilitySlot.MAIN;
        if (parts.length >= 2) {
            switch (parts[1].toLowerCase(Locale.ROOT)) {
                case "1" -> slot = AbilitySlot.ONE;
                case "2" -> slot = AbilitySlot.TWO;
                case "3" -> slot = AbilitySlot.THREE;
                case "4" -> slot = AbilitySlot.FOUR;
                case "break" -> slot = AbilitySlot.BREAK;
                default -> slot = AbilitySlot.MAIN;
            }
        }

        plugin.charterAbilityManager().activate(event.getPlayer(), slot);
    }
}
