package com.thatguyjack.bannerfallCharters.charters.jinxitsbinx;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.core.AbilitySlot;
import com.thatguyjack.bannerfallCharters.core.CommandCharterAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class UnicornsBlessingAbility implements CommandCharterAbility {
    private final BannerfallCharters plugin;

    private static final double RADIUS = 6.0;
    private static final double ALLY_HEAL_PER_PULSE = 2.0;
    private static final double ENEMY_DAMAGE_PER_PULSE = 2.0;

    private static final int MAX_CHANNEL_SECONDS = 12;
    private static final int COOLDOWN = 8 * 60;

    private static final Particle.DustOptions DAWN_DUST = new Particle.DustOptions(
            Color.fromRGB(255, 235, 145), 1.25f
    );

    private static final Particle.DustOptions BLESSING_DUST = new Particle.DustOptions(
            Color.fromRGB(255, 190, 240), 1.1f
    );

    private final Map<UUID, BukkitRunnable> activeChannels = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public UnicornsBlessingAbility(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    @Override
    public AbilitySlot slot() {
        return AbilitySlot.MAIN;
    }

    @Override
    public int cooldownSeconds() {
        // cooldown managed internally so it is started when channel ends.
        return 0;
    }

    @Override
    public String id() {
        return "unicorns_blessing";
    }

    @Override
    public String displayName() {
        return "Unicorns Blessing";
    }

    @Override
    public boolean activate(Player player) {
        UUID playerId = player.getUniqueId();

        if(activeChannels.containsKey(playerId)) {
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Unicorn's Blessing is already active.");
            return false;
        }

        long remainingMillis = getRemainingCooldownMillis(playerId);

        if (remainingMillis > 0) {
            long remainingSeconds = (remainingMillis + 999) / 1000;
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;

            player.sendMessage(ChatColor.RED + "Unicorn's blessing is on cooldown for " + minutes + "m " + seconds + "s.");
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

        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 1.35f);
        caster.getWorld().playSound(caster.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.7f, 1.6f);

        BukkitRunnable channel = new BukkitRunnable() {
            private int seconds = 0;

            @Override
            public void run() {
                if(!caster.isOnline() || caster.isDead()) {
                    endChannel(casterId, false);
                    cancel();
                    return;
                }

                pulse(caster);
                seconds++;

                if(seconds >= MAX_CHANNEL_SECONDS) {
                    endChannel(casterId, true);
                    cancel();
                }
            }
        };

        activeChannels.put(casterId, channel);
        channel.runTaskTimer(plugin, 0L, 20L);
    }

    private void pulse(Player caster) {
        Location center = caster.getLocation();
        World world = caster.getWorld();

        spawnRangeRing(center);
        spawnPulseBurst(center);

        Team casterTeam = getKingdomTeam(caster);

        if (casterTeam == null) {
            caster.sendMessage(ChatColor.RED + "Unicorn's Blessing faded because you are not in a kingdom.");
            cancelChannel(caster);
            return;
        }

        for(Player target : Bukkit.getOnlinePlayers()) {
            if (!target.getWorld().equals(world)) {
                continue;
            }

            if(target.getLocation().distanceSquared(center) > RADIUS * RADIUS) {
                continue;
            }

            Team targetTeam = getKingdomTeam(target);

            if (targetTeam == null) {
                continue;
            }

            if(targetTeam.equals(casterTeam)) {
                blessAlly(target);
            } else {
                harmEnemy(caster, target);
            }
        }

        world.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.8f);
    }

    private void blessAlly(Player ally) {
        double newHealth = Math.min(ally.getMaxHealth(), ally.getHealth() + ALLY_HEAL_PER_PULSE);
        ally.setHealth(newHealth);

        ally.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                45,
                0,
                true,
                true,
                true
        ));

        ally.getWorld().spawnParticle(
                Particle.HEART,
                ally.getLocation().add(0, 1.15, 0),
                2,
                0.35,
                0.45,
                0.35,
                0.02
        );

        ally.getWorld().spawnParticle(
                Particle.DUST,
                ally.getLocation().add(0, 1.0, 0),
                8,
                0.35,
                0.55,
                0.35,
                0.0,
                BLESSING_DUST
        );
    }

    private void harmEnemy(Player caster, Player enemy) {
        enemy.damage(ENEMY_DAMAGE_PER_PULSE, caster);

        Vector knockback = enemy.getLocation().toVector().subtract(caster.getLocation().toVector()).setY(0);

        if (knockback.lengthSquared() > 0.001) {
            knockback.normalize().multiply(0.12);
            knockback.setY(0.05);
            enemy.setVelocity(enemy.getVelocity().add(knockback));
        }

        enemy.getWorld().spawnParticle(
                Particle.DAMAGE_INDICATOR,
                enemy.getLocation().add(0, 1.0, 0),
                3,
                0.25,
                0.35,
                0.25,
                0.03
        );

        enemy.getWorld().spawnParticle(
                Particle.DUST,
                enemy.getLocation().add(0, 1.0, 0),
                8,
                0.35,
                0.55,
                0.35,
                0.0,
                DAWN_DUST
        );
    }

    private void spawnRangeRing(Location center) {
        World world = center.getWorld();

        if (world == null) {
             return;
        }

        Location base = center.clone().add(0, 0.15, 0);

        int points = 72;

        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0 * i) / points;
            double x = Math.cos(angle) * RADIUS;
            double z = Math.sin(angle) * RADIUS;

            Location point = base.clone().add(x, 0, z);

            Particle.DustOptions dust = i % 2 == 0 ? BLESSING_DUST : DAWN_DUST;

            world.spawnParticle(
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

    private void spawnPulseBurst(Location center)  {
        World world = center.getWorld();

        if (world == null) {
            return;
        }

        Location burstCenter = center.clone().add(0, 0.1, 0);

        world.spawnParticle(
                Particle.END_ROD,
                burstCenter,
                22,
                1.2,
                0.6,
                1.2,
                0.035
        );

        world.spawnParticle(
                Particle.DUST,
                burstCenter,
                32,
                1.5,
                0.75,
                1.5,
                0.0,
                BLESSING_DUST
        );
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

    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
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
