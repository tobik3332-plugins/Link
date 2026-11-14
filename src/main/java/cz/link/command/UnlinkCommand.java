package cz.link.command;

import cz.link.LinkPlugin;
import cz.link.model.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class UnlinkCommand implements CommandExecutor, TabCompleter {

    private final LinkPlugin plugin;
    private final Map<UUID, PendingUnlink> confirmations = new HashMap<>();

    private record PendingUnlink(String platform, long timestamp) {}

    public UnlinkCommand(LinkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("link.unlink")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length != 1 || (!args[0].equalsIgnoreCase("email") && !args[0].equalsIgnoreCase("discord"))) {
            player.sendMessage(plugin.getConfigManager().colorize("&cPoužití: /unlink <email|discord>"));
            return true;
        }

        String platform = args[0].toLowerCase();
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());

        if (platform.equals("email") && !data.hasEmail()) {
            player.sendMessage(plugin.getConfigManager().colorize("&cNemáš propojený žádný email."));
            return true;
        }
        if (platform.equals("discord") && !data.hasDiscord()) {
            player.sendMessage(plugin.getConfigManager().colorize("&cNemáš propojený žádný Discord."));
            return true;
        }

        PendingUnlink pending = confirmations.get(player.getUniqueId());
        long now = System.currentTimeMillis();

        // Kontrola do 5 sekund (5000 ms)
        if (pending != null && pending.platform().equals(platform) && (now - pending.timestamp()) <= 5000) {
            confirmations.remove(player.getUniqueId());
            if (platform.equals("email")) {
                plugin.getDatabaseManager().removeEmail(player.getUniqueId());
                player.sendMessage(plugin.getConfigManager().getMessage("unlink-success", "{platform}", "Email"));
            } else {
                plugin.getDatabaseManager().removeDiscord(player.getUniqueId());
                player.performCommand("discord unlink");
                player.sendMessage(plugin.getConfigManager().getMessage("unlink-success", "{platform}", "Discord"));
            }
        } else {
            // Nový požadavek nebo vypršelý čas -> znovu vyzve na 5 sekund
            confirmations.put(player.getUniqueId(), new PendingUnlink(platform, now));
            player.sendMessage(plugin.getConfigManager().getMessage("unlink-prompt"));
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission("link.unlink")) {
            return List.of("email", "discord").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
