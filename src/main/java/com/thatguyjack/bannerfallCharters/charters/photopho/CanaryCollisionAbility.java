package com.thatguyjack.bannerfallCharters.charters.photopho;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.core.AbilitySlot;
import com.thatguyjack.bannerfallCharters.core.CommandCharterAbility;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CanaryCollisionAbility implements CommandCharterAbility {
    private final BannerfallCharters plugin;

    private static final int DASH_TICKS = 10;
    private static final int POST_GROUND_HITBOX_TICKS = 4;

    private static final double HIT_RADIUS = 1.35;
    private static final double KNOCKBACK_STRENGTH = 1.25;
    private static final double KNOCKBACK_Y = 0.45;

    private static final double FLAT_HORIZONTAL_SPEED = 3;
    private static final double FLAT_Y_LIFT = 0.34;

    private static final double ANGLED_HORIZONTAL_SPEED = 2.1;
    private static final double ANGLED_VERTICAL_SPEED = 2.0;
    private static final double MAX_VERTICAL_COMPONENT = 0.85;
    private static final double AIRBORNE_HORIZONTAL_MULTIPLIER = 0.65;

    private static final int DASH_TRAIL_TICKS = DASH_TICKS;

    private static final Particle.DustOptions CANARY_DUST = new Particle.DustOptions(
            Color.fromRGB(255, 235, 120),
            0.95f
    );

    private static final Particle.DustOptions PALE_WIND_DUST = new Particle.DustOptions(
            Color.fromRGB(235, 250, 255),
            0.75f
    );

    private static final Particle.DustTransition CANARY_TO_WHITE_DUST = new Particle.DustTransition(
            Color.fromRGB(255, 226, 95),
            Color.fromRGB(235, 250, 255),
            1.15f
    );

    private static final Particle.DustTransition WHITE_TO_CANARY_DUST = new Particle.DustTransition(
            Color.fromRGB(235, 250, 255),
            Color.fromRGB(255, 235, 120),
            0.9f
    );

    public CanaryCollisionAbility(BannerfallCharters plugin) {
        this.plugin = plugin;
    }


    @Override
    public AbilitySlot slot() {
        return AbilitySlot.MAIN;
    }

    @Override
    public int cooldownSeconds() {
        return 40;
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

        //stupid bugfix. FIX LATER
        plugin.bannerfallFallDamageImmunityManager().grantUntilGround(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() || player.isDead()) {
                plugin.bannerfallFallDamageImmunityManager().grantUntilGround(player);
            }
        }, 6L);

        playDashStartEffects(player);

        player.setRiptiding(true);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() || player.isDead()) {
                startSlowFalling(player);
            }
        }, 15L);

        startCollisionHitbox(player, dash);

        startDashTrail(player, dash);

        return true;
    }

    private Vector getDashVelocity(Player player) {
        float pitch = player.getLocation().getPitch();
        float yaw = player.getLocation().getYaw();

        boolean grounded = ((Entity) player).isOnGround();

        Vector horizontal = new Vector(
                -Math.sin(Math.toRadians(yaw)),
                0,
                Math.cos(Math.toRadians(yaw))
        ).normalize();

        Vector look = player.getLocation().getDirection();

        if (pitch <= -85) {
            return new Vector(0, ANGLED_VERTICAL_SPEED, 0);
        }

        if (pitch >= 85) {
            return new Vector(0, -ANGLED_VERTICAL_SPEED, 0);
        }

        if (pitch >= -7 && pitch <= 20) {
            double horizontalSpeed = grounded
                    ? FLAT_HORIZONTAL_SPEED
                    : FLAT_HORIZONTAL_SPEED * AIRBORNE_HORIZONTAL_MULTIPLIER;

            return horizontal
                    .multiply(horizontalSpeed)
                    .setY(grounded ? FLAT_Y_LIFT : 0.0);
        }

        double vertical = Math.max(
                -MAX_VERTICAL_COMPONENT,
                Math.min(MAX_VERTICAL_COMPONENT, look.getY())
        );

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
            private int groundedTicks = 0;
            private boolean touchedGround = false;

            @Override
            public void run() {
                if(!player.isOnline() || player.isDead()) {
                    player.setRiptiding(false);
                    cancel();
                    return;
                }

                boolean grounded = ticks > 1 && ((Entity) player).isOnGround();
                if(grounded) {
                    touchedGround = true;
                    groundedTicks++;
                    player.setRiptiding(false);
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

                    if(!hasClearPathToPlayer(target, player)) {
                        continue;
                    }

                    knockbackTarget(target, finalKnockbackDirection);
                }

                ticks++;

                if (touchedGround && groundedTicks >= POST_GROUND_HITBOX_TICKS) {
                    player.setRiptiding(false);
                    cancel();
                    return;
                }

                if (!touchedGround && ticks >= DASH_TICKS*2) {
                    player.setRiptiding(false);
                    cancel();
                    return;
                }

                if (ticks >= DASH_TICKS*2 + POST_GROUND_HITBOX_TICKS) {
                    player.setRiptiding(false);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private boolean hasClearPathToPlayer(LivingEntity target, Player player) {
        if(!target.getWorld().equals(player.getWorld())) {
            return false;
        }

        Location start = target.getEyeLocation();
        Location end = player.getEyeLocation();

        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();

        if(distance < 0.001) {
            return true;
        }

        RayTraceResult result = target.getWorld().rayTraceBlocks(start, direction.normalize(), distance, FluidCollisionMode.NEVER, true);

        return result == null;
    }

    private boolean isInWater(Player player) {
        Material feet = player.getLocation().getBlock().getType();
        Material eyes = player.getEyeLocation().getBlock().getType();

        return feet == Material.WATER
                || eyes == Material.WATER
                || feet == Material.BUBBLE_COLUMN
                || eyes == Material.BUBBLE_COLUMN;
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


        Location center = target.getLocation().clone().add(0, 1.0, 0);

        target.getWorld().spawnParticle(
                Particle.GUST_EMITTER_SMALL,
                center,
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );

        target.getWorld().spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                center,
                28,
                0.55,
                0.45,
                0.55,
                0.04,
                CANARY_TO_WHITE_DUST
        );

        target.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                center,
                14,
                0.35,
                0.3,
                0.35,
                0.08
        );

        target.getWorld().spawnParticle(
                Particle.END_ROD,
                center,
                10,
                0.4,
                0.3,
                0.4,
                0.055
        );

        target.getWorld().spawnParticle(
                Particle.ENCHANTED_HIT,
                center,
                12,
                0.45,
                0.35,
                0.45,
                0.06
        );

        target.getWorld().playSound(center, Sound.ENTITY_BREEZE_WIND_BURST, 0.95f, 1.08f);
        target.getWorld().playSound(center, Sound.ENTITY_BREEZE_DEFLECT, 0.45f, 1.35f);
        target.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.5f, 1.25f);
    }

    private void startSlowFalling(Player player) {
        if (((Entity) player).isOnGround()) return;

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOW_FALLING,
                Integer.MAX_VALUE,
                0,
                false,
                false,
                true
        ));

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_BREEZE_INHALE,
                0.1f,
                0.15f
        );

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    cancel();
                    return;
                }

                if (isInWater(player)) {
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    cancel();
                    return;
                }

                if (ticks > 3 && ((Entity) player).isOnGround()) {
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);
                    playFloatLandingEffects(player);
                    cancel();
                    return;
                }

                spawnFloatEffects(player, ticks);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnFloatEffects(Player player, int ticks) {
        Location center = player.getLocation().clone().add(0, 0.85, 0);

        if (ticks % 2 == 0) {
            player.getWorld().spawnParticle(
                    Particle.WHITE_ASH,
                    center,
                    3,
                    0.35,
                    0.25,
                    0.35,
                    0.01
            );
        }

        if (ticks % 4 == 0) {
            player.getWorld().spawnParticle(
                    Particle.END_ROD,
                    center.clone().add(0, -0.25, 0),
                    2,
                    0.22,
                    0.12,
                    0.22,
                    0.008
            );
        }

        if (ticks % 8 == 0) {
            player.getWorld().spawnParticle(
                    Particle.SMALL_GUST,
                    center,
                    1,
                    0.25,
                    0.12,
                    0.25,
                    0.005
            );

            player.getWorld().playSound(
                    center,
                    Sound.ENTITY_BREEZE_IDLE_AIR,
                    0.065f,
                    0.25f
            );
        }
    }

    private void playFloatLandingEffects(Player player) {
        Location center = player.getLocation().clone().add(0, 0.25, 0);

        player.getWorld().playSound(center, Sound.ENTITY_BREEZE_LAND, 0.35f, 1.35f);

        player.getWorld().spawnParticle(
                Particle.WHITE_SMOKE,
                center,
                8,
                0.35,
                0.08,
                0.35,
                0.012
        );

        player.getWorld().spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                center.clone().add(0, 0.15, 0),
                8,
                0.3,
                0.08,
                0.3,
                0.01,
                WHITE_TO_CANARY_DUST
        );
    }

    private void playDashStartEffects(Player player) {
        Location center = player.getLocation().clone().add(0, 1.0, 0);

        player.getWorld().playSound(center, Sound.ENTITY_BREEZE_INHALE, 0.45f, 1.45f);
        player.getWorld().playSound(center, Sound.ENTITY_BREEZE_JUMP, 0.9f, 1.18f);
        player.getWorld().playSound(center, Sound.ENTITY_BREEZE_SHOOT, 0.7f, 1.45f);
        player.getWorld().playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.25f, 1.75f);

        player.getWorld().spawnParticle(
                Particle.GUST_EMITTER_SMALL,
                center,
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );

        player.getWorld().spawnParticle(
                Particle.DUST_COLOR_TRANSITION,
                center,
                22,
                0.55,
                0.45,
                0.55,
                0.02,
                CANARY_TO_WHITE_DUST
        );

        player.getWorld().spawnParticle(
                Particle.END_ROD,
                center,
                10,
                0.35,
                0.35,
                0.35,
                0.035
        );

        player.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                center,
                8,
                0.3,
                0.25,
                0.3,
                0.035
        );

        player.getWorld().spawnParticle(
                Particle.SMALL_GUST,
                center,
                4,
                0.45,
                0.25,
                0.45,
                0.02
        );
    }

    private void startDashTrail(Player player, Vector dash) {
        Vector backwards = dash.clone();

        if (backwards.lengthSquared() > 0.001) {
            backwards.normalize().multiply(-0.45);
        } else {
            backwards = new Vector(0, 0, 0);
        }

        Vector finalBackwards = backwards;

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                Location center = player.getLocation().clone().add(0, 0.9, 0);
                Location trailPoint = center.clone().add(finalBackwards);

                player.getWorld().spawnParticle(
                        Particle.DUST_COLOR_TRANSITION,
                        trailPoint,
                        8,
                        0.18,
                        0.22,
                        0.18,
                        0.01,
                        WHITE_TO_CANARY_DUST
                );

                player.getWorld().spawnParticle(
                        Particle.END_ROD,
                        trailPoint,
                        3,
                        0.16,
                        0.18,
                        0.16,
                        0.018
                );

                if (ticks % 2 == 0) {
                    player.getWorld().spawnParticle(
                            Particle.SMALL_GUST,
                            trailPoint,
                            2,
                            0.24,
                            0.18,
                            0.24,
                            0.01
                    );
                }

                spawnDashSpiral(player, ticks);

                ticks++;

                if (ticks >= DASH_TRAIL_TICKS) {
                    playDashEndEffects(player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void spawnDashSpiral(Player player, int ticks) {
        Location center = player.getLocation().clone().add(0, 0.95, 0);

        double baseAngle = Math.toRadians(ticks * 55.0);

        for (int i = 0; i < 3; i++) {
            double angle = baseAngle + ((Math.PI * 2) / 3.0) * i;
            double radius = 0.42;
            double yOffset = -0.35 + (i * 0.35);

            Location point = center.clone().add(
                    Math.cos(angle) * radius,
                    yOffset,
                    Math.sin(angle) * radius
            );

            Particle.DustOptions dust = i % 2 == 0 ? CANARY_DUST : PALE_WIND_DUST;

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.0,
                    dust
            );
        }
    }

    private void playDashEndEffects(Player player) {
        if (!player.isOnline()) {
            return;
        }

        Location center = player.getLocation().clone().add(0, 0.75, 0);

        player.getWorld().playSound(center, Sound.ENTITY_BREEZE_WHIRL, 0.28f, 1.55f);
        player.getWorld().playSound(center, Sound.ENTITY_BREEZE_LAND, 0.32f, 1.35f);

        player.getWorld().spawnParticle(
                Particle.GUST,
                center,
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );

        player.getWorld().spawnParticle(
                Particle.WHITE_SMOKE,
                center,
                8,
                0.3,
                0.16,
                0.3,
                0.015
        );

        player.getWorld().spawnParticle(
                Particle.DUST,
                center,
                8,
                0.28,
                0.16,
                0.28,
                0.0,
                PALE_WIND_DUST
        );
    }
}
