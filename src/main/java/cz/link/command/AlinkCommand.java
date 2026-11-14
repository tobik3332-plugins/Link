package cz.link.command;

import cz.link.LinkPlugin;
import cz.link.gui.AdminGUI;
import org.bukkit.Bukkit;
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

public class AlinkCommand implements CommandExecutor, TabCompleter {

    private final LinkPlugin plugin;

    public AlinkCommand(LinkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().getMessage("usage-alink"));
            return true;
        }

        // /alink reload (permise: link.reload)
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("link.reload")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            plugin.getConfigManager().reload();
            plugin.getDatabaseManager().load();
            sender.sendMessage(plugin.getConfigManager().getMessage("reload"));
            return true;
        }

        // /alink check <hráč> (permise: link.alink)
        if (args[0].equalsIgnoreCase("check")) {
            if (!sender.hasPermission("link.alink")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            if (!(sender instanceof Player admin)) {
                sender.sendMessage(plugin.getConfigManager().colorize("&cGUID může otevřít pouze hráč."));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(plugin.getConfigManager().getMessage("usage-alink"));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                return true;
            }

            new AdminGUI(plugin, target).open(admin);
            return true;
        }

        // /alink set email <hráč> <email> (permise: link.alink)
        if (args[0].equalsIgnoreCase("set") && args.length >= 2 && args[1].equalsIgnoreCase("email")) {
            if (!sender.hasPermission("link.alink")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                return true;
            }
            if (args.length < 4) {
                sender.sendMessage(plugin.getConfigManager().getMessage("usage-alink"));
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
                return true;
            }

            String email = args[3];
            plugin.getDatabaseManager().setEmail(target.getUniqueId(), email);
            sender.sendMessage(plugin.getConfigManager().getMessage("email-saved", "{email}", email));
            return true;
        }

        sender.sendMessage(plugin.getConfigManager().getMessage("usage-alink"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("link.reload")) suggestions.add("reload");
            if (sender.hasPermission("link.alink")) {
                suggestions.add("check");
                suggestions.add("set");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("check") && sender.hasPermission("link.alink")) {
                Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
            } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission("link.alink")) {
                suggestions.add("email");
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("email") && sender.hasPermission("link.alink")) {
            Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
        }

        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .toList();
    }
}
