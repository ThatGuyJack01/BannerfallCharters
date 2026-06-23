package com.thatguyjack.bannerfallCharters;

import com.scheduler.Scheduler;
import com.thatguyjack.bannerfallCharters.commands.CharterCommand;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pow.bannerfall.Bannerfall;
import pow.bannerfall.dungeonfall.DungeonFall;

public final class BannerfallCharters extends JavaPlugin {
    private Bannerfall bannerfall;
    private DungeonFall dungeonFall;
    private Scheduler scheduler;


    @Override
    public void onEnable() {
        // saveDefaultConfig();

        this.bannerfall = requirePlugin("Bannerfall", Bannerfall.class);
        this.dungeonFall = requirePlugin("DungeonFall", DungeonFall.class);
        this.scheduler = requirePlugin("Scheduler", Scheduler.class);

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

}
