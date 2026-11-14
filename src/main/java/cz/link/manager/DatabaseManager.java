package cz.link.manager;

import cz.link.LinkPlugin;
import cz.link.model.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {

    private final LinkPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss dd.MM. yyyy");

    public DatabaseManager(LinkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "database.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nelze vytvořit soubor database.yml!");
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        cache.clear();

        if (config.getConfigurationSection("players") != null) {
            for (String key : config.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                PlayerData data = new PlayerData(uuid);
                data.setEmail(config.getString("players." + key + ".email"));
                data.setEmailDate(config.getString("players." + key + ".email-date"));
                data.setDiscordAccount(config.getString("players." + key + ".discord-account"));
                data.setDiscordDate(config.getString("players." + key + ".discord-date"));
                cache.put(uuid, data);
            }
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, PlayerData::new);
    }

    public String getCurrentTimestamp() {
        return dateFormat.format(new Date());
    }

    public void setEmail(UUID uuid, String email) {
        PlayerData data = getPlayerData(uuid);
        data.setEmail(email);
        data.setEmailDate(getCurrentTimestamp());
        savePlayerData(data);
    }

    public void removeEmail(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        data.setEmail(null);
        data.setEmailDate(null);
        savePlayerData(data);
    }

    public void setDiscord(UUID uuid, String discordTag) {
        PlayerData data = getPlayerData(uuid);
        data.setDiscordAccount(discordTag);
        data.setDiscordDate(getCurrentTimestamp());
        savePlayerData(data);
    }

    public void removeDiscord(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        data.setDiscordAccount(null);
        data.setDiscordDate(null);
        savePlayerData(data);
    }

    public void savePlayerData(PlayerData data) {
        String path = "players." + data.getUuid().toString();
        if (!data.hasEmail() && !data.hasDiscord()) {
            config.set(path, null);
        } else {
            config.set(path + ".email", data.getEmail());
            config.set(path + ".email-date", data.getEmailDate());
            config.set(path + ".discord-account", data.getDiscordAccount());
            config.set(path + ".discord-date", data.getDiscordDate());
        }
        saveFile();
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Chyba při ukládání database.yml!");
            e.printStackTrace();
        }
    }
}
