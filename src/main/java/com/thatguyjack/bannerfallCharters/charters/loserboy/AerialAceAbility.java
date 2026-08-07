package com.thatguyjack.bannerfallCharters.charters.loserboy;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.core.AbilitySlot;
import com.thatguyjack.bannerfallCharters.core.CommandCharterAbility;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

public class AerialAceAbility implements CommandCharterAbility {
    private final BannerfallCharters plugin;

    private double PULL_RADIUS = 5.0;
    private double PULL_STRENGTH = 1.4;

    private double SPIRAL_ROTATION_SPEED = 3.8;

    private double LAUNCH_SPEED = 1.2;
    private int LAUNCH_CUTOFF_TICKS = 6;
    private double POST_LAUNCH_Y = 0.15;

    private int PULL_TICKS = 12;
    private int AIR_TICKS = 16;

    private int SLASH_COUNT = 3;
    private int SLASH_STAGGER_TICKS = 3;
    private int SLASH_ANIMATION_TICKS = 10;
    private int SLASH_START_DELAY = 1;

    private double SLASH_RADIUS = PULL_RADIUS * 0.9;
    private double SLASH_LIFT_Y = 0.22;

    private double SWORD_SLASH_RADIUS = 2.8;
    private double SWORD_SLASH_HEIGHT = -0.8;
    private double SWORD_SLASH_ARC_DEGREES = 145.0;

    private double SLASH_DAMAGE_MULTIPLIER = 0.45;

    private static final Particle.DustOptions AERIAL_DUST = new Particle.DustOptions(
            Color.fromRGB(180, 225, 255),
            1.0f
    );

    private static final Particle.DustOptions SWORD_DUST = new Particle.DustOptions(
            Color.fromRGB(245, 250, 255),
            0.85f
    );

    private static final Particle.DustOptions VORTEX_BLUE_DUST = new Particle.DustOptions(
            Color.fromRGB(120, 210, 255),
            0.9f
    );

    private static final Particle.DustOptions VORTEX_WHITE_DUST = new Particle.DustOptions(
            Color.fromRGB(245, 252, 255),
            0.75f
    );

    private static final Particle.DustTransition SLASH_DUST_TRANSITION = new Particle.DustTransition(
            Color.fromRGB(245, 252, 255),
            Color.fromRGB(120, 210, 255),
            1.25f
    );

    private static final Particle.DustTransition VORTEX_DUST_TRANSITION = new Particle.DustTransition(
            Color.fromRGB(120, 210, 255),
            Color.fromRGB(245, 252, 255),
            0.9f
    );

    public AerialAceAbility(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    @Override
    public AbilitySlot slot() {
        return AbilitySlot.MAIN;
    }

    @Override
    public int cooldownSeconds() {
        return 60;
    }

    @Override
    public String id() {
        return "aerialace";
    }

    @Override
    public String displayName() {
        return "Aerial Ace";
    }

    @Override
    public boolean activate(Player player) {
        if(!isHoldingSword(player)) {
            player.sendMessage(ChatColor.RED + "You need to be holding a sword to use Aerial Ace.");
            return false;
        }

        startPullAndLaunch(player);

        return true;
    }

    private boolean isHoldingSword(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if(item == null || item.getType() == Material.AIR) {
            return false;
        }

        return item.getType().name().endsWith("_SWORD");
    }

    private void spawnFloorVortex(Player player, int ticks) {
        Location center = player.getLocation().clone();
        center.setY(player.getLocation().getBlockY() + 0.08);

        int arms = 5;
        int pointsPerArm = 16;

        double maxRadius = 3.2;
        double minRadius = 0.25;

        double rotation = Math.toRadians(ticks * -SPIRAL_ROTATION_SPEED);

        double inwardShift = (ticks % 40) / 40.0;

        for (int arm = 0; arm < arms; arm++) {
            double armOffset = ((Math.PI * 2) / arms) * arm;

            for (int point = 0; point < pointsPerArm; point++) {
                double progress = point / (double) (pointsPerArm - 1);

                double radius = maxRadius - ((maxRadius - minRadius) * progress);

                radius -= inwardShift * 0.65;

                if (radius < minRadius) {
                    radius += maxRadius - minRadius;
                }

                double curve = progress * 2.8;

                double angle = armOffset + curve + rotation;

                Location particleLocation = center.clone().add(
                        Math.cos(angle) * radius,
                        0,
                        Math.sin(angle) * radius
                );

                Particle.DustOptions dust = point % 2 == 0 ? AERIAL_DUST : SWORD_DUST;

                player.getWorld().spawnParticle(
                        Particle.DUST,
                        particleLocation,
                        1,
                        0,
                        0,
                        0,
                        0,
                        dust
                );
            }
        }
    }

    private void startPullAndLaunch(Player player) {
        Location vortexCenter = player.getLocation().clone();
        vortexCenter.setY(player.getLocation().getBlockY() + 0.08);

        List<LivingEntity> pulledEntities = new ArrayList<>();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                spawnFloorVortex(player, ticks);

                for (Entity entity : vortexCenter.getWorld().getNearbyEntities(
                        vortexCenter,
                        PULL_RADIUS*1.1,
                        PULL_RADIUS*1.1,
                        PULL_RADIUS*1.1
                )) {
                    if(!(entity instanceof LivingEntity target)) {
                        continue;
                    }

                    if(target.getUniqueId().equals(player.getUniqueId())) {
                        continue;
                    }

                    if(!pulledEntities.contains(target)) {
                        pulledEntities.add(target);
                    }

                    pullEntityToCenter(target, vortexCenter);
                }

                ticks++;

                if (ticks >= PULL_TICKS) {
                    launchPlayersAndTargets(player, pulledEntities);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void pullEntityToCenter(LivingEntity target, Location vortexCenter) {
        Vector direction = vortexCenter.toVector().subtract(target.getLocation().toVector());
        direction.setY(0);

        if (direction.lengthSquared() < 0.001) {
            Vector currentVelocity = target.getVelocity();
            currentVelocity.setX(currentVelocity.getX() * 0.65);
            currentVelocity.setZ(currentVelocity.getZ() * 0.65);
            target.setVelocity(currentVelocity);
            return;
        }

        double distance = direction.length();

        direction.normalize();

        double scaledPullStrength = Math.min(PULL_STRENGTH/10, 0.04 + distance * 0.035);

        if (distance < 1.25) {
            scaledPullStrength *= distance / 1.25;
        }

        Vector currentVelocity = target.getVelocity();

        currentVelocity.setX(currentVelocity.getX() * 0.65);
        currentVelocity.setZ(currentVelocity.getZ() * 0.65);

        Vector pullVelocity = direction.multiply(scaledPullStrength);
        pullVelocity.setY(0.04);

        target.setVelocity(currentVelocity.add(pullVelocity));
    }

    private void launchPlayersAndTargets(Player player, List<LivingEntity> targets) {
        launchAndHover(player);
        plugin.bannerfallFallDamageImmunityManager().grantUntilGround(player);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                player.setVelocity(new Vector(0, -LAUNCH_SPEED*1.5, 0));
            }
        }, LAUNCH_CUTOFF_TICKS+AIR_TICKS);


        for (LivingEntity target : targets) {
            if (target.isDead() || !target.isValid()) {
                continue;
            }

            launchAndHover(target);
        }

        Location center = player.getLocation().clone().add(0, 1.0, 0);

        player.getWorld().playSound(center, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.9f, 1.35f);
        player.getWorld().playSound(center, Sound.ITEM_TRIDENT_RIPTIDE_1, 0.35f, 1.7f);

        player.getWorld().spawnParticle(
                Particle.SWEEP_ATTACK,
                center,
                3,
                0.35,
                0.25,
                0.35,
                0.0
        );

        player.getWorld().spawnParticle(
                Particle.DUST,
                center,
                24,
                0.45,
                0.65,
                0.45,
                0.0,
                SWORD_DUST
        );

        ItemStack usedSword = player.getInventory().getItemInMainHand().clone();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                startSlashSequence(player, targets, usedSword);
            }
        }, LAUNCH_CUTOFF_TICKS);

    }

    private void launchAndHover(LivingEntity entity) {
        entity.setVelocity(new Vector(0, LAUNCH_SPEED, 0));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if(entity.isDead() || !entity.isValid()) {
                return;
            }

            Vector velocity = entity.getVelocity();

            if(velocity.getY() > POST_LAUNCH_Y) {
                velocity.setY(POST_LAUNCH_Y);
                entity.setVelocity(velocity);
            }

            startHover(entity);
        }, LAUNCH_CUTOFF_TICKS);
    }

    private void startHover(LivingEntity entity) {
        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (entity.isDead() || !entity.isValid()) {
                    cancel();
                    return;
                }

                Vector velocity = entity.getVelocity();

                velocity.setY(0.02);
                entity.setVelocity(velocity);

                ticks++;

                if(ticks >= AIR_TICKS) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startSlashSequence(Player player, List<LivingEntity> targets, ItemStack sword) {
        for (int i = 0; i < SLASH_COUNT; i++) {
            int slashIndex = i;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline() || player.isDead()) {
                    return;
                }

                double baseAngle = getPseudoRandomSlashAngle(player, slashIndex);

                startSingleAnimatedSlash(player, targets, sword.clone(), slashIndex, baseAngle);
            }, SLASH_START_DELAY + slashIndex * SLASH_STAGGER_TICKS);
        }
    }

    private double getPseudoRandomSlashAngle(Player player, int slashIndex) {
        long seed = player.getUniqueId().getMostSignificantBits()
                ^ player.getUniqueId().getLeastSignificantBits()
                ^ (player.getWorld().getGameTime() * 31L)
                ^ (slashIndex * 997L);

        Random random = new Random(seed);

        double base = player.getLocation().getYaw();

        double spread = switch (slashIndex) {
            case 0 -> -55.0;
            case 1 -> 55.0;
            default -> 180.0;
        };

        double randomOffset = random.nextDouble(-24.0, 24.0);

        return base + spread + randomOffset;
    }

    private void startSingleAnimatedSlash(Player player, List<LivingEntity> targets, ItemStack sword, int slashIndex, double baseAngleDegrees) {
        ArmorStand swordVisual = spawnSlashSword(player, sword);

        Set<UUID> hitThisSlash = new HashSet<>();

        new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    swordVisual.remove();
                    cancel();
                    return;
                }

                Location center = player.getLocation().clone().add(0, SWORD_SLASH_HEIGHT, 0);

                double progress = ticks / (double) Math.max(1, SLASH_ANIMATION_TICKS - 1);

                boolean reverseSlash = slashIndex % 2 == 1;

                double arcStart = reverseSlash
                        ? SWORD_SLASH_ARC_DEGREES / 2.0
                        : -SWORD_SLASH_ARC_DEGREES / 2.0;

                double arcEnd = reverseSlash
                        ? -SWORD_SLASH_ARC_DEGREES / 2.0
                        : SWORD_SLASH_ARC_DEGREES / 2.0;

                double arc = arcStart + (arcEnd - arcStart) * progress;

                double angle = Math.toRadians(baseAngleDegrees + arc);

                double heightWave = Math.sin(progress * Math.PI) * 0.45;

                double diagonalStartY = 0.75;
                double diagonalEndY = -0.45;
                double diagonalY = diagonalStartY + (diagonalEndY - diagonalStartY) * progress;

                Location swordLocation = center.clone().add(
                        -Math.sin(angle) * SWORD_SLASH_RADIUS,
                        diagonalY,
                        Math.cos(angle) * SWORD_SLASH_RADIUS
                );

                swordVisual.teleport(swordLocation);
                swordVisual.setRotation((float) Math.toDegrees(angle) + 90f, 0f);

                swordVisual.setRightArmPose(new EulerAngle(
                        Math.toRadians(230),
                        Math.toRadians(reverseSlash ? -25 : 25),
                        Math.toRadians(40 + progress * 260)
                ));

                spawnSlashArcParticles(player, center, baseAngleDegrees, progress, slashIndex);

                if (ticks == SLASH_ANIMATION_TICKS / 2) {
                    damageSlashTargets(player, targets, sword, center, hitThisSlash);
                }

                ticks++;

                if (ticks >= SLASH_ANIMATION_TICKS) {
                    swordVisual.remove();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private ArmorStand spawnSlashSword(Player player, ItemStack sword) {
        return player.getWorld().spawn(
                player.getLocation().clone().add(0, SWORD_SLASH_HEIGHT, 0),
                ArmorStand.class,
                stand -> {
                    stand.setInvisible(true);
                    stand.setMarker(true);
                    stand.setGravity(false);
                    stand.setSilent(true);
                    stand.setInvulnerable(true);
                    stand.setCollidable(false);
                    stand.getEquipment().setItem(EquipmentSlot.HAND, sword);
                    stand.setRightArmPose(new EulerAngle(Math.toRadians(250), 0, 0));
                }
        );
    }

    private void spawnSlashArcParticles(Player player, Location center, double baseAngleDegrees, double progress, int slashIndex) {
        boolean reverseSlash = slashIndex % 2 == 1;

        double arcStart = reverseSlash
                ? SWORD_SLASH_ARC_DEGREES / 2.0
                : -SWORD_SLASH_ARC_DEGREES / 2.0;

        double arcEnd = reverseSlash
                ? -SWORD_SLASH_ARC_DEGREES / 2.0
                : SWORD_SLASH_ARC_DEGREES / 2.0;

        double arc = arcStart + (arcEnd - arcStart) * progress;

        for (int i = 0; i < 9; i++) {
            double pointProgress = i / 8.0;

            double angleDegrees = baseAngleDegrees + arcStart + (arcEnd - arcStart) * pointProgress;
            double angle = Math.toRadians(angleDegrees);

            double radius = 1.15 + pointProgress * 1.0;
            double height = 0.65 + (-0.45 - 0.65) * pointProgress;

            Location point = center.clone().add(
                    -Math.sin(angle) * radius,
                    height,
                    Math.cos(angle) * radius
            );

            Particle.DustOptions dust = slashIndex % 2 == 0 ? SWORD_DUST : AERIAL_DUST;

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0,
                    0,
                    0,
                    0,
                    dust
            );
        }

        if (progress > 0.25 && progress < 0.85) {
            player.getWorld().spawnParticle(
                    Particle.SWEEP_ATTACK,
                    center,
                    1,
                    0.35,
                    0.15,
                    0.35,
                    0.0
            );
        }
    }

    private void damageSlashTargets(Player player, List<LivingEntity> targets, ItemStack sword, Location slashCenter, Set<UUID> hitThisSlash) {
        player.getWorld().playSound(slashCenter, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.95f, 1.1f);
        player.getWorld().playSound(slashCenter, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.45f, 1.35f);
        player.getWorld().playSound(slashCenter, Sound.ENTITY_BREEZE_WIND_BURST, 0.35f, 1.45f);

        for (LivingEntity target : targets) {
            if (target == null || !target.isValid() || target.isDead()) {
                continue;
            }

            if (target.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            if (!target.getWorld().equals(player.getWorld())) {
                continue;
            }

            if (target.getLocation().distanceSquared(slashCenter) > SLASH_RADIUS * SLASH_RADIUS) {
                continue;
            }

            if (!hitThisSlash.add(target.getUniqueId())) {
                continue;
            }

            double damage = getSwordDamageAgainstTarget(sword, target) * SLASH_DAMAGE_MULTIPLIER;

            target.setNoDamageTicks(0);
            target.damage(damage, player);
            target.setNoDamageTicks(0);

            Vector velocity = target.getVelocity();
            velocity.setY(Math.max(velocity.getY(), SLASH_LIFT_Y));
            target.setVelocity(velocity);

            applySwordBonusEffects(sword, target);

            target.getWorld().spawnParticle(
                    Particle.CRIT,
                    target.getLocation().clone().add(0, 1.0, 0),
                    10,
                    0.25,
                    0.25,
                    0.25,
                    0.04
            );
        }
    }

    private double getSwordDamage(ItemStack sword) {
        if (sword == null || sword.getType() == Material.AIR) {
            return 1.0;
        }

        double damage = switch (sword.getType()) {
            case WOODEN_SWORD, GOLDEN_SWORD -> 4.0;
            case STONE_SWORD -> 5.0;
            case IRON_SWORD -> 6.0;
            case DIAMOND_SWORD -> 7.0;
            case NETHERITE_SWORD -> 8.0;
            default -> 1.0;
        };

        int sharpness = sword.getEnchantmentLevel(Enchantment.SHARPNESS);
        if (sharpness > 0) {
            damage += 0.5 * sharpness + 0.5;
        }

        return damage;
    }

    private double getSwordDamageAgainstTarget(ItemStack sword, LivingEntity target) {
        double damage = getSwordDamage(sword);

        int smite = sword.getEnchantmentLevel(Enchantment.SMITE);
        if (smite > 0 && isUndead(target)) {
            damage += 2.5 * smite;
        }

        int bane = sword.getEnchantmentLevel(Enchantment.BANE_OF_ARTHROPODS);
        if (bane > 0 && isArthropod(target)) {
            damage += 2.5 * bane;
        }

        return damage;
    }

    private boolean isUndead(LivingEntity target) {
        return switch (target.getType()) {
            case ZOMBIE,
                 HUSK,
                 DROWNED,
                 ZOMBIE_VILLAGER,
                 ZOMBIFIED_PIGLIN,
                 SKELETON,
                 STRAY,
                 WITHER_SKELETON,
                 SKELETON_HORSE,
                 ZOMBIE_HORSE,
                 PHANTOM,
                 WITHER,
                 ZOGLIN,
                 BOGGED -> true;
            default -> false;
        };
    }

    private boolean isArthropod(LivingEntity target) {
        return switch (target.getType()) {
            case SPIDER,
                 CAVE_SPIDER,
                 SILVERFISH,
                 ENDERMITE,
                 BEE -> true;
            default -> false;
        };
    }

    private void applySwordBonusEffects(ItemStack sword, LivingEntity target) {
        int fireAspect = sword.getEnchantmentLevel(Enchantment.FIRE_ASPECT);

        if (fireAspect > 0) {
            target.setFireTicks(Math.max(target.getFireTicks(), fireAspect * 80));
        }
    }
}
