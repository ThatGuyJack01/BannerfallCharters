package com.thatguyjack.bannerfallCharters.listeners;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class BannerfallAbilityBlockerListener implements Listener {
    private final BannerfallCharters plugin;

    public BannerfallAbilityBlockerListener(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClicked(InventoryClickEvent event) {
        if(!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if(plugin.charterManager().getCharter(player.getUniqueId()).isEmpty()) {
            return;
        }

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if(!title.equals("Knight Abilities") && !title.equals("Rogue Abilities")) {
            return;
        }

        event.setCancelled(true);

        player.sendMessage(ChatColor.RED + "You cannot select a Bannerfall ability while you have an active charter.");

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.bannerfallAbilityCleaner().clearFor(player);
            player.closeInventory();
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (plugin.charterManager().getCharter(player.getUniqueId()).isPresent()) {
            plugin.bannerfallAbilityCleaner().clearFor(player);
        }
    }
}
