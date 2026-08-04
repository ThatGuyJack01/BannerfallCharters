package com.thatguyjack.bannerfallCharters;

import com.scheduler.Scheduler;
import com.thatguyjack.bannerfallCharters.core.CharterAbilityManager;
import com.thatguyjack.bannerfallCharters.core.CharterPassiveTicker;
import com.thatguyjack.bannerfallCharters.core.CharterRegistry;
import com.thatguyjack.bannerfallCharters.commands.CharterCommand;
import com.thatguyjack.bannerfallCharters.core.CharterManager;
import com.thatguyjack.bannerfallCharters.integrations.bannerfall.BannerfallFallDamageImmunityManager;
import com.thatguyjack.bannerfallCharters.listeners.AbilityCommandListener;
import com.thatguyjack.bannerfallCharters.listeners.BannerfallAbilityBlockerListener;
import com.thatguyjack.bannerfallCharters.integrations.bannerfall.BannerfallAbilityCleaner;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pow.bannerfall.Bannerfall;
import pow.bannerfall.dungeonfall.DungeonFall;

public final class BannerfallCharters extends JavaPlugin {
    private Bannerfall bannerfall;
    private DungeonFall dungeonFall;
    private Scheduler scheduler;

    private CharterManager charterManager;
    private CharterRegistry charterRegistry;

    private CharterAbilityManager charterAbilityManager;
    private BannerfallAbilityCleaner bannerfallAbilityCleaner;

    private BannerfallFallDamageImmunityManager bannerfallFallDamageImmunityManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.bannerfall = requirePlugin("Bannerfall", Bannerfall.class);
        this.dungeonFall = requirePlugin("DungeonFall", DungeonFall.class);
        this.scheduler = requirePlugin("Scheduler", Scheduler.class);

        this.charterManager = new CharterManager(this);
        this.charterManager.load();

        this.charterRegistry = new CharterRegistry(this);
        this.charterRegistry.registerDefaults();

        this.charterAbilityManager = new CharterAbilityManager(this);
        this.bannerfallAbilityCleaner = new BannerfallAbilityCleaner(this);

        this.bannerfallFallDamageImmunityManager = new BannerfallFallDamageImmunityManager(this);

        new CharterPassiveTicker(this).runTaskTimer(this, 20L, 20L);

        getServer().getPluginManager().registerEvents(new BannerfallAbilityBlockerListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityCommandListener(this), this);

        CharterCommand charterCommand = new CharterCommand(this);
        PluginCommand pluginCommand = getCommand("charter");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(charterCommand);
            pluginCommand.setTabCompleter(charterCommand);
        }

        getLogger().info("Bannerfall Charter enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Bannerfall Charter disabled.");
    }

    private <T extends Plugin> T requirePlugin(String name, Class<T> type) {
        Plugin plugin = getServer().getPluginManager().getPlugin(name);
        if(!type.isInstance(plugin)) {
            throw new IllegalStateException(name + " is required but was not loaded as expected.");
        }
        return type.cast(plugin);
    }

    public Bannerfall bannerfall() {
        return bannerfall;
    }

    public DungeonFall dungeonFall() {
        return dungeonFall;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public CharterManager charterManager() {
        return charterManager;
    }

    public CharterRegistry charterRegistry() {
        return charterRegistry;
    }

    public CharterAbilityManager charterAbilityManager() {
        return charterAbilityManager;
    }

    public BannerfallAbilityCleaner bannerfallAbilityCleaner() {
        return bannerfallAbilityCleaner;
    }

    public BannerfallFallDamageImmunityManager bannerfallFallDamageImmunityManager() {
        return bannerfallFallDamageImmunityManager;
    }
}
