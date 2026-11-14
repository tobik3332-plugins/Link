package cz.link;

import cz.link.command.AlinkCommand;
import cz.link.command.LinkCommand;
import cz.link.command.UnlinkCommand;
import cz.link.listener.ChatInputListener;
import cz.link.listener.DiscordLinkListener;
import cz.link.listener.GUIListener;
import cz.link.manager.ConfigManager;
import cz.link.manager.DatabaseManager;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.plugin.java.JavaPlugin;

public class LinkPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ChatInputListener chatInputListener;
    private DiscordLinkListener discordLinkListener;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.chatInputListener = new ChatInputListener(this);

        // Registrace Bukkit listenerů
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(chatInputListener, this);

        // Registrace DiscordSRV listeneru
        this.discordLinkListener = new DiscordLinkListener(this);
        DiscordSRV.api.subscribe(discordLinkListener);

        // Registrace příkazů a TabCompleterů
        LinkCommand linkCmd = new LinkCommand(this);
        getCommand("link").setExecutor(linkCmd);
        getCommand("link").setTabCompleter(linkCmd);

        AlinkCommand alinkCmd = new AlinkCommand(this);
        getCommand("alink").setExecutor(alinkCmd);
        getCommand("alink").setTabCompleter(alinkCmd);

        UnlinkCommand unlinkCmd = new UnlinkCommand(this);
        getCommand("unlink").setExecutor(unlinkCmd);
        getCommand("unlink").setTabCompleter(unlinkCmd);

        getLogger().info("Plugin Link byl úspěšně zapnut!");
    }

    @Override
    public void onDisable() {
        if (discordLinkListener != null) {
            DiscordSRV.api.unsubscribe(discordLinkListener);
        }
        getLogger().info("Plugin Link byl vypnut.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ChatInputListener getChatInputListener() {
        return chatInputListener;
    }
}
