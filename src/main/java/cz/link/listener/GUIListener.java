package cz.link.listener;

import cz.link.LinkPlugin;
import cz.link.gui.AdminGUI;
import cz.link.gui.LinkGUI;
import cz.link.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
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

    private void copyToClipboard(Player player, String text, Component successMessage) {
        Component message = successMessage.clickEvent(ClickEvent.copyToClipboard(text));
        player.sendMessage(message);
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
                } else if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
                    copyToClipboard(player, data.getEmail(), plugin.getConfigManager().getMessage("email-copied", "{email}", data.getEmail()));
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    plugin.getDatabaseManager().removeEmail(player.getUniqueId());
                    player.sendMessage(plugin.getConfigManager().getMessage("unlink-success", "{platform}", "Email"));
                    new LinkGUI(plugin, player).open();
                }
            } else if (slot == discordSlot) {
                if (!data.hasDiscord()) {
                    player.closeInventory();
                    player.performCommand("discord link");
                } else if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
                    copyToClipboard(player, data.getDiscordAccount(), plugin.getConfigManager().getMessage("discord-copied", "{account}", data.getDiscordAccount()));
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
            OfflinePlayer target = adminGUI.getTarget();
            PlayerData data = plugin.getDatabaseManager().getPlayerData(target.getUniqueId());

            if (slot == emailSlot) {
                if (!data.hasEmail()) {
                    if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
                        player.closeInventory();
                        plugin.getChatInputListener().addPendingAdminInput(player.getUniqueId(), target.getUniqueId());
                        player.sendMessage(plugin.getConfigManager().getMessage("admin-email-prompt", "{player}", target.getName() != null ? target.getName() : "hráč"));
                    }
                } else {
                    if (event.getClick() == ClickType.LEFT) {
                        copyToClipboard(player, data.getEmail(), plugin.getConfigManager().getMessage("email-copied", "{email}", data.getEmail()));
                    } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                        player.closeInventory();
                        plugin.getChatInputListener().addPendingAdminInput(player.getUniqueId(), target.getUniqueId());
                        player.sendMessage(plugin.getConfigManager().getMessage("admin-email-prompt", "{player}", target.getName() != null ? target.getName() : "hráč"));
                    } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                        plugin.getDatabaseManager().removeEmail(target.getUniqueId());
                        player.sendMessage(plugin.getConfigManager().getMessage("unlink-success", "{platform}", "Email"));
                        if (target.isOnline() && target.getPlayer() != null) {
                            target.getPlayer().sendMessage(plugin.getConfigManager().getMessage("admin-unlinked-target", "{platform}", "Email"));
                        }
                        new AdminGUI(plugin, target).open(player);
                    }
                }
            } else if (slot == discordSlot) {
                // Discord lze v admin GUI ovládat (kopírovat / unlinkovat) pouze pokud je propojený
                if (data.hasDiscord()) {
                    if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT) {
                        copyToClipboard(player, data.getDiscordAccount(), plugin.getConfigManager().getMessage("discord-copied", "{account}", data.getDiscordAccount()));
                    } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
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
    }
}
