package com.thatguyjack.bannerfallCharters.abilities;

import org.bukkit.entity.Player;

public interface CommandCharterAbility extends CharterAbility {
    AbilitySlot slot();

    int cooldownSeconds();

    boolean activate(Player player);
}
