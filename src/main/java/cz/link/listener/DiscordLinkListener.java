package cz.link.listener;

import cz.link.LinkPlugin;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;

public class DiscordLinkListener {

    private final LinkPlugin plugin;

    public DiscordLinkListener(LinkPlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onAccountLinked(AccountLinkedEvent event) {
        String discordName = event.getUser().getName();
        plugin.getDatabaseManager().setDiscord(event.getPlayer().getUniqueId(), discordName);
    }

    @Subscribe
    public void onAccountUnlinked(AccountUnlinkedEvent event) {
        plugin.getDatabaseManager().removeDiscord(event.getPlayer().getUniqueId());
    }
}
