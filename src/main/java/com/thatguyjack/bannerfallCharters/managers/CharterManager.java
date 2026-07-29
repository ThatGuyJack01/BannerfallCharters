package com.thatguyjack.bannerfallCharters.managers;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class CharterManager {
    public final BannerfallCharters plugin;
    private final Map<UUID, String> playerCharters = new HashMap<>();

    public CharterManager(BannerfallCharters plugin) {
        this.plugin = plugin;
    }


    public void load() {
        playerCharters.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("charters.players");
        if(section == null) {
            return;
        }

        for (String uuidString : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String charter = section.getString(uuidString + ".charter");

                if (charter != null && !charter.isBlank()) {
                    playerCharters.put(uuid, charter.toLowerCase());
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in charters.players: " + uuidString);
            }
        }

        plugin.getLogger().info("Loaded " + playerCharters.size() + " player charters.");
    }

    public void save() {
        plugin.getConfig().set("charters.players", null);

        for (Map.Entry<UUID, String> entry : playerCharters.entrySet()) {
            UUID uuid = entry.getKey();
            String charter = entry.getValue();

            String path = "charters.players." + uuid;
            plugin.getConfig().set(path + ".charter", charter);
        }

        plugin.saveConfig();;
    }

    public void setCharacter(OfflinePlayer player, String charterId) {
        UUID uuid = player.getUniqueId();

        playerCharters.put(uuid, charterId.toLowerCase());

        String path = "charters.players." + uuid;
        plugin.getConfig().set(path + ".name", player.getName());
        plugin.getConfig().set(path + ".charter", charterId.toLowerCase());
        plugin.saveConfig();
    }

    public boolean removeCharacter(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();

        boolean existed = playerCharters.remove(uuid) != null;
        plugin.getConfig().set("charters.players." + uuid, null);
        plugin.saveConfig();

        return existed;
    }

    public Optional<String> getCharter(UUID uuid) {
        return Optional.ofNullable(playerCharters.get(uuid));
    }

    public boolean hasCharter(UUID uuid) {
        return playerCharters.containsKey(uuid);
    }

    public Map<UUID, String> getAllCharters() {
        return Map.copyOf(playerCharters);
    }
}
