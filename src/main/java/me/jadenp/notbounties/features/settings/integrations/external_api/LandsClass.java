package me.jadenp.notbounties.features.settings.integrations.external_api;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.enums.FlagTarget;
import me.angeschossen.lands.api.flags.enums.RoleFlagCategory;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.Land;
import me.angeschossen.lands.api.land.LandWorld;
import me.angeschossen.lands.api.nation.Nation;
import me.jadenp.notbounties.NotBounties;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LandsClass {
    private static LandsIntegration api = null;
    private static RoleFlag claimFlag = null;

    public LandsClass() {
        if (api == null)
            api = LandsIntegration.of(NotBounties.getInstance());
    }

    public void registerClaimFlag(){
        claimFlag = RoleFlag.of(api, FlagTarget.PLAYER, RoleFlagCategory.ACTION, "claim_bounties");
        claimFlag.setDisplayName("Claim Bounties");
        claimFlag.setDescription("Allow bounties to be claimed in your land.");
        claimFlag.setDefaultState(true);
        claimFlag.setAlwaysAllowInWilderness(true);
        claimFlag.setIcon(new ItemStack(Material.SKELETON_SKULL));
        claimFlag.setToggleableByNation(true);
    }

    public boolean canClaim(@NotNull Player player, @NotNull Location location) {
        if (claimFlag == null)
            // claim flag not registered
            return true;
        LandWorld world = api.getWorld(Objects.requireNonNull(location.getWorld()));
        if (world == null)
            return true;
        return world.hasRoleFlag(player.getUniqueId(), location, claimFlag);
    }

    public boolean shareLand(Player player1, Player player2) {
        Land land1 = api.getLandPlayer(player1.getUniqueId()).getOwningLand();
        Land land2 = api.getLandPlayer(player2.getUniqueId()).getOwningLand();
        return (land1 != null && land1.getTrustedPlayers().contains(player2.getUniqueId()) || land2 != null && land2.getTrustedPlayers().contains(player1.getUniqueId()));
    }

    public boolean shareNation(Player player1, Player player2) {
        Land land1 = api.getLandPlayer(player1.getUniqueId()).getOwningLand();
        Land land2 = api.getLandPlayer(player2.getUniqueId()).getOwningLand();
        if (land1 == null || land2 == null)
            return false;
        Nation nation1 = land1.getNation();
        Nation nation2 = land2.getNation();
        if (nation1 == null || nation2 == null)
            return false;
        return nation1.getName().equals(nation2.getName());
    }

    public boolean areNationsAllied(Player player1, Player player2) {
        Land land1 = api.getLandPlayer(player1.getUniqueId()).getOwningLand();
        Land land2 = api.getLandPlayer(player2.getUniqueId()).getOwningLand();
        if (land1 == null || land2 == null)
            return false;
        Nation nation1 = land1.getNation();
        Nation nation2 = land2.getNation();
        if (nation1 == null || nation2 == null)
            return false;
        return nation1.getAllies().contains(nation2);
    }

    public boolean areLandsAllied(Player player1, Player player2) {
        Land land1 = api.getLandPlayer(player1.getUniqueId()).getOwningLand();
        Land land2 = api.getLandPlayer(player2.getUniqueId()).getOwningLand();
        if (land1 == null || land2 == null)
            return false;
        return land1.getAllies().contains(land2);
    }


}
