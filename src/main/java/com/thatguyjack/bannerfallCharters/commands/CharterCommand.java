package com.thatguyjack.bannerfallCharters.commands;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pow.bannerfall.PlayerClass;
import pow.bannerfall.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class CharterCommand implements CommandExecutor, TabCompleter {
    private final BannerfallCharters plugin;

    public CharterCommand(BannerfallCharters plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("bannerfallcharters.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }


        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status" -> sendStatus(sender);
            case "reload" -> reloadAll(sender);
            case "scheduler" -> handleScheduler(sender, args);
            case "dungeon" -> handleDungeon(sender, args);
            case "farkle" -> handleFarkle(sender, args);
            case "class" -> handleClass(sender, args);
            case "team" -> handleTeam(sender, args);
            default -> sender.sendMessage(ChatColor.RED + "Unknown command.");
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "Bannerfall Charters Commands:");
        sender.sendMessage(ChatColor.YELLOW + label + " status - Show hook/status info.");
        sender.sendMessage(ChatColor.YELLOW + label + " reload - Reload Bannerfall, DungeonFall, Scheduler, and Charters config.");
        sender.sendMessage(ChatColor.YELLOW + label + " scheduler reload - Reload Scheduler schedules.");
        sender.sendMessage(ChatColor.YELLOW + label + " dungeon status - Show DungeonFall status.");
        sender.sendMessage(ChatColor.YELLOW + label + " dungeon close - Force-close the active dungeon run.");
        sender.sendMessage(ChatColor.YELLOW + label + " dungeon seed <seed> - Set the next dungeon seed.");
        sender.sendMessage(ChatColor.YELLOW + label + " dungeon entries reset <player|all> - Reset dungeon entry tracking.");
        sender.sendMessage(ChatColor.YELLOW + label + " farkle allowance <player> <amount> - Set a player's Farkle allowance.");
        sender.sendMessage(ChatColor.YELLOW + label + " class <player> <NONE | KNIGHT | MAGE|ROGUE> - Set Bannerfall class.");
        sender.sendMessage(ChatColor.YELLOW + label + " team <player> <NONE | BLUE | RED> - Set Bannerfall team.");
    }


    private void sendStatus(CommandSender sender) {
        boolean bannerfallLoaded = plugin.getServer().getPluginManager().isPluginEnabled("Bannerfall");
        boolean dungeonFallLoaded = plugin.getServer().getPluginManager().isPluginEnabled("DungeonFall");
        boolean schedulerLoaded = plugin.getServer().getPluginManager().isPluginEnabled("Scheduler");

        sender.sendMessage(ChatColor.GOLD + "Bannerfall Charters Status:");
        sender.sendMessage(ChatColor.YELLOW + "Bannerfall: " + loadedText(bannerfallLoaded));
        sender.sendMessage(ChatColor.YELLOW + "DungeonFall: " + loadedText(dungeonFallLoaded));
        sender.sendMessage(ChatColor.YELLOW + "Scheduler: " + loadedText(schedulerLoaded));
    }

    private String loadedText(boolean loaded) {
        return loaded ? ChatColor.GREEN + "Loaded" : ChatColor.RED + "MISSING";
    }

    private void reloadAll(CommandSender sender) {
        plugin.reloadConfig();
        plugin.dungeonFall().reload();
        plugin.scheduler().reloadSchedules();
        plugin.bannerfall().reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "Reloaded all plugins");
    }

    private void handleScheduler(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("reload")) {
            plugin.scheduler().reloadSchedules();
            sender.sendMessage(ChatColor.GREEN + "Scheduler reloaded.");
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown Command. Try /charter scheduler reload");
        }
    }

    private void handleDungeon(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Unknown Command. Try /charter dungeon <status | close | seed | entries>");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "status" -> sender.sendMessage(plugin.dungeonFall().getDungeonManager().getStatusInfo());
            case "close" -> {
                plugin.dungeonFall().getDungeonManager().forceClose();
                sender.sendMessage(ChatColor.GREEN + "Forced the dungeon to close.");
            }
            case "seed" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /charter dungeon seed <long>");
                    return;
                }
                try {
                    long seed = Long.parseLong(args[2]);
                    plugin.dungeonFall().getDungeonManager().setNextSeed(seed);
                    sender.sendMessage(ChatColor.GREEN + "Next dungeon seed set to " + seed);
                } catch (NumberFormatException ex) {
                    sender.sendMessage(ChatColor.RED + "That seed must be a whole number.");
                }
            }
            case "entries" -> handleDungeonEntries(sender, args);
            default -> sender.sendMessage(ChatColor.RED + "Unknown Command. Try /charter dungeon <status | close | seed | entries>");
        }
    }

    private void handleDungeonEntries(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[2].equalsIgnoreCase("reset")) {
            sender.sendMessage(ChatColor.RED + "Usage: /charter dungeon entries reset <player | all>");
            return;
        }

        if (args[3].equalsIgnoreCase("all")) {
            int removed = plugin.dungeonFall().getEnteredDungeonManager().removeAll();
            sender.sendMessage(ChatColor.GREEN + "Reset dungeon entries for " + removed + " players.");
            return;
        }

        boolean removed = plugin.dungeonFall().getEnteredDungeonManager().removePlayer(args[3]);
        sender.sendMessage(removed
                ? ChatColor.GREEN + "Reset dungeon entry tracking for &f" + args[3]
                : ChatColor.RED + "No dungeon entry tracking existed for &f" + args[3]);
    }

    private void handleFarkle(CommandSender sender, String[] args) {
        if (args.length < 4 || !args[1].equalsIgnoreCase("allowance")) {
            sender.sendMessage(ChatColor.RED + "Unknown command. Usage: /charter farkle allowance <online-player> <amount>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Chosen player must be online.");
            return;
        }

        try {
            int amount = Integer.parseInt(args[3]);
            plugin.bannerfall().getFarkleManager().setAllowance(target, amount);
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s Farkle allowance to " + amount);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Amount must be a whole number.");
        }
    }

    private void handleClass(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /charter class <player> <NONE | KNIGHT | MAGE | ROGUE>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Chosen player must be online.");
            return;
        }

        try {
            PlayerClass playerClass = PlayerClass.valueOf(args[2].toUpperCase(Locale.ROOT));
            plugin.bannerfall().getClassManager().setPlayerClass(target.getUniqueId(), playerClass);
            plugin.bannerfall().getClassManager().applyClassEffects(target);
            plugin.bannerfall().getClassManager().updatePlayerListName(target);
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s class to " + playerClass.name());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Unknown class. Use NONE, KNIGHT, MAGE, or ROGUE.");
        }
    }

    private void handleTeam(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /charter team <player> <NONE | BLUE | RED>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Chosen player must be online.");
            return;
        }

        try {
            Team team = Team.valueOf(args[2].toUpperCase(Locale.ROOT));
            plugin.bannerfall().getTerritoryManager().setPlayerTeam(target.getUniqueId(), team);
            plugin.bannerfall().getTerritoryManager().addToScoreboardTeam(target, team);
            plugin.bannerfall().getClassManager().updatePlayerListName(target);
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s team to " + team.name());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Unknown Team. Use NONE, BLUE, or RED.");
        }
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if(!sender.hasPermission("bannerfallcharters.admin")) {
            return List.of();
        }
        if(args.length == 1) {
            return startsWith(args[0], "status", "reload", "scheduler", "dungeon", "farkle", "class", "team", "help");
        }
        if(args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "scheduler" -> startsWith(args[1], "reload");
                case "dungeon" -> startsWith(args[1], "status", "close", "seed", "entries");
                case "farkle" -> startsWith(args[1], "allowance");
                case "class", "team" -> onlinePlayerNames(args[1]);
                default -> List.of();
            };
        }
        if(args.length == 3) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "dungeon" -> args[1].equalsIgnoreCase("entries") ? startsWith(args[2], "reset") : List.of();
                case "farkle" -> args[1].equalsIgnoreCase("allowance") ? onlinePlayerNames(args[2]) : List.of();
                case "class" -> startsWith(args[2], Arrays.stream(PlayerClass.values()).map(Enum::name).toArray(String[]::new));
                case "team" -> startsWith(args[2], "NONE", "BLUE", "RED");
                default -> List.of();
            };
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("dungeon") && args[1].equalsIgnoreCase("entries")) {
            List<String> names = new ArrayList<>(plugin.dungeonFall().getEnteredDungeonManager().getEnteredPlayers());
            names.add("all");
            return startsWith(args[3], names.toArray(String[]::new));
        }
        return List.of();
    }

    private List<String> startsWith(String prefix, String... options) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private List<String> onlinePlayerNames(String prefix) {
        return startsWith(prefix, Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new));
    }
}
