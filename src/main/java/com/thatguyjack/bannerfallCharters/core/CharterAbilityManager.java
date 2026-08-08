package com.thatguyjack.bannerfallCharters.core;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class CharterAbilityManager {
    private final BannerfallCharters plugin;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    public CharterAbilityManager(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    public void activate(Player player, AbilitySlot slot) {
        String charterId = plugin.charterManager().getCharter(player.getUniqueId()).orElse(null);

        if(charterId == null) {
            player.sendMessage(ChatColor.RED + "You don't have a charter.");
            return;
        }

        Charter charter = plugin.charterRegistry().getCharter(charterId).orElse(null);
        if(charter == null) {
            player.sendMessage(ChatColor.RED + "Your charter is not registered.");
            return;
        }

        boolean isDebugPower = charterId.equalsIgnoreCase("thatguyjack");

        if(player.getGameMode() == GameMode.SPECTATOR && !isDebugPower) {
            player.sendMessage(ChatColor.RED + "You cant activate this ability in spectator mode.");
            return;
        }

        CommandCharterAbility ability = null;

        for (CharterAbility charterAbility : charter.abilities()) {
            if (charterAbility instanceof CommandCharterAbility commandAbility
                    && commandAbility.slot() == slot) {
                ability = commandAbility;
                break;
            }
        }

        if (ability == null) {
            player.sendMessage(ChatColor.RED + "Your charter does not have an ability in that slot.");
            return;
        }

        String normalizedAbilityId = ability.id().toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();

        if(player.getGameMode() != GameMode.CREATIVE) {
            Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
            long cooldownEnd = playerCooldowns == null ? 0L : playerCooldowns.getOrDefault(normalizedAbilityId, 0L);

            if (cooldownEnd > now) {
                long secondsLeft = (cooldownEnd - now + 999) / 1000;
                player.sendMessage(ChatColor.RED + "That ability is on cooldown for " + secondsLeft + "s.");
                return;
            }

        }

        boolean success = ability.activate(player);

        if(success && player.getGameMode() != GameMode.CREATIVE) {
            cooldowns
                    .computeIfAbsent(player.getUniqueId(), uuid -> new HashMap<>())
                    .put(normalizedAbilityId, now + ability.cooldownSeconds() * 1000L);
        }
    }

}
