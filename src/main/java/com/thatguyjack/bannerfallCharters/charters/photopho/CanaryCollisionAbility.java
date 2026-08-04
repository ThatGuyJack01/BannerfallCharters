package com.thatguyjack.bannerfallCharters.charters.photopho;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.core.AbilitySlot;
import com.thatguyjack.bannerfallCharters.core.CommandCharterAbility;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CanaryCollisionAbility implements CommandCharterAbility {
    private final BannerfallCharters plugin;

    private static final int DASH_TICKS = 10;

    private static final double HIT_RADIUS = 1.35;
    private static final double KNOCKBACK_STRENGTH = 1.25;
    private static final double KNOCKBACK_Y = 0.45;

    private static final double FLAT_HORIZONTAL_SPEED = 3;
    private static final double FLAT_Y_LIFT = 0.34;

    private static final double ANGLED_HORIZONTAL_SPEED = 2.1;
    private static final double ANGLED_VERTICAL_SPEED = 2.0;
    private static final double MAX_VERTICAL_COMPONENT = 0.85;
    private static final double AIRBORNE_HORIZONTAL_MULTIPLIER = 0.65;

    public CanaryCollisionAbility(BannerfallCharters plugin) {
        this.plugin = plugin;
    }


    @Override
    public AbilitySlot slot() {
        return AbilitySlot.MAIN;
    }

    @Override
    public int cooldownSeconds() {
        return 1;
    }

    @Override
    public String id() {
        return "canarycollision";
    }

    @Override
    public String displayName() {
        return "Canary Collision";
    }

    @Override
    public boolean activate(Player player) {
        Vector dash = getDashVelocity(player);

        player.setVelocity(dash);

        player.setNoDamageTicks(DASH_TICKS + 2);

        player.setRiptiding(true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                startSlowFalling(player);
            }
        }, 10L);

        startCollisionHitbox(player, dash);

        return true;
    }

    private Vector getDashVelocity(Player player) {
        float pitch = player.getLocation().getPitch();
        float yaw = player.getLocation().getYaw();

        Vector horizontal = new Vector(
                -Math.sin(Math.toRadians(yaw)),
                0,
                Math.cos(Math.toRadians(yaw))
        ).normalize();

        if (pitch >= -7 && pitch <= 20) {
            boolean grounded = ((Entity) player).isOnGround();

            double horizontalSpeed = grounded
                    ? FLAT_HORIZONTAL_SPEED
                    : FLAT_HORIZONTAL_SPEED * AIRBORNE_HORIZONTAL_MULTIPLIER;

            return horizontal
                    .multiply(horizontalSpeed)
                    .setY(grounded ? FLAT_Y_LIFT : 0.0);
        }

        Vector look = player.getLocation().getDirection();

        double vertical = Math.max(
                -MAX_VERTICAL_COMPONENT,
                Math.min(MAX_VERTICAL_COMPONENT, look.getY())
        );

        boolean grounded = ((Entity) player).isOnGround();

        double horizontalSpeed = grounded
                ? ANGLED_HORIZONTAL_SPEED
                : ANGLED_HORIZONTAL_SPEED * AIRBORNE_HORIZONTAL_MULTIPLIER;

        Vector dash = horizontal.multiply(horizontalSpeed);
        dash.setY(vertical * ANGLED_VERTICAL_SPEED);

        return dash;
    }

    private void startCollisionHitbox(Player player, Vector dash) {
        Set<UUID> hitEntities = new HashSet<>();

        Vector knockbackDirection = dash.clone();

        if(knockbackDirection.lengthSquared() < 0.001) {
            knockbackDirection = player.getLocation().getDirection();
        }

        if (knockbackDirection.lengthSquared() < 0.001) {
            knockbackDirection = new Vector(0, 0, 1);
        }

        knockbackDirection.normalize();

        Vector finalKnockbackDirection = knockbackDirection;
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if(!player.isOnline() || player.isDead()) {
                    player.setRiptiding(false);
                    cancel();
                    return;
                }

                if (ticks > 1 && ((Entity) player).isOnGround()) {
                    player.setRiptiding(false);
                    cancel();
                    return;
                }

                for (Entity entity : player.getNearbyEntities(HIT_RADIUS, HIT_RADIUS, HIT_RADIUS)) {
                    if(!(entity instanceof LivingEntity target)) {
                        continue;
                    }

                    if(target.getUniqueId().equals(player.getUniqueId())) {
                        continue;
                    }

                    if(!hitEntities.add(target.getUniqueId())) {
                        continue;
                    }

                    knockbackTarget(target, finalKnockbackDirection);
                }

                ticks++;

                if(ticks >= DASH_TICKS) {
                    player.setRiptiding(false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void knockbackTarget(LivingEntity target, Vector direction) {
        Vector knockback = direction.clone();

        if(knockback.getY() < 0.1) {
            knockback.setY(0.1);
        }

        knockback.normalize();
        knockback.multiply(KNOCKBACK_STRENGTH);
        knockback.setY(knockback.getY() + KNOCKBACK_Y);

        target.setVelocity(target.getVelocity().add(knockback));


        target.getWorld().spawnParticle(
                Particle.POOF,
                target.getLocation().add(0, 1,0),
                12,
                0.25,
                0.25,
                0.25,
                0.03
        );

        target.getWorld().playSound(
                target.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                0.8f,
                1.15f
        );
    }

    private void startSlowFalling(Player player) {
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                Integer.MAX_VALUE,
                0,
                false,
                false,
                true
        ));

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if(!player.isOnline() || player.isDead()) {
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    cancel();
                    return;
                }

                if (ticks > 3 && ((Entity) player).isOnGround()) {
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
