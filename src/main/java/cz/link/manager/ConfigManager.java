package cz.link.manager;

import cz.link.LinkPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {

    private final LinkPlugin plugin;

    public ConfigManager(LinkPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public Component colorize(String text) {
        if (text == null) return Component.empty();
        // Podpora pro MiniMessages a HEX pokud řetězec obsahuje značky <...>
        if (text.contains("<") && text.contains(">")) {
            try {
                return MiniMessage.miniMessage().deserialize(text);
            } catch (Exception ignored) {
                // Fallback na legacy
            }
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public List<Component> colorizeList(List<String> list) {
        return list.stream().map(this::colorize).collect(Collectors.toList());
    }

    public Component getMessage(String path) {
        String prefix = getConfig().getString("messages.prefix", "");
        String msg = getConfig().getString("messages." + path, "&cChybějící zpráva: " + path);
        return colorize(prefix + msg);
    }

    public Component getMessage(String path, String placeholder, String value) {
        String prefix = getConfig().getString("messages.prefix", "");
        String msg = getConfig().getString("messages." + path, "&cChybějící zpráva: " + path);
        msg = msg.replace(placeholder, value);
        return colorize(prefix + msg);
    }
}
