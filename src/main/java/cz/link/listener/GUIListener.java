package cz.link.listener;

import cz.link.LinkPlugin;
import cz.link.gui.AdminGUI;
import cz.link.gui.LinkGUI;
import cz.link.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GUIListener implements Listener {

    private final LinkPlugin plugin;

    public GUIListener(LinkPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int emailSlot = plugin.getConfigManager().getConfig().getInt("settings.email-slot", 12);
        int discordSlot = plugin.getConfigManager().getConfig().getInt("settings.discord-slot", 16);

        // Uživatelské GUI
        if (event.getInventory().getHolder() instanceof LinkGUI) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());

            if (slot == emailSlot) {
                if (!data.hasEmail()) {
                    player.closeInventory();
                    plugin.getChatInputListener().addPendingPlayer(player.getUniqueId());
                    player.sendMessage(plugin.getConfigManager().getMessage("email-prompt"));
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    plugin.getDatabaseManager().removeEmail(player.getUniqueId());
                    player.sendMessage(plugin.getConfigManager().getMessage("unlink-success", "{platform}", "Email"));
                    new LinkGUI(plugin, player).open();
                }
            } else if (slot == discordSlot) {
                if (!data.hasDiscord()) {
                    player.closeInventory();
                    player.performCommand("discord link");
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    plugin.getDatabaseManager().removeDiscord(player.getUniqueId());
                    player.performCommand("discord unlink");
                    player.sendMessage(plugin.getConfigManager().getMessage("unlink-success", "{platform}", "Discord"));
                    new LinkGUI(plugin, player).open();
                }
            }
        }

        // Administrátorské GUI
        if (event.getInventory().getHolder() instanceof AdminGUI adminGUI) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            OfflinePlayer target = Bukkit.getOfflinePlayer(adminGUI.getTargetUUID());
            PlayerData data = plugin.getDatabaseManager().getPlayerData(target.getUniqueId());

            if (slot == emailSlot && data.hasEmail() && event.getClick() == ClickType.SHIFT_RIGHT) {
                plugin.getDatabaseManager().removeEmail(target.getUniqueId());
                player.sendMessage(plugin.getConfigManager().getMessage("unlink-success", "{platform}", "Email"));
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(plugin.getConfigManager().getMessage("admin-unlinked-target", "{platform}", "Email"));
                }
                new AdminGUI(plugin, target).open(player);
            } else if (slot == discordSlot && data.hasDiscord() && event.getClick() == ClickType.SHIFT_RIGHT) {
                plugin.getDatabaseManager().removeDiscord(target.getUniqueId());
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().performCommand("discord unlink");
                    target.getPlayer().sendMessage(plugin.getConfigManager().getMessage("admin-unlinked-target", "{platform}", "Discord"));
                } else {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "discord unlink " + target.getName());
                }
                player.sendMessage(plugin.getConfigManager().getMessage("unlink-success", "{platform}", "Discord"));
                new AdminGUI(plugin, target).open(player);
            }
        }
    }
}
