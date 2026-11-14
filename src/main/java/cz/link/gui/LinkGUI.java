package cz.link.gui;

import cz.link.LinkPlugin;
import cz.link.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        ).decoration(TextDecoration.ITALIC, false);

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

        String materialStr = sec.getString("material", "STONE");
        ItemStack item = null;

        // Podpora pro Nexo itemy přes reflexi (podpora ItemBuilder .build())
        if (materialStr.toLowerCase().startsWith("nexo-")) {
            String nexoId = materialStr.substring(5);
            try {
                Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
                Method method = nexoItemsClass.getMethod("itemFromId", String.class);
                Object result = method.invoke(null, nexoId);
                if (result != null) {
                    try {
                        Method buildMethod = result.getClass().getMethod("build");
                        Object built = buildMethod.invoke(result);
                        if (built instanceof ItemStack) {
                            item = ((ItemStack) built).clone();
                        }
                    } catch (NoSuchMethodException e) {
                        if (result instanceof ItemStack) {
                            item = ((ItemStack) result).clone();
                        }
                    }
                }
            } catch (Exception ignored) {
                // Nexo není dostupné nebo item nebyl nalezen
            }
        }

        if (item == null) {
            Material mat = Material.matchMaterial(materialStr);
            item = new ItemStack(mat != null ? mat : Material.STONE);
        }

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

            Component displayName = plugin.getConfigManager().colorize(name)
                    .decoration(TextDecoration.ITALIC, false);
            meta.displayName(displayName);

            List<Component> loreComponents = plugin.getConfigManager().colorizeList(processedLore).stream()
                    .map(line -> line.decoration(TextDecoration.ITALIC, false))
                    .collect(Collectors.toList());
            meta.lore(loreComponents);

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
