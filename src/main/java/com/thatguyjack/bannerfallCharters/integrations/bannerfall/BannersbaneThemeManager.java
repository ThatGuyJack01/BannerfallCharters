package com.thatguyjack.bannerfallCharters.integrations.bannerfall;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Field;

public class BannersbaneThemeManager {
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private final BannerfallCharters plugin;
    private BukkitTask repeatingPatchTask;

    public BannersbaneThemeManager(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    public void apply() {
        patchBannerfallTeamEnum();
        patchScoreboardTeams();
        patchOnlinePlayerTabNamesSafely();
    }

    public void startPatchTasks() {
        apply();

        if (repeatingPatchTask != null && !repeatingPatchTask.isCancelled()) {
            return;
        }

        repeatingPatchTask = Bukkit.getScheduler().runTaskTimer(plugin, this::apply, 1L, 20L);
    }

    private void patchBannerfallTeamEnum() {
        try {
            setEnumField(pow.bannerfall.Team.BLUE, "displayName", "Dusk");
            setEnumField(pow.bannerfall.Team.BLUE, "colorCode", "§9");

            setEnumField(pow.bannerfall.Team.RED, "displayName", "Dawn");
            setEnumField(pow.bannerfall.Team.RED, "colorCode", "§e");
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

        patchScoreboardTeam(scoreboard, "bf_blue", "Dusk", NamedTextColor.BLUE);
        patchScoreboardTeam(scoreboard, "bf_red", "Dawn", NamedTextColor.YELLOW);
    }

    private void patchScoreboardTeam(Scoreboard scoreboard, String teamId, String displayName, NamedTextColor color) {
        Team team = scoreboard.getTeam(teamId);

        if (team == null) {
            return;
        }

        team.displayName(Component.text(displayName, color));
        team.color(color);
    }

    private void patchOnlinePlayerTabNamesSafely() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        Team blueTeam = scoreboard.getTeam("bf_blue");
        Team redTeam = scoreboard.getTeam("bf_red");

        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerName = player.getName();

            if (blueTeam != null && blueTeam.hasEntry(playerName)) {
                patchPlayerListNameColorOnly(player, NamedTextColor.BLUE);
                continue;
            }

            if (redTeam != null && redTeam.hasEntry(playerName)) {
                patchPlayerListNameColorOnly(player, NamedTextColor.YELLOW);
            }
        }
    }

    private void patchPlayerListNameColorOnly(Player player, NamedTextColor color) {
        Component currentName = player.playerListName();

        if (currentName == null) {
            player.playerListName(Component.text(player.getName(), color));
            return;
        }

        Component cleanedName = currentName.color(null);

        Component patchedName = recolorOnlyPlayerName(cleanedName, player.getName(), color);

        if (!currentName.equals(patchedName)) {
            player.playerListName(patchedName);
        }
    }

    private Component recolorOnlyPlayerName(Component component, String playerName, NamedTextColor color) {
        Component patched = component;

        if (component instanceof TextComponent textComponent) {
            String content = textComponent.content();

            if (content.equals(playerName) || content.contains(playerName)) {
                patched = textComponent.color(color);
            }
        }

        java.util.List<Component> newChildren = new java.util.ArrayList<>();

        for (Component child : patched.children()) {
            newChildren.add(recolorOnlyPlayerName(child, playerName, color));
        }

        return patched.children(newChildren);
    }
}