package com.thatguyjack.bannerfallCharters.commands;

import com.thatguyjack.bannerfallCharters.BannerfallCharters;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BFChartersCommand implements CommandExecutor, TabCompleter {
    private final BannerfallCharters plugin;

    List<String> chartersList = new ArrayList<>(List.of("test1", "test2", "test3"));

    public BFChartersCommand(BannerfallCharters plugin) {
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

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set", "clear", "list" -> handleCharter(sender, args);
            default -> sender.sendMessage(ChatColor.RED + "Unknown command. Try /charter <set|clear|list>");
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "Bannerfall Charters Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " set <player> <charter>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " clear <player>");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list all");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " list <player>");
    }

    private void handleCharter(CommandSender sender, String[] args) {
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /charter set <player> <charter>");
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String targetName = target.getName() != null ? target.getName() : args[1];

                String charterID = args[2].toLowerCase(Locale.ROOT);
                if (chartersList.stream().noneMatch(charterID::equalsIgnoreCase)) {
                    sender.sendMessage(ChatColor.RED + "Please enter a valid Charter ID.");
                    return;
                }

                plugin.charterManager().setCharacter(target, charterID);

                sender.sendMessage(ChatColor.GREEN + "Set " + targetName + "'s charter to " + charterID);
            }

            case "clear" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /charter clear <player>");
                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String targetName = target.getName() != null ? target.getName() : args[1];

                boolean removed = plugin.charterManager().removeCharacter(target);

                if (removed) sender.sendMessage(ChatColor.GREEN + "Cleared " + targetName + "'s charter");
                else sender.sendMessage(ChatColor.RED + targetName + " does not have an active charter");
            }

            case "list" -> {
                if (Bukkit.getOnlinePlayers().isEmpty() && plugin.charterManager().getAllCharters().isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "There are no active charters");
                    return;
                }

                if (args.length < 2 || args[1].equalsIgnoreCase("all")) {
                    List<Player> playersWithoutCharters = new ArrayList<>(Bukkit.getOnlinePlayers());

                    sender.sendMessage(ChatColor.GOLD + "Displaying all active charters:");

                    plugin.charterManager().getAllCharters().forEach((uuid, charterId) -> {
                        Player player = Bukkit.getPlayer(uuid);

                        String playerName;
                        if (player != null) playerName = player.getName();
                        else playerName = Bukkit.getOfflinePlayer(uuid).getName();
                        if (playerName == null) playerName = uuid.toString();

                        sender.sendMessage(ChatColor.YELLOW + playerName + " : " + charterId);

                        playersWithoutCharters.removeIf(p -> p.getUniqueId().equals(uuid));
                    });

                    if (!playersWithoutCharters.isEmpty()) {
                        playersWithoutCharters.forEach(user ->
                                sender.sendMessage(ChatColor.YELLOW + user.getName() + " : none")
                        );
                    }

                    return;
                }

                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                String targetName = target.getName() != null ? target.getName() : args[1];
                String charter = plugin.charterManager()
                                    .getCharter(target.getUniqueId())
                                    .orElse("none");

                sender.sendMessage(ChatColor.YELLOW + targetName + " : " + charter);
            }

            default -> sender.sendMessage(ChatColor.RED + "Unknown command. Try /charter <set|clear|list>");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        if(!sender.hasPermission("bannerfallcharters.admin")) {
            return List.of();
        }
        if(args.length == 1) {
            return startsWith(args[0], "set", "clear", "list", "help");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "set" -> onlinePlayerNames(args[1]);

                case "clear", "list" -> {
                    List<String> names = new ArrayList<>();

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (plugin.charterManager().getCharter(player.getUniqueId()).isPresent()) {
                            names.add(player.getName());
                        }
                    }

                    if (args[0].equalsIgnoreCase("list")) {
                        names.add("all");
                    }

                    yield startsWith(args[1], names.toArray(String[]::new));
                }

                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return startsWith(args[2], chartersList.toArray(String[]::new));
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
