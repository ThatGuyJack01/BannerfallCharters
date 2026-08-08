package com.thatguyjack.bannerfallCharters.core;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.charters.loserboy.AerialAceAbility;
import com.thatguyjack.bannerfallCharters.charters.photopho.CanaryCollisionAbility;
import com.thatguyjack.bannerfallCharters.charters.test.TestPoofAbility;
import com.thatguyjack.bannerfallCharters.charters.test.TestWaterGlowPassive;

import java.util.*;

public final class CharterRegistry {
    private final BannerfallCharters plugin;
    private final Map<String, Charter> charters = new HashMap<>();

    public CharterRegistry(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    public void registerDefaults() {
//        register(new Charter(
//                "test",
//                "Test Charter",
//                List.of(
//                        new TestPoofAbility(),
//                        new TestWaterGlowPassive()
//                )
//        ));
        register(new Charter(
                "photopho",
                "Photopho",
                List.of(
                        new CanaryCollisionAbility(plugin)
                )
        ));
        register(new Charter(
                "loserboy",
                "Loserboy",
                List.of(
                        new AerialAceAbility(plugin)
                )
        ));
    }

    public void register(Charter charter) {
        charters.put(charter.id().toLowerCase(Locale.ROOT), charter);
    }

    public Optional<Charter> getCharter(String id) {
        return Optional.ofNullable(charters.get(id.toLowerCase(Locale.ROOT)));
    }

    public boolean exists(String id) {
        return charters.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Charter> getAllCharters() {
        return charters.values();
    }
}
