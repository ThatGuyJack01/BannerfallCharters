package com.thatguyjack.bannerfallCharters.core;

import java.util.List;
import java.util.Locale;

public final class Charter {
    private final String id;
    private final String displayName;
    private final List<CharterAbility> abilities;

    public Charter(String id, String displayName, List<CharterAbility> abilities) {
        this.id = id.toLowerCase(Locale.ROOT);
        this.displayName = displayName;
        this.abilities = List.copyOf(abilities);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public List<CharterAbility> abilities() {
        return abilities;
    }
}
