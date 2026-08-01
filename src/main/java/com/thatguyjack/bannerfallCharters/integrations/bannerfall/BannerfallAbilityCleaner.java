package com.thatguyjack.bannerfallCharters.integrations.bannerfall;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BannerfallAbilityCleaner {
    private final BannerfallCharters plugin;

    public BannerfallAbilityCleaner(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    public void clearFor(OfflinePlayer target) {
        UUID uuid = target.getUniqueId();

        clearMageSpells(uuid);
        clearKnightAbility(uuid);
        clearRogueAbility(uuid);

        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if(onlinePlayer != null) {
            plugin.bannerfall().getRogueAbilityManager().handleClassChange(onlinePlayer);
        }
    }

    private void clearMageSpells(UUID uuid) {
        plugin.bannerfall().getSpellManager().setPlayerSpells(uuid, List.of());
        plugin.bannerfall().getSpellManager().clearAllCooldowns(uuid);

        try {
            Field activeSpellField = plugin.bannerfall().getSpellManager().getClass().getDeclaredField("playerActiveSpell");
            activeSpellField.setAccessible(true);

            Map<?, ?> activeSpells = (Map<?, ?>) activeSpellField.get(plugin.bannerfall().getSpellManager());
            activeSpells.remove(uuid);

            plugin.bannerfall().getSpellManager().saveAllData();
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Failed to clear active Bannerfall spell for " + uuid + ": " + e);
        }
    }

    private void clearKnightAbility(UUID uuid) {
        try {
            Field selectedAbilitiesField = plugin.bannerfall().getKnightAbilityManager().getClass().getDeclaredField("selectedAbilities");
            selectedAbilitiesField.setAccessible(true);

            Map<?, ?> selectedAbilities  = (Map<?, ?>) selectedAbilitiesField.get(plugin.bannerfall().getKnightAbilityManager());
            selectedAbilities.remove(uuid);

            plugin.bannerfall().getKnightAbilityManager().shutdown();
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Failed to clear Bannerfall knight ability for " + uuid + ": " + exception.getMessage());
        }
    }

    private void clearRogueAbility(UUID uuid) {
        try {
            Field selectedAbilitiesField = plugin.bannerfall().getRogueAbilityManager().getClass().getDeclaredField("selectedAbilities");
            selectedAbilitiesField.setAccessible(true);

            Map<?, ?> selectedAbilities = (Map<?, ?>) selectedAbilitiesField.get(plugin.bannerfall().getRogueAbilityManager());
            selectedAbilities.remove(uuid);

            plugin.bannerfall().getRogueAbilityManager().shutdown();
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Failed to clear Bannerfall rogue ability for " + uuid + ": " + exception.getMessage());
        }
    }
}
