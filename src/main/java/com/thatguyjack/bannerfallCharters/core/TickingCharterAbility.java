package com.thatguyjack.bannerfallCharters.core;

import org.bukkit.entity.Player;

public interface TickingCharterAbility extends CharterAbility {
    void tick(Player player);
}
