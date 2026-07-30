package com.thatguyjack.bannerfallCharters.abilities.implemented;

import com.thatguyjack.bannerfallCharters.abilities.TickingCharterAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TestWaterGlowPassive implements TickingCharterAbility {

    @Override
    public String id() {
        return "water_glow";
    }

    @Override
    public String displayName() {
        return "Water Glow";
    }

    @Override
    public void tick(Player player) {
        Material feetBlock = player.getLocation().getBlock().getType();

        if(feetBlock == Material.WATER) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.GLOWING,
                    40,
                    0,
                    false,
                    false,
                    true
            ));
        }
    }
}
