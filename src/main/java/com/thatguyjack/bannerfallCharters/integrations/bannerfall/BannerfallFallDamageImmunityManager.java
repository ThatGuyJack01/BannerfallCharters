package com.thatguyjack.bannerfallCharters.integrations.bannerfall;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pow.bannerfall.managers.ShootingStarManager;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.UUID;

public final class BannerfallFallDamageImmunityManager {
    private final BannerfallCharters plugin;
    private final Set<UUID> fallDamageImmune;

    @SuppressWarnings("unchecked")
    public BannerfallFallDamageImmunityManager(BannerfallCharters plugin) {
        this.plugin = plugin;

        try {
            ShootingStarManager shootingStarManager = plugin.bannerfall().getShootingStarManager();

            Field field = ShootingStarManager.class.getDeclaredField("fallDamageImmune");
            field.setAccessible(true);

            this.fallDamageImmune = (Set<UUID>) field.get(shootingStarManager);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not access Bannerfall ShootingStar fall damage immunity.", exception);
        }
    }

    public void grantUntilGround(Player player) {
        grantUntilGround(player, 5, 20 * 15);
    }

    public void grantUntilGround(Player player, int minimumTicks, int maxTicks) {
        UUID playerId = player.getUniqueId();

        fallDamageImmune.add(playerId);

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    fallDamageImmune.remove(playerId);
                    cancel();
                    return;
                }

                if (ticks > minimumTicks && ((Entity) player).isOnGround()) {
                    fallDamageImmune.remove(playerId);
                    cancel();
                    return;
                }

                if (ticks > maxTicks) {
                    fallDamageImmune.remove(playerId);
                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void grantBriefly(Player player, int ticks) {
        UUID playerId = player.getUniqueId();

        fallDamageImmune.add(playerId);

        new BukkitRunnable() {
            @Override
            public void run() {
                fallDamageImmune.remove(playerId);
            }
        }.runTaskLater(plugin, ticks);
    }

    public void remove(Player player) {
        fallDamageImmune.remove(player.getUniqueId());
    }
}
