package me.jadenp.notbounties;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class BountyExpansion extends PlaceholderExpansion {

    private final NotBounties notBounties;

    public BountyExpansion(NotBounties notBounties){
        this.notBounties = notBounties;
    }

    @Override
    public @NotNull String getAuthor() {
        return "Not_Jaden";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "notbounties";
    }

    @Override
    public @NotNull String getVersion() {
        return notBounties.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params){
        String uuid = player.getUniqueId().toString();
        if (params.equalsIgnoreCase("bounty")){
            Bounty bounty = notBounties.SQL.isConnected() ? notBounties.data.getBounty(player.getUniqueId().toString()) : notBounties.getBounty(player);
            if (bounty != null){
                return bounty.getTotalBounty() + "";
            }
            return "0";
        }

        if (params.equalsIgnoreCase("bounties_claimed")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getClaimed(player.getUniqueId().toString()) + "";
            }
            return notBounties.bountiesClaimed.get(uuid) + "";
        }

        if (params.equalsIgnoreCase("bounties_set")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getSet(player.getUniqueId().toString()) + "";
            }
            return notBounties.bountiesSet.get(uuid) + "";
        }

        if (params.equalsIgnoreCase("bounties_received")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getReceived(player.getUniqueId().toString()) + "";
            }
            return notBounties.bountiesReceived.get(uuid) + "";
        }

        if (params.equalsIgnoreCase("immunity_spent")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getImmunity(player.getUniqueId().toString()) + "";
            }
            return notBounties.immunitySpent.get(player.getUniqueId().toString()) + "";
        }

        if (params.equalsIgnoreCase("all_time_bounty")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getAllTime(player.getUniqueId().toString()) + "";
            }
            return notBounties.allTimeBounty.get(player.getUniqueId().toString()) + "";
        }

        if (params.equalsIgnoreCase("currency_gained")){
            if (notBounties.SQL.isConnected()){
                return notBounties.data.getTotalClaimed(player.getUniqueId().toString()) + "";
            }
            return notBounties.allClaimed.get(player.getUniqueId().toString()) + "";
        }

        return null;
    }
}