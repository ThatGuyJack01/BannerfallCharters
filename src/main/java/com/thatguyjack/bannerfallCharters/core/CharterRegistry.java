package com.thatguyjack.bannerfallCharters.core;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import com.thatguyjack.bannerfallCharters.charters.bulldozerplays.BlessingOfTheStarsAbility;
import com.thatguyjack.bannerfallCharters.charters.jinxitsbinx.UnicornsBlessingAbility;
import com.thatguyjack.bannerfallCharters.charters.loserboy.AerialAceAbility;
import com.thatguyjack.bannerfallCharters.charters.photopho.CanaryCollisionAbility;
import com.thatguyjack.bannerfallCharters.charters.test.TestPoofAbility;
import com.thatguyjack.bannerfallCharters.charters.test.TestWaterGlowPassive;
import com.thatguyjack.bannerfallCharters.charters.thatguyjack.ModeToggleAbility;

import java.util.*;

public final class CharterRegistry {
    private final BannerfallCharters plugin;
    private final Map<String, Charter> charters = new HashMap<>();
    private final Map<String, String> customSetMessages = new HashMap<>();

    public CharterRegistry(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    public void registerDefaults() {
        UnicornsBlessingAbility unicornsBlessingAbility = new UnicornsBlessingAbility(plugin);

//        register(new Charter(
//                "test",
//                "Test Charter",
//                List.of(
//                        new TestPoofAbility(),
//                        new TestWaterGlowPassive()
//                )
//        ));
        register(new Charter(
                "thatguyjack",
                "ThatGuyJack",
                List.of(
                        new ModeToggleAbility()
                )
        ));
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
        register(new Charter(
                "bulldozerplays",
                "Bulldozerplays",
                List.of(
                    new BlessingOfTheStarsAbility(plugin)
                )
        ));
//        register(new Charter(
//                "jinxitsbinx",
//                "Jinxitsbinx",
//                List.of(
//                        unicornsBlessingAbility,
//                        unicornsBlessingAbility.createCancelAbility()
//                )
//        ));


        registerCustomSetMessage(
                "bulldozerplays",
                "&r&9✦ &fThe stars twinkle above you. &7Something unseen stirs within you, but its meaning remains unclear."
        );
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

    public void registerCustomSetMessage(String charterId, String message) {
        customSetMessages.put(charterId.toLowerCase(Locale.ROOT), message);
    }

    public Optional<String> getCustomSetMessage(String charterId) {
        return Optional.ofNullable(customSetMessages.get(charterId.toLowerCase(Locale.ROOT)));
    }

    public boolean hasCustomSetMessage(String charterId) {
        return customSetMessages.containsKey(charterId.toLowerCase(Locale.ROOT));
    }
}
