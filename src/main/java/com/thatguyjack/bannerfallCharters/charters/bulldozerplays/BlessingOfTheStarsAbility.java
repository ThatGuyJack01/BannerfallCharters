package com.thatguyjack.bannerfallCharters.charters.bulldozerplays;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.core.AbilitySlot;
import com.thatguyjack.bannerfallCharters.core.CommandCharterAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class BlessingOfTheStarsAbility implements CommandCharterAbility {

    private final BannerfallCharters plugin;

    private static final int NIGHT_DURATION_TICKS = 20 * 120;
    private static final int DAY_DURATION_TICKS = 20 * 120;

    private static final int NIGHT_COOLDOWN_SECONDS = 5 * 60;

    private final Map<UUID, Long> nightCooldowns = new HashMap<>();
    private final Map<UUID, Long> usedDayBlessingOnDay = new HashMap<>();
    private final Map<UUID, BukkitTask> passiveParticleTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> daySunsetCleanupTasks = new HashMap<>();

    public BlessingOfTheStarsAbility(BannerfallCharters plugin) {
        this.plugin = plugin;
    }


    private static final Particle.DustOptions NIGHT_BLUE =
            new Particle.DustOptions(
                    Color.fromRGB(90, 115, 255),
                    1.0f
            );

    private static final Particle.DustOptions NIGHT_LIGHT =
            new Particle.DustOptions(
                    Color.fromRGB(215, 230, 255),
                    0.85f
            );

    private static final Particle.DustOptions NIGHT_STAR =
            new Particle.DustOptions(
                    Color.fromRGB(245, 245, 255),
                    0.7f
            );

    private static final Particle.DustOptions DAY_GOLD =
            new Particle.DustOptions(
                    Color.fromRGB(255, 180, 55),
                    0.9f
            );

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
        return "blessingofthestars";
    }

    @Override
    public String displayName() {
        return "Blessing of the Stars";
    }

    @Override
    public boolean activate(Player player) {
        World world = player.getWorld();

        boolean daytime = world.isDayTime();
        UUID uuid = player.getUniqueId();
        boolean ignoresCooldown = player.getGameMode() == GameMode.CREATIVE;

        if (daytime) {
            long minecraftDay = getMinecraftDay(world);
            Long usedDay = usedDayBlessingOnDay.get(uuid);

            if (!ignoresCooldown && usedDay != null && usedDay == minecraftDay) {
                player.sendMessage(ChatColor.RED + "Blessing of the Stars has already answered you today. Wait until nightfall.");
                return false;
            }

            activateDayBlessing(player);

            if (!ignoresCooldown) {
                usedDayBlessingOnDay.put(uuid, minecraftDay);
            }

            return true;
        }

        long remainingMillis = getNightCooldownRemainingMillis(uuid);

        if (!ignoresCooldown && remainingMillis > 0) {
            sendNightCooldownMessage(player, remainingMillis);
            return false;
        }

        activateNightBlessing(player);

        if (!ignoresCooldown) {
            nightCooldowns.put(uuid, System.currentTimeMillis() + NIGHT_COOLDOWN_SECONDS * 1000L);
        }

        return true;
    }

    private long getMinecraftDay(World world) {
        return world.getFullTime() / 24000L;
    }

    private long getNightCooldownRemainingMillis(UUID uuid) {
        Long cooldownEnd = nightCooldowns.get(uuid);

        if (cooldownEnd == null) {
            return 0L;
        }

        long remaining = cooldownEnd - System.currentTimeMillis();

        if (remaining <= 0) {
            nightCooldowns.remove(uuid);
            return 0L;
        }

        return remaining;
    }

    private void sendNightCooldownMessage(Player player, long remainingMillis) {
        long remainingSeconds = (remainingMillis + 999L) / 1000L;
        long minutes = remainingSeconds / 60L;
        long seconds = remainingSeconds % 60L;

        player.sendMessage(ChatColor.RED + "Blessing of the Stars will return in "
                + minutes + "m " + seconds + "s.");
    }

    private void activateNightBlessing(Player player) {
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        NIGHT_DURATION_TICKS,
                        0
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        NIGHT_DURATION_TICKS,
                        0
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.REGENERATION,
                        NIGHT_DURATION_TICKS,
                        0
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                    PotionEffectType.SLOW_FALLING,
                    NIGHT_DURATION_TICKS,
                    0
                )
        );

        playNightActivation(player);
        startPassiveParticles(player, true, NIGHT_DURATION_TICKS);
    }

    private void activateDayBlessing(Player player) {
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.WEAKNESS,
                        DAY_DURATION_TICKS,
                        1
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SPEED,
                        DAY_DURATION_TICKS,
                        0
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        DAY_DURATION_TICKS,
                        0
                )
        );

        playDayActivation(player);
        startPassiveParticles(player, false, DAY_DURATION_TICKS);
        startDaySunsetCleanup(player);
    }

    private void startDaySunsetCleanup(Player player) {
        UUID uuid = player.getUniqueId();

        BukkitTask existingTask = daySunsetCleanupTasks.remove(uuid);

        if (existingTask != null) {
            existingTask.cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    daySunsetCleanupTasks.remove(uuid);
                    cancel();
                    return;
                }

                if (!player.getWorld().isDayTime()) {
                    player.removePotionEffect(PotionEffectType.WEAKNESS);
                    player.removePotionEffect(PotionEffectType.SPEED);
                    player.removePotionEffect(PotionEffectType.SLOW_FALLING);

                    player.sendMessage(ChatColor.GRAY + "As the sun sets, the daytime blessing fades.");

                    World world = player.getWorld();
                    Location location = player.getLocation().add(0, 1.0, 0);

                    world.spawnParticle(
                            Particle.SMOKE,
                            location,
                            10,
                            0.45,
                            0.55,
                            0.45,
                            0.02
                    );

                    world.playSound(
                            location,
                            Sound.BLOCK_FIRE_EXTINGUISH,
                            0.45f,
                            1.25f
                    );

                    BukkitTask passiveTask = passiveParticleTasks.remove(uuid);
                    if (passiveTask != null) {
                        passiveTask.cancel();
                    }

                    daySunsetCleanupTasks.remove(uuid);
                    cancel();
                    return;
                }

                if (!player.hasPotionEffect(PotionEffectType.WEAKNESS)
                        && !player.hasPotionEffect(PotionEffectType.SPEED)) {
                    daySunsetCleanupTasks.remove(uuid);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);

        daySunsetCleanupTasks.put(uuid, task);
    }

    private void playNightActivation(Player player) {
        World world = player.getWorld();
        Location start = player.getLocation().clone().add(0, 1.0, 0);

        world.playSound(
                start,
                Sound.BLOCK_BEACON_POWER_SELECT,
                0.65f,
                1.45f
        );

        world.playSound(
                start,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                0.8f,
                1.15f
        );

        new BukkitRunnable() {

            private int tick = 0;

            @Override
            public void run() {
                if(!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                World world = player.getWorld();
                Location base = player.getLocation().clone();
                Location chest = base.clone().add(0, 1.1, 0);

                double rotation = tick * 0.17;

                for(int strand = 0; strand < 2; strand++) {
                    double strandOffset = strand * Math.PI;

                    for(int i = 0; i < 3; i++) {
                        double height =
                                0.25 + ((tick * 0.07) + i * 0.55) % 1.65;

                        double radius = 0.72;

                        double angle =
                                rotation
                                        + strandOffset
                                        + height * 1.65;

                        Location point = base.clone().add(
                                Math.cos(angle) * radius,
                                height,
                                Math.sin(angle) * radius
                        );

                        world.spawnParticle(
                                Particle.DUST,
                                point,
                                1,
                                0,
                                0,
                                0,
                                0,
                                i % 2 == 0 ? NIGHT_BLUE : NIGHT_LIGHT
                        );

                        if((tick + i + strand) % 3 == 0) {
                            world.spawnParticle(
                                    Particle.END_ROD,
                                    point,
                                    1,
                                    0.01,
                                    0.01,
                                    0.01,
                                    0
                            );
                        }
                    }
                }

                for(int i = 0; i < 4; i++) {
                    int delay = i * 3;
                    double progress = (tick - delay) / 18.0;

                    if(progress >= 0.0 && progress <= 1.0) {
                        double angle =
                                i * (Math.PI * 2.0 / 4.0)
                                        + rotation * 0.45;

                        double radius =
                                1.45 - (progress * 0.85);

                        double y =
                                3.0 - (progress * 1.35);

                        Location fallingStar = base.clone().add(
                                Math.cos(angle) * radius,
                                y,
                                Math.sin(angle) * radius
                        );

                        world.spawnParticle(
                                Particle.GLOW,
                                fallingStar,
                                1,
                                0.02,
                                0.02,
                                0.02,
                                0
                        );

                        world.spawnParticle(
                                Particle.DUST,
                                fallingStar,
                                1,
                                0,
                                0,
                                0,
                                0,
                                NIGHT_STAR
                        );

                        if(tick % 4 == 0) {
                            world.spawnParticle(
                                    Particle.END_ROD,
                                    fallingStar,
                                    1,
                                    0.01,
                                    0.01,
                                    0.01,
                                    0
                            );
                        }
                    }
                }

                if(tick >= 10) {
                    Location crownCenter = base.clone().add(0, 2.15, 0);

                    for(int i = 0; i < 6; i++) {
                        double angle =
                                rotation * 0.6
                                        + i * (Math.PI * 2.0 / 6.0);

                        double radius =
                                0.78 + Math.sin(i * 1.7) * 0.06;

                        double y =
                                Math.sin(angle * 2.0 + i) * 0.08;

                        Location star = crownCenter.clone().add(
                                Math.cos(angle) * radius,
                                y,
                                Math.sin(angle) * radius
                        );

                        world.spawnParticle(
                                i % 2 == 0 ? Particle.GLOW : Particle.DUST,
                                star,
                                1,
                                0,
                                0,
                                0,
                                0,
                                i % 2 == 0 ? null : NIGHT_LIGHT
                        );

                        if(i % 2 == 1 && tick % 5 == 0) {
                            world.spawnParticle(
                                    Particle.END_ROD,
                                    star,
                                    1,
                                    0,
                                    0,
                                    0,
                                    0
                            );
                        }
                    }
                }

                if(tick == 16) {
                    world.playSound(
                            chest,
                            Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
                            0.55f,
                            1.7f
                    );

                    for(int i = 0; i < 8; i++) {
                        double angle = i * (Math.PI * 2.0 / 8.0);

                        Location pulse = chest.clone().add(
                                Math.cos(angle) * 0.55,
                                0.15,
                                Math.sin(angle) * 0.55
                        );

                        world.spawnParticle(
                                Particle.GLOW,
                                pulse,
                                1,
                                0.02,
                                0.02,
                                0.02,
                                0
                        );

                        world.spawnParticle(
                                Particle.DUST,
                                pulse,
                                1,
                                0,
                                0,
                                0,
                                0,
                                NIGHT_LIGHT
                        );
                    }
                }

                if(tick == 28) {
                    world.playSound(
                            chest,
                            Sound.BLOCK_AMETHYST_BLOCK_RESONATE,
                            0.8f,
                            1.25f
                    );

                    world.playSound(
                            chest,
                            Sound.BLOCK_BEACON_ACTIVATE,
                            0.45f,
                            1.7f
                    );

                    for(int i = 0; i < 10; i++) {
                        double angle = i * (Math.PI * 2.0 / 10.0);

                        Location burst = chest.clone().add(
                                Math.cos(angle) * 0.65,
                                0.05,
                                Math.sin(angle) * 0.65
                        );

                        world.spawnParticle(
                                Particle.DUST,
                                burst,
                                1,
                                0,
                                0,
                                0,
                                0,
                                i % 2 == 0 ? NIGHT_BLUE : NIGHT_LIGHT
                        );

                        world.spawnParticle(
                                Particle.GLOW,
                                burst,
                                1,
                                0.02,
                                0.02,
                                0.02,
                                0
                        );
                    }
                }

                if(tick >= 28 && tick <= 38) {
                    double radius =
                            0.25 + (tick - 28) * 0.08;

                    int points = 12;

                    for(int i = 0; i < points; i++) {
                        double angle = i * (Math.PI * 2.0 / points);

                        Location halo = base.clone().add(
                                Math.cos(angle) * radius,
                                0.08,
                                Math.sin(angle) * radius
                        );

                        world.spawnParticle(
                                Particle.DUST,
                                halo,
                                1,
                                0,
                                0,
                                0,
                                0,
                                i % 2 == 0 ? NIGHT_BLUE : NIGHT_LIGHT
                        );

                        if(i % 3 == 0) {
                            world.spawnParticle(
                                    Particle.END_ROD,
                                    halo,
                                    1,
                                    0,
                                    0,
                                    0,
                                    0
                            );
                        }
                    }
                }

                tick++;

                if(tick >= 42) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void playDayActivation(Player player) {
        World world = player.getWorld();

        Location start =
                player.getLocation().clone().add(0, 1.0, 0);

        world.playSound(
                start,
                Sound.ENTITY_FIREWORK_ROCKET_SHOOT,
                0.55f,
                0.55f
        );


        new BukkitRunnable() {

            private int tick = 0;

            @Override
            public void run() {
                if(!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                ThreadLocalRandom random =
                        ThreadLocalRandom.current();

                World world = player.getWorld();

                Location base =
                        player.getLocation().clone();

                Location center =
                        base.clone().add(0, 1.0, 0);

                world.spawnParticle(
                        Particle.SMOKE,
                        center,
                        random.nextInt(1, 4),
                        0.5,
                        0.75,
                        0.5,
                        0.015
                );

                if(random.nextDouble() < 0.75) {
                    Location spark =
                            randomPointAroundPlayer(
                                    base,
                                    random,
                                    0.2,
                                    0.85,
                                    0.15,
                                    2.1
                            );

                    world.spawnParticle(
                            Particle.ELECTRIC_SPARK,
                            spark,
                            random.nextInt(1, 4),
                            0.08,
                            0.08,
                            0.08,
                            0.06
                    );
                }

                if(random.nextDouble() < 0.55) {
                    Location flame =
                            randomPointAroundPlayer(
                                    base,
                                    random,
                                    0.25,
                                    0.8,
                                    0.1,
                                    1.9
                            );

                    world.spawnParticle(
                            Particle.SMALL_FLAME,
                            flame,
                            random.nextInt(1, 4),
                            0.07,
                            0.09,
                            0.07,
                            0.025
                    );
                }

                if(random.nextDouble() < 0.35) {
                    Location fragment =
                            randomPointAroundPlayer(
                                    base,
                                    random,
                                    0.35,
                                    1.0,
                                    0.2,
                                    2.2
                            );

                    world.spawnParticle(
                            Particle.DUST,
                            fragment,
                            random.nextInt(1, 3),
                            0.05,
                            0.05,
                            0.05,
                            0,
                            DAY_GOLD
                    );
                }

                if(tick == 4
                        || tick == 10
                        || tick == 18) {

                    Location misfire =
                            randomPointAroundPlayer(
                                    base,
                                    random,
                                    0.35,
                                    0.9,
                                    0.25,
                                    1.9
                            );

                    world.spawnParticle(
                            Particle.ELECTRIC_SPARK,
                            misfire,
                            8,
                            0.18,
                            0.18,
                            0.18,
                            0.1
                    );

                    world.spawnParticle(
                            Particle.SMALL_FLAME,
                            misfire,
                            5,
                            0.15,
                            0.15,
                            0.15,
                            0.04
                    );

                    world.spawnParticle(
                            Particle.SMOKE,
                            misfire,
                            6,
                            0.2,
                            0.2,
                            0.2,
                            0.03
                    );

                    world.playSound(
                            misfire,
                            Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                            0.35f,
                            0.65f + random.nextFloat() * 0.45f
                    );
                }

                if(tick == 26) {
                    Location failure =
                            center.clone().add(
                                    random.nextDouble(-0.25, 0.25),
                                    random.nextDouble(-0.2, 0.25),
                                    random.nextDouble(-0.25, 0.25)
                            );

                    world.spawnParticle(
                            Particle.LARGE_SMOKE,
                            failure,
                            7,
                            0.35,
                            0.45,
                            0.35,
                            0.025
                    );

                    world.spawnParticle(
                            Particle.FLAME,
                            failure,
                            7,
                            0.25,
                            0.25,
                            0.25,
                            0.035
                    );

                    world.spawnParticle(
                            Particle.ELECTRIC_SPARK,
                            failure,
                            6,
                            0.3,
                            0.3,
                            0.3,
                            0.08
                    );

                    world.playSound(
                            failure,
                            Sound.BLOCK_FIRE_EXTINGUISH,
                            0.75f,
                            1.15f
                    );
                }

                if(tick == 31) {
                    world.spawnParticle(
                            Particle.LARGE_SMOKE,
                            center,
                            10,
                            0.5,
                            0.7,
                            0.5,
                            0.025
                    );

                    world.spawnParticle(
                            Particle.SMALL_FLAME,
                            center,
                            4,
                            0.35,
                            0.5,
                            0.35,
                            0.02
                    );

                    world.playSound(
                            center,
                            Sound.BLOCK_FIRE_EXTINGUISH,
                            0.45f,
                            0.8f
                    );
                }


                tick++;

                if(tick >= 35) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startPassiveParticles(
            Player player,
            boolean nightBlessing,
            int durationTicks
    ) {
        UUID uuid = player.getUniqueId();

        BukkitTask existingTask =
                passiveParticleTasks.remove(uuid);

        if(existingTask != null) {
            existingTask.cancel();
        }


        BukkitTask task = new BukkitRunnable() {

            private int elapsedTicks = 0;
            private int animationTick = 0;

            @Override
            public void run() {
                if(!player.isOnline()
                        || player.isDead()
                        || elapsedTicks >= durationTicks
                        || !player.hasPotionEffect(
                        PotionEffectType.SLOW_FALLING
                )) {

                    passiveParticleTasks.remove(uuid);
                    cancel();
                    return;
                }

                if(nightBlessing) {
                    playPassiveNightParticles(
                            player,
                            animationTick
                    );
                } else {
                    playPassiveDayParticles(player);
                }

                elapsedTicks += 10;
                animationTick++;
            }
        }.runTaskTimer(plugin, 10L, 10L);


        passiveParticleTasks.put(uuid, task);
    }


    private void playPassiveNightParticles(
            Player player,
            int animationTick
    ) {
        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        World world = player.getWorld();
        Location base = player.getLocation().clone();

        Location star = base.clone().add(
                random.nextDouble(-0.5, 0.5),
                random.nextDouble(0.25, 2.0),
                random.nextDouble(-0.5, 0.5)
        );

        world.spawnParticle(
                Particle.END_ROD,
                star,
                1,
                0.015,
                0.025,
                0.015,
                0.002
        );

        if(animationTick % 2 == 0) {
            Location shimmer = base.clone().add(
                    random.nextDouble(-0.45, 0.45),
                    random.nextDouble(0.25, 1.9),
                    random.nextDouble(-0.45, 0.45)
            );

            world.spawnParticle(
                    Particle.DUST,
                    shimmer,
                    1,
                    0,
                    0,
                    0,
                    0,
                    random.nextBoolean()
                            ? NIGHT_BLUE
                            : NIGHT_LIGHT
            );
        }

        if(animationTick % 5 == 0) {
            Location glow = base.clone().add(
                    random.nextDouble(-0.6, 0.6),
                    random.nextDouble(0.6, 2.15),
                    random.nextDouble(-0.6, 0.6)
            );

            world.spawnParticle(
                    Particle.GLOW,
                    glow,
                    1,
                    0.05,
                    0.05,
                    0.05,
                    0
            );
        }
    }


    private void playPassiveDayParticles(Player player) {
        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        World world = player.getWorld();
        Location base = player.getLocation().clone();

        Location smoke = base.clone().add(
                random.nextDouble(-0.45, 0.45),
                random.nextDouble(0.25, 1.8),
                random.nextDouble(-0.45, 0.45)
        );

        world.spawnParticle(
                Particle.SMOKE,
                smoke,
                1,
                0.04,
                0.06,
                0.04,
                0.01
        );

        if(random.nextDouble() < 0.3) {
            Location flame = base.clone().add(
                    random.nextDouble(-0.45, 0.45),
                    random.nextDouble(0.25, 1.65),
                    random.nextDouble(-0.45, 0.45)
            );

            world.spawnParticle(
                    Particle.SMALL_FLAME,
                    flame,
                    1,
                    0.03,
                    0.04,
                    0.03,
                    0.01
            );
        }

        if(random.nextDouble() < 0.2) {
            Location spark = base.clone().add(
                    random.nextDouble(-0.5, 0.5),
                    random.nextDouble(0.3, 1.9),
                    random.nextDouble(-0.5, 0.5)
            );

            world.spawnParticle(
                    Particle.ELECTRIC_SPARK,
                    spark,
                    1,
                    0.05,
                    0.05,
                    0.05,
                    0.025
            );
        }
    }


    private Location randomPointAroundPlayer(
            Location base,
            ThreadLocalRandom random,
            double minimumRadius,
            double maximumRadius,
            double minimumHeight,
            double maximumHeight
    ) {
        double angle =
                random.nextDouble(0, Math.PI * 2.0);

        double radius =
                random.nextDouble(
                        minimumRadius,
                        maximumRadius
                );

        return base.clone().add(
                Math.cos(angle) * radius,
                random.nextDouble(
                        minimumHeight,
                        maximumHeight
                ),
                Math.sin(angle) * radius
        );
    }

    public void clearCooldown(UUID uuid) {
        nightCooldowns.remove(uuid);
        usedDayBlessingOnDay.remove(uuid);

        BukkitTask cleanupTask = daySunsetCleanupTasks.remove(uuid);
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
    }
}
