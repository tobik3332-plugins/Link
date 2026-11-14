package cz.link.listener;

import cz.link.LinkPlugin;
import cz.link.gui.LinkGUI;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ChatInputListener implements Listener {

    private final LinkPlugin plugin;
    private final Set<UUID> pendingPlayers = ConcurrentHashMap.newKeySet();
    private final Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public ChatInputListener(LinkPlugin plugin) {
        this.plugin = plugin;
    }

    public void addPendingPlayer(UUID uuid) {
        pendingPlayers.add(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!pendingPlayers.contains(player.getUniqueId())) return;

        // Zrušení události zabrání odeslání do veřejného chatu i do DiscordSRV
        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (message.equalsIgnoreCase("cancel")) {
            pendingPlayers.remove(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().getMessage("email-cancelled"));
            Bukkit.getScheduler().runTask(plugin, () -> new LinkGUI(plugin, player).open());
            return;
        }

        if (!emailPattern.matcher(message).matches()) {
            player.sendMessage(plugin.getConfigManager().getMessage("email-invalid"));
            return;
        }

        pendingPlayers.remove(player.getUniqueId());
        plugin.getDatabaseManager().setEmail(player.getUniqueId(), message);
        player.sendMessage(plugin.getConfigManager().getMessage("email-saved", "{email}", message));

        Bukkit.getScheduler().runTask(plugin, () -> new LinkGUI(plugin, player).open());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingPlayers.remove(event.getPlayer().getUniqueId());
    }
}
