package com.thatguyjack.bannerfallCharters.charters.test;

import com.thatguyjack.bannerfallCharters.core.AbilitySlot;
import com.thatguyjack.bannerfallCharters.core.CommandCharterAbility;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class TestPoofAbility implements CommandCharterAbility {

    @Override
    public AbilitySlot slot() {
        return AbilitySlot.MAIN;
    }

    @Override
    public String id() {
        return "poof";
    }

    @Override
    public String displayName() {
        return "Poof";
    }

    @Override
    public int cooldownSeconds() {
        return 5;
    }

    @Override
    public boolean activate(Player player) {
        player.getWorld().spawnParticle(
                Particle.CLOUD,
                player.getLocation().add(0, 1, 0),
                40,
                0.45,
                0.45,
                0.45,
                0.02
        );
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1.0f, 1.2f);
        return true;
    }
}
