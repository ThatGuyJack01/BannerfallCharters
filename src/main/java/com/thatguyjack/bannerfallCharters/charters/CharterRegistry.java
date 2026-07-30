package com.thatguyjack.bannerfallCharters.charters;

import com.thatguyjack.bannerfallCharters.abilities.implemented.TestPoofAbility;
import com.thatguyjack.bannerfallCharters.abilities.implemented.TestWaterGlowPassive;

import java.util.*;

public final class CharterRegistry {
    private final Map<String, Charter> charters = new HashMap<>();

    public void registerDefaults() {
        register(new Charter(
                "test",
                "Test Charter",
                List.of(
                        new TestPoofAbility(),
                        new TestWaterGlowPassive()
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
