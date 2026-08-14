package com.thatguyjack.bannerfallCharters.charters.jinxitsbinx;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.core.AbilitySlot;
import com.thatguyjack.bannerfallCharters.core.CommandCharterAbility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UnicornsBlessingAbility implements CommandCharterAbility, Listener {
    private final BannerfallCharters plugin;

    private static final double RADIUS = 6.0;
    private static final double ALLY_HEAL_PER_PULSE = 2.0;
    private static final double ENEMY_DAMAGE_PER_PULSE = 2.0;

    private static final int MAX_CHANNEL_SECONDS = 12;
    private static final int MAX_CHANNEL_TICKS = MAX_CHANNEL_SECONDS * 20;
    private static final int PULSE_INTERVAL_TICKS = 40;
    private static final int COOLDOWN = 8 * 60;

    private static final boolean REQUIRES_CONCENTRATION = true;
    private static final boolean CASTER_TOTEM_WHILE_CHANNELING = false;

    private static final double CONCENTRATION_MOVE_EPSILON = 0.0001;
    private static final double ENEMY_KNOCKBACK_HORIZONTAL = 0.035;
    private static final double ENEMY_KNOCKBACK_VERTICAL = 0.01;

    private static final double TOTEM_SAVE_HEALTH = 6.0;
    private static final int TOTEM_REGEN_TICKS = 20 * 4;
    private static final int TOTEM_ABSORPTION_TICKS = 20 * 5;
    private static final int TOTEM_RESISTANCE_TICKS = 20 * 3;

    private static final Color[] UNICORN_COLORS = new Color[] {
            Color.fromRGB(255, 120, 210),
            Color.fromRGB(255, 180, 235),
            Color.fromRGB(255, 245, 155),
            Color.fromRGB(165, 255, 220),
            Color.fromRGB(135, 220, 255),
            Color.fromRGB(190, 155, 255)
    };

    private final Map<UUID, BukkitRunnable> activeChannels = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public UnicornsBlessingAbility(BannerfallCharters plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

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
        return "unicorns_blessing";
    }

    @Override
    public String displayName() {
        return "Unicorn's Blessing";
    }

    @Override
    public boolean activate(Player player) {
        UUID playerId = player.getUniqueId();

        if (activeChannels.containsKey(playerId)) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Unicorn's Blessing is already active.");
            return false;
        }

        long remainingMillis = getRemainingCooldownMillis(playerId);

        if (remainingMillis > 0 && player.getGameMode() != GameMode.CREATIVE) {
            long remainingSeconds = (remainingMillis + 999) / 1000;
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;

            player.sendMessage(ChatColor.RED + "Unicorn's Blessing is on cooldown for " + minutes + "m " + seconds + "s.");
            return false;
        }

        if (getKingdomTeam(player) == null) {
            player.sendMessage(ChatColor.RED + "You must be in a kingdom to use Unicorn's Blessing.");
            return false;
        }

        startChannel(player);
        return true;
    }

    public CommandCharterAbility createCancelAbility() {
        return new UnicornsBlessingCancelAbility();
    }

    private void startChannel(Player caster) {
        UUID casterId = caster.getUniqueId();

        caster.sendMessage(ChatColor.LIGHT_PURPLE + "You begin channeling "
                + ChatColor.WHITE + "Unicorn's Blessing" + ChatColor.LIGHT_PURPLE + ".");

        if (REQUIRES_CONCENTRATION) {
            caster.sendMessage(ChatColor.GRAY + "Concentrate. Your movement is locked while the blessing is active.");
        }

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.35f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.6f);

        BukkitRunnable channel = new BukkitRunnable() {
            private int ticks = 0;

            @Override
            public void run() {
                if (!caster.isOnline() || caster.isDead()) {
                    endChannel(casterId, false);
                    cancel();
                    return;
                }

                if (getKingdomTeam(caster) == null) {
                    caster.sendMessage(ChatColor.RED + "Unicorn's Blessing faded because you are not in a kingdom.");
                    endChannel(casterId, false);
                    cancel();
                    return;
                }

                renderChannelVisuals(caster, ticks);

                if (ticks % PULSE_INTERVAL_TICKS == 0) {
                    pulse(caster);
                }

                ticks++;

                if (ticks >= MAX_CHANNEL_TICKS) {
                    endChannel(casterId, true);
                    cancel();
                }
            }
        };

        activeChannels.put(casterId, channel);
        channel.runTaskTimer(plugin, 0L, 1L);
    }

    private void pulse(Player caster) {
        Location center = caster.getLocation();
        World world = caster.getWorld();

        Team casterTeam = getKingdomTeam(caster);

        if (casterTeam == null) {
            caster.sendMessage(ChatColor.RED + "Unicorn's Blessing faded because you are not in a kingdom.");
            cancelChannel(caster);
            return;
        }

        spawnPulseImpact(center);

        for (Player target : Bukkit.getOnlinePlayers()) {
            if (!target.getWorld().equals(world)) {
                continue;
            }

            if (target.getLocation().distanceSquared(center) > RADIUS * RADIUS) {
                continue;
            }

            Team targetTeam = getKingdomTeam(target);

            if (targetTeam == null) {
                continue;
            }

            if (targetTeam.equals(casterTeam)) {
                blessAlly(target);
            } else {
                harmEnemy(caster, target);
            }
        }

        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.65f);
        world.playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_STEP, 0.45f, 1.8f);
    }

    private void blessAlly(Player ally) {
        double newHealth = Math.min(ally.getMaxHealth(), ally.getHealth() + ALLY_HEAL_PER_PULSE);
        ally.setHealth(newHealth);

        ally.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                55,
                0,
                true,
                true,
                true
        ));

        Location location = ally.getLocation().add(0, 1.15, 0);
        World world = ally.getWorld();

        world.spawnParticle(
                Particle.HEART,
                location,
                2,
                0.35,
                0.45,
                0.35,
                0.02
        );

        world.spawnParticle(
                Particle.END_ROD,
                location,
                5,
                0.25,
                0.35,
                0.25,
                0.02
        );

        spawnRainbowCloud(location, 10, 0.35, 0.55, 0.35);
    }

    private void harmEnemy(Player caster, Player enemy) {
        enemy.damage(ENEMY_DAMAGE_PER_PULSE, caster);

        Vector knockback = enemy.getLocation().toVector()
                .subtract(caster.getLocation().toVector())
                .setY(0);

        if (knockback.lengthSquared() > 0.001) {
            knockback.normalize().multiply(ENEMY_KNOCKBACK_HORIZONTAL);
            knockback.setY(ENEMY_KNOCKBACK_VERTICAL);
            enemy.setVelocity(enemy.getVelocity().add(knockback));
        }

        Location location = enemy.getLocation().add(0, 1.0, 0);
        World world = enemy.getWorld();

        world.spawnParticle(
                Particle.DAMAGE_INDICATOR,
                location,
                3,
                0.25,
                0.35,
                0.25,
                0.03
        );

        world.spawnParticle(
                Particle.CRIT,
                location,
                6,
                0.3,
                0.45,
                0.3,
                0.035
        );

        spawnRainbowCloud(location, 8, 0.35, 0.5, 0.35);
    }

    private void renderChannelVisuals(Player caster, int ticks) {
        Location center = caster.getLocation();

        spawnOuterRangeCircle(center, ticks);
        spawnGrowingPulseCircle(center, ticks);
        spawnCasterSparkles(caster, ticks);
    }

    private void spawnOuterRangeCircle(Location center, int ticks) {
        World world = center.getWorld();

        if (world == null) {
            return;
        }

        Location base = center.clone().add(0, 0.12, 0);
        int points = 72;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = Math.cos(angle) * RADIUS;
            double z = Math.sin(angle) * RADIUS;

            Location point = base.clone().add(x, 0, z);

            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.01,
                    0.01,
                    0.01,
                    0.0,
                    rainbowDust(i + ticks / 2L, 1.0f)
            );
        }
    }

    private void spawnGrowingPulseCircle(Location center, int ticks) {
        World world = center.getWorld();

        if (world == null) {
            return;
        }

        int pulseTick = ticks % PULSE_INTERVAL_TICKS;
        double progress = pulseTick / (double) PULSE_INTERVAL_TICKS;

        double pulseRadius = Math.max(0.15, RADIUS * progress);
        double yOffset = 0.18 + progress * 0.15;

        Location base = center.clone().add(0, yOffset, 0);

        int points = Math.max(12, (int) (pulseRadius * 14));

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = Math.cos(angle) * pulseRadius;
            double z = Math.sin(angle) * pulseRadius;

            Location point = base.clone().add(x, 0, z);

            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0.015,
                    0.015,
                    0.015,
                    0.0,
                    rainbowDust(i + ticks * 2L, 1.25f)
            );
        }

        if (pulseTick == 0) {
            world.spawnParticle(
                    Particle.END_ROD,
                    center.clone().add(0, 1.0, 0),
                    18,
                    0.75,
                    0.45,
                    0.75,
                    0.035
            );
        }
    }

    private void spawnPulseImpact(Location center) {
        World world = center.getWorld();

        if (world == null) {
            return;
        }

        Location burstCenter = center.clone().add(0, 1.0, 0);

        world.spawnParticle(
                Particle.END_ROD,
                burstCenter,
                16,
                0.9,
                0.45,
                0.9,
                0.035
        );

        spawnRainbowCloud(burstCenter, 28, 1.0, 0.55, 1.0);
    }

    private void spawnCasterSparkles(Player caster, int ticks) {
        World world = caster.getWorld();
        Location center = caster.getLocation().add(0, 1.25, 0);

        if (ticks % 2 == 0) {
            world.spawnParticle(
                    Particle.END_ROD,
                    center,
                    2,
                    0.25,
                    0.55,
                    0.25,
                    0.02
            );
        }

        if (ticks % 5 == 0) {
            spawnRainbowCloud(center, 8, 0.35, 0.75, 0.35);
        }
    }

    private void spawnRainbowCloud(Location location, int count, double offsetX, double offsetY, double offsetZ) {
        World world = location.getWorld();

        if (world == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            world.spawnParticle(
                    Particle.DUST,
                    location,
                    1,
                    offsetX,
                    offsetY,
                    offsetZ,
                    0.0,
                    rainbowDust(i + world.getGameTime(), 1.0f)
            );
        }
    }

    private Particle.DustOptions rainbowDust(long index, float size) {
        Color color = UNICORN_COLORS[(int) (Math.abs(index) % UNICORN_COLORS.length)];
        return new Particle.DustOptions(color, size);
    }

    private Team getKingdomTeam(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        Team blueTeam = scoreboard.getTeam("bf_blue");
        Team redTeam = scoreboard.getTeam("bf_red");

        String name = player.getName();

        if (blueTeam != null && blueTeam.hasEntry(name)) {
            return blueTeam;
        }

        if (redTeam != null && redTeam.hasEntry(name)) {
            return redTeam;
        }

        return null;
    }

    private long getRemainingCooldownMillis(UUID playerId) {
        Long cooldownEnd = cooldowns.get(playerId);

        if (cooldownEnd == null) {
            return 0;
        }

        long remaining = cooldownEnd - System.currentTimeMillis();

        if (remaining <= 0) {
            cooldowns.remove(playerId);
            return 0;
        }

        return remaining;
    }

    private void endChannel(UUID casterId, boolean showEndMessage) {
        activeChannels.remove(casterId);
        Player caster = Bukkit.getPlayer(casterId);

        if (caster == null || caster.getGameMode() != GameMode.CREATIVE) {
            cooldowns.put(casterId, System.currentTimeMillis() + COOLDOWN * 1000L);
        }

        if (caster != null && caster.isOnline()) {
            if (showEndMessage) {
                caster.sendMessage(ChatColor.LIGHT_PURPLE + "Unicorn's Blessing fades.");
            }

            caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.55f, 1.7f);
        }
    }

    private void cancelChannel(Player caster) {
        UUID casterId = caster.getUniqueId();

        BukkitRunnable channel = activeChannels.remove(casterId);

        if (channel == null) {
            return;
        }

        channel.cancel();

        if (caster.getGameMode() != GameMode.CREATIVE) {
            cooldowns.put(casterId, System.currentTimeMillis() + COOLDOWN * 1000L);
        }

        caster.sendMessage(ChatColor.LIGHT_PURPLE + "You cancel Unicorn's Blessing.");
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.55f, 1.35f);
    }

    private void totemSave(Player player) {
        double saveHealth = Math.min(player.getMaxHealth(), TOTEM_SAVE_HEALTH);

        player.setHealth(saveHealth);
        player.setFireTicks(0);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                TOTEM_REGEN_TICKS,
                1,
                true,
                true,
                true
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                TOTEM_ABSORPTION_TICKS,
                1,
                true,
                true,
                true
        ));

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                TOTEM_RESISTANCE_TICKS,
                0,
                true,
                true,
                true
        ));

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.3f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.5f, 1.0f);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Unicorn's Blessing shatters to save you.");
    }

    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!REQUIRES_CONCENTRATION) {
            return;
        }

        Player player = event.getPlayer();

        if (!activeChannels.containsKey(player.getUniqueId())) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        double dx = from.getX() - to.getX();
        double dy = from.getY() - to.getY();
        double dz = from.getZ() - to.getZ();

        double movedDistanceSquared = dx * dx + dy * dy + dz * dz;

        if (movedDistanceSquared <= CONCENTRATION_MOVE_EPSILON) {
            return;
        }

        event.setTo(new Location(
                from.getWorld(),
                from.getX(),
                from.getY(),
                from.getZ(),
                to.getYaw(),
                to.getPitch()
        ));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!activeChannels.containsKey(player.getUniqueId())) {
            return;
        }

        cancelChannel(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCasterFatalDamage(EntityDamageEvent event) {
        if (!CASTER_TOTEM_WHILE_CHANNELING) {
            return;
        }

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!activeChannels.containsKey(player.getUniqueId())) {
            return;
        }

        if (player.getHealth() - event.getFinalDamage() > 0) {
            return;
        }

        event.setCancelled(true);
        totemSave(player);
        cancelChannel(player);
    }

    private class UnicornsBlessingCancelAbility implements CommandCharterAbility {

        @Override
        public AbilitySlot slot() {
            return AbilitySlot.BREAK;
        }

        @Override
        public int cooldownSeconds() {
            return 0;
        }

        @Override
        public String id() {
            return "unicorns_blessing_cancel";
        }

        @Override
        public String displayName() {
            return "Cancel Unicorn's Blessing";
        }

        @Override
        public boolean activate(Player player) {
            if (!activeChannels.containsKey(player.getUniqueId())) {
                return false;
            }

            cancelChannel(player);
            return true;
        }
    }
}