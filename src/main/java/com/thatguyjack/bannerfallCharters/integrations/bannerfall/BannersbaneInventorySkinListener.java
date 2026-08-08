package com.thatguyjack.bannerfallCharters.listeners;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.integrations.bannerfall.BannersbaneTextSkin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BannersbaneInventorySkinListener implements Listener {
    private final BannerfallCharters plugin;

    public BannersbaneInventorySkinListener(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (!isBannerfallInventory(title)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            Inventory inventory = event.getInventory();

            if (title.equalsIgnoreCase("Choose Your Kingdom")) {
                skinChooseKingdomInventory(inventory);
                return;
            }

            skinGenericBannerfallInventory(inventory);
        });
    }

    private boolean isBannerfallInventory(String title) {
        if (title == null) {
            return false;
        }

        return title.equalsIgnoreCase("Choose Your Kingdom")
                || title.contains("Kingdom")
                || title.contains("Bannerfall")
                || title.contains("Blue")
                || title.contains("Red");
    }

    private void skinChooseKingdomInventory(Inventory inventory) {
        skinGenericBannerfallInventory(inventory);

        inventory.setItem(2, createDawnItem());
        inventory.setItem(6, createDuskItem());
    }

    private ItemStack createDawnItem() {
        ItemStack item = new ItemStack(Material.YELLOW_BANNER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Dawn", NamedTextColor.YELLOW));
        meta.lore(List.of(
                Component.text("Join the Dawn.", NamedTextColor.GRAY),
                Component.text("Click to select", NamedTextColor.GRAY)
        ));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDuskItem() {
        ItemStack item = new ItemStack(Material.BLUE_BANNER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Dusk", NamedTextColor.BLUE));
        meta.lore(List.of(
                Component.text("Join the Dusk.", NamedTextColor.GRAY),
                Component.text("Click to select", NamedTextColor.GRAY)
        ));

        item.setItemMeta(meta);
        return item;
    }

    private void skinGenericBannerfallInventory(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);

            if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
                continue;
            }

            ItemStack skinned = skinItem(item);

            if (skinned != item) {
                inventory.setItem(slot, skinned);
            }
        }
    }

    private ItemStack skinItem(ItemStack original) {
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();

        boolean changed = false;

        if (meta.hasDisplayName()) {
            String plainName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            String skinnedName = BannersbaneTextSkin.apply(plainName);

            if (!plainName.equals(skinnedName)) {
                meta.displayName(colorSkinnedText(skinnedName));
                changed = true;
            }
        }

        if (meta.hasLore() && meta.lore() != null) {
            List<Component> newLore = new ArrayList<>();

            for (Component line : meta.lore()) {
                String plainLine = PlainTextComponentSerializer.plainText().serialize(line);
                String skinnedLine = BannersbaneTextSkin.apply(plainLine);

                if (!plainLine.equals(skinnedLine)) {
                    changed = true;
                }

                newLore.add(Component.text(skinnedLine, NamedTextColor.GRAY));
            }

            meta.lore(newLore);
        }

        Material replacementMaterial = getReplacementMaterial(item.getType());
        if (replacementMaterial != item.getType()) {
            item.setType(replacementMaterial);
            changed = true;
        }

        if (!changed) {
            return original;
        }

        item.setItemMeta(meta);
        return item;
    }

    private Component colorSkinnedText(String text) {
        if (text == null) {
            return Component.empty();
        }

        if (text.contains("Dawn")) {
            return Component.text(text, NamedTextColor.YELLOW);
        }

        if (text.contains("Dusk")) {
            return Component.text(text, NamedTextColor.BLUE);
        }

        return Component.text(text, NamedTextColor.WHITE);
    }

    private Material getReplacementMaterial(Material material) {
        return switch (material) {
            case BLUE_BANNER -> Material.YELLOW_BANNER;
            case RED_BANNER -> Material.BLUE_BANNER;
            case BLUE_WOOL -> Material.YELLOW_WOOL;
            case RED_WOOL -> Material.BLUE_WOOL;
            case BLUE_STAINED_GLASS_PANE -> Material.YELLOW_STAINED_GLASS_PANE;
            case RED_STAINED_GLASS_PANE -> Material.BLUE_STAINED_GLASS_PANE;
            case BLUE_CONCRETE -> Material.YELLOW_CONCRETE;
            case RED_CONCRETE -> Material.BLUE_CONCRETE;
            default -> material;
        };
    }
}