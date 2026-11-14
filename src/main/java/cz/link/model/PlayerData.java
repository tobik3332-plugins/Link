package cz.link.model;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String email;
    private String emailDate;
    private String discordAccount;
    private String discordDate;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmailDate() { return emailDate; }
    public void setEmailDate(String emailDate) { this.emailDate = emailDate; }

    public String getDiscordAccount() { return discordAccount; }
    public void setDiscordAccount(String discordAccount) { this.discordAccount = discordAccount; }

    public String getDiscordDate() { return discordDate; }
    public void setDiscordDate(String discordDate) { this.discordDate = discordDate; }

    public boolean hasEmail() { return email != null && !email.isEmpty(); }
    public boolean hasDiscord() { return discordAccount != null && !discordAccount.isEmpty(); }
}
