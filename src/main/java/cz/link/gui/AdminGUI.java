package cz.link.gui;

import cz.link.LinkPlugin;
import cz.link.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import java.util.UUID;
import java.util.stream.Collectors;

public class AdminGUI implements InventoryHolder {

    private final LinkPlugin plugin;
    private final OfflinePlayer target;
    private final Inventory inventory;

    public AdminGUI(LinkPlugin plugin, OfflinePlayer target) {
        this.plugin = plugin;
        this.target = target;
        int size = plugin.getConfigManager().getConfig().getInt("settings.gui-size", 27);
        String prefix = plugin.getConfigManager().getConfig().getString("gui.admin-title-prefix", "&8Admin: &e");
        String targetName = target.getName() != null ? target.getName() : target.getUniqueId().toString();
        Component title = plugin.getConfigManager().colorize(prefix + targetName)
                .decoration(TextDecoration.ITALIC, false);

        this.inventory = Bukkit.createInventory(this, size, title);
        setupItems();
    }

    private void setupItems() {
        PlayerData data = plugin.getDatabaseManager().getPlayerData(target.getUniqueId());
        int emailSlot = plugin.getConfigManager().getConfig().getInt("settings.email-slot", 12);
        int discordSlot = plugin.getConfigManager().getConfig().getInt("settings.discord-slot", 16);

        if (data.hasEmail()) {
            inventory.setItem(emailSlot, createItem("gui.email.linked",
                    "{email}", data.getEmail(),
                    "{date}", data.getEmailDate()));
        } else {
            inventory.setItem(emailSlot, createItem("gui.email.unlinked"));
        }

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
            } catch (Exception ignored) {}
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

    public UUID getTargetUUID() {
        return target.getUniqueId();
    }

    public OfflinePlayer getTarget() {
        return target;
    }

    public void open(Player admin) {
        admin.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
