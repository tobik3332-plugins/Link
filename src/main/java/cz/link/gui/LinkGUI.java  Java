package cz.link.gui;

import cz.link.LinkPlugin;
import cz.link.model.PlayerData;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class LinkGUI implements InventoryHolder {

    private final LinkPlugin plugin;
    private final Player player;
    private final Inventory inventory;

    public LinkGUI(LinkPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        int size = plugin.getConfigManager().getConfig().getInt("settings.gui-size", 27);
        Component title = plugin.getConfigManager().colorize(
                plugin.getConfigManager().getConfig().getString("gui.title", "&8Propojení účtů")
        );
        this.inventory = Bukkit.createInventory(this, size, title);
        setupItems();
    }

    private void setupItems() {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(player.getUniqueId());
        int emailSlot = plugin.getConfigManager().getConfig().getInt("settings.email-slot", 12);
        int discordSlot = plugin.getConfigManager().getConfig().getInt("settings.discord-slot", 16);

        // Email item
        if (data.hasEmail()) {
            inventory.setItem(emailSlot, createItem("gui.email.linked",
                    "{email}", data.getEmail(),
                    "{date}", data.getEmailDate()));
        } else {
            inventory.setItem(emailSlot, createItem("gui.email.unlinked"));
        }

        // Discord item
        if (data.hasDiscord()) {
            inventory.setItem(discordSlot, createItem("gui.discord.linked",
                    "{account}", data.getDiscordAccount(),
                    "{date}", data.getDiscordDate()));
        } else {
            inventory.setItem(discordSlot, createItem("gui.discord.unlinked"));
        }
    }

    private ItemStack createItem(String path, String... replacements) {
        ConfigurationSection sec = plugin.getConfigManager().getConfig().getConfigurationSection(path);
        if (sec == null) return new ItemStack(Material.BARRIER);

        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = sec.getString("name", "");
            List<String> rawLore = sec.getStringList("lore");
            List<String> processedLore = new ArrayList<>();

            for (String line : rawLore) {
                for (int i = 0; i < replacements.length; i += 2) {
                    line = line.replace(replacements[i], replacements[i + 1]);
                }
                processedLore.add(line);
            }

            meta.displayName(plugin.getConfigManager().colorize(name));
            meta.lore(plugin.getConfigManager().colorizeList(processedLore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
