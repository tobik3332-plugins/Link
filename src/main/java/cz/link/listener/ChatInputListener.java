package cz.link.listener;

import cz.link.LinkPlugin;
import cz.link.gui.AdminGUI;
import cz.link.gui.LinkGUI;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ChatInputListener implements Listener {

    private final LinkPlugin plugin;
    private final Set<UUID> pendingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> pendingAdminInputs = new ConcurrentHashMap<>(); // Admin UUID -> Target UUID
    private final Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public ChatInputListener(LinkPlugin plugin) {
        this.plugin = plugin;
    }

    public void addPendingPlayer(UUID uuid) {
        pendingPlayers.add(uuid);
    }

    public void addPendingAdminInput(UUID adminUuid, UUID targetUuid) {
        pendingAdminInputs.put(adminUuid, targetUuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        boolean isUserPending = pendingPlayers.contains(uuid);
        boolean isAdminPending = pendingAdminInputs.containsKey(uuid);

        if (!isUserPending && !isAdminPending) return;

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        // Zpracování pro běžného hráče
        if (isUserPending) {
            if (message.equalsIgnoreCase("cancel")) {
                pendingPlayers.remove(uuid);
                player.sendMessage(plugin.getConfigManager().getMessage("email-cancelled"));
                Bukkit.getScheduler().runTask(plugin, () -> new LinkGUI(plugin, player).open());
                return;
            }

            if (!emailPattern.matcher(message).matches()) {
                player.sendMessage(plugin.getConfigManager().getMessage("email-invalid"));
                return;
            }

            pendingPlayers.remove(uuid);
            plugin.getDatabaseManager().setEmail(uuid, message);
            player.sendMessage(plugin.getConfigManager().getMessage("email-saved", "{email}", message));
            Bukkit.getScheduler().runTask(plugin, () -> new LinkGUI(plugin, player).open());
            return;
        }

        // Zpracování pro admina
        if (isAdminPending) {
            UUID targetUuid = pendingAdminInputs.remove(uuid);
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);

            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.getConfigManager().getMessage("admin-email-cancelled"));
                Bukkit.getScheduler().runTask(plugin, () -> new AdminGUI(plugin, target).open(player));
                return;
            }

            if (!emailPattern.matcher(message).matches()) {
                pendingAdminInputs.put(uuid, targetUuid); // vrátíme zpět do čekání
                player.sendMessage(plugin.getConfigManager().getMessage("email-invalid"));
                return;
            }

            plugin.getDatabaseManager().setEmail(targetUuid, message);
            player.sendMessage(plugin.getConfigManager().getMessage("email-saved", "{email}", message));
            Bukkit.getScheduler().runTask(plugin, () -> new AdminGUI(plugin, target).open(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingPlayers.remove(event.getPlayer().getUniqueId());
        pendingAdminInputs.remove(event.getPlayer().getUniqueId());
    }
}
