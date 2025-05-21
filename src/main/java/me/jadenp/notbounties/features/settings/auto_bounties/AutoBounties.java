package me.jadenp.notbounties.features.settings.auto_bounties;

import me.jadenp.notbounties.features.ActionCommands;
import me.jadenp.notbounties.features.settings.ResourceConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AutoBounties extends ResourceConfiguration {

    private String consoleBountyName;
    private double expireTime;
    private boolean overrideImmunity;
    private Set<String> blockedBountyCommands;

    @Override
    protected void loadConfiguration(YamlConfiguration config) {
        MurderBounties.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("murder-bounty")));
        RandomBounties.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("random-bounties")));
        TimedBounties.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("timed-bounties")));
        TrickleBounties.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("trickle-bounties")));
        Prompt.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("prompts")));
        BigBounty.loadConfiguration(Objects.requireNonNull(config.getConfigurationSection("big-bounties")));
        ActionCommands.loadConfiguration(plugin, config.getStringList("bounty-claim-commands"), config.getStringList("big-bounties.commands"), config.getStringList("bounty-set-commands"));

        consoleBountyName = config.getString("console-bounty-name");
        expireTime = config.getDouble("expire-time");
        overrideImmunity = config.getBoolean("override-immunity");
        blockedBountyCommands = config.getStringList("blocked-bounty-commands").stream().collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected String[] getModifiableSections() {
        return new String[0];
    }

    @Override
    protected String getPath() {
        return "settings/auto-bounties.yml";
    }

    public double getExpireTime() {
        return expireTime;
    }

    /**
     * Get a list of the blocked commands for players with bounties.
     * @return The commands that are blocked.
     */
    public Set<String> getBlockedBountyCommands() {
        return blockedBountyCommands;
    }

    public String getConsoleBountyName() {
        return consoleBountyName;
    }

    public boolean isOverrideImmunity() {
        return overrideImmunity;
    }
}
