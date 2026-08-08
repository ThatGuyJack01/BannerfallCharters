package com.thatguyjack.bannerfallCharters.charters.thatguyjack;

import com.thatguyjack.bannerfallCharters.core.AbilitySlot;
import com.thatguyjack.bannerfallCharters.core.CommandCharterAbility;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class ModeToggleAbility implements CommandCharterAbility {
    @Override
    public AbilitySlot slot() {
        return AbilitySlot.MAIN;
    }

    @Override
    public int cooldownSeconds() {
        return 0;
    }

    @Override
    public String id() {
        return "thatguyjack_toggle_mode";
    }

    @Override
    public String displayName() {
        return "Toggle Debug Mode";
    }

    @Override
    public boolean activate(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.CREATIVE);
            player.sendMessage(ChatColor.GRAY + "Debug mode: " + ChatColor.AQUA + "Creative");
        } else {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(ChatColor.GRAY + "Debug mode: " + ChatColor.DARK_PURPLE + "Spectator");
        }

        return true;
    }
}
