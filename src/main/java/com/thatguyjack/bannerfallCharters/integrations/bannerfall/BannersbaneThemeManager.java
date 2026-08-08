package com.thatguyjack.bannerfallCharters.integrations.bannerfall;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;

public class BannersbaneThemeManager {
    private final BannerfallCharters plugin;

    public BannersbaneThemeManager(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    public void apply() {
        patchBannerfallTeamEnum();
        patchScoreboardTeams();
    }

    private void patchBannerfallTeamEnum() {
        try {
            setEnumField(pow.bannerfall.Team.BLUE, "displayName", "Dawn");
            setEnumField(pow.bannerfall.Team.BLUE, "colorCode", "§e");

            setEnumField(pow.bannerfall.Team.RED, "displayName", "Dusk");
            setEnumField(pow.bannerfall.Team.RED, "colorCode", "§9");

            plugin.getLogger().info("Applied Bannersbane kingdom names/colors.");
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Could not patch Bannerfall team enum: " + exception.getMessage());
        }
    }

    private void setEnumField(pow.bannerfall.Team team, String fieldName, String value) throws ReflectiveOperationException {
        Field field = pow.bannerfall.Team.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(team, value);
    }

    private void patchScoreboardTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        patchScoreboardTeam(scoreboard, "bf_blue", "Dawn", NamedTextColor.YELLOW);
        patchScoreboardTeam(scoreboard, "bf_red", "Dusk", NamedTextColor.BLUE);
    }

    private void patchScoreboardTeam(Scoreboard scoreboard, String teamId, String displayName, NamedTextColor color) {
        Team team = scoreboard.getTeam(teamId);

        if (team == null) {
            return;
        }

        team.displayName(Component.text(displayName, color));
        team.color(color);
        team.prefix(Component.text("", color));
    }
}
