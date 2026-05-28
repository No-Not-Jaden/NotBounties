package me.jadenp.notbounties.ui;

import me.jadenp.notbounties.NotBounties;

import java.util.UUID;

// Recreate the way the client assigns default skins in the DefaultPlayerSkin class
public class DefaultSkinHelper {

    private DefaultSkinHelper() {}

    // The client version of this array is twice as long with skins for slim and wide
    private static final PlayerSkin[] DEFAULT_SKINS = new PlayerSkin[] {
            create("3b60a1f6d562f52aaebbf1434f1de147933a3affe0e764fa49ea057536623cd3"), // Alex
            create("4c05ab9e07b3505dc3ec11370c3bdce5570ad2fb2b562e9b9dd9cf271f81aa44"), // Ari
            create("fdc7f52d7f26fbd782099868b54689f30f35182877ba0a9c8846550e07338443"), // Efe
            create("f2e4bce5182baa510ed9f075fde8975b1c170c3555827041bba35d563eab9137"), // Kai
            create("7cb3ba52ddd5cc82c0b050c3f920f87da36add80165846f479079663805433db"), // Makena
            create("6c160fbd16adbc4bff2409e70180d911002aebcfa811eb6ec3d1040761aea6dd"), // Noor
            create("1a4af718455d4aab528e7a61f86fa25e6a369d1768dcb13f7df319a713eb810b"), // Steve
            create("a3bd16079f764cd541e072e888fe43885e711f98658323db0f9a6045da91ee7a"), // Sunny
            create("f5dddb41dcafef616e959c2817808e0be741c89ffbfed39134a13e75b811863d") // Zuri
    };

    private static final PlayerSkin[] DEFAULT_SKINS_OLD = new PlayerSkin[] {
            create("1a4af718455d4aab528e7a61f86fa25e6a369d1768dcb13f7df319a713eb810b"), // Steve
            create("3b60a1f6d562f52aaebbf1434f1de147933a3affe0e764fa49ea057536623cd3") // Alex
    };

    private static PlayerSkin create(final String textureId) {
        return new PlayerSkin(textureId, true);
    }

    /**
     * Get the default skin from a player's UUID.
     * @param uuid UUID of the player.
     * @return The default skin of the player.
     */
    public static PlayerSkin get(final UUID uuid) {
        if (NotBounties.isAboveVersion(19,2)) {
            int vanillaIndex = Math.floorMod(uuid.hashCode(), DEFAULT_SKINS.length * 2);
            return DEFAULT_SKINS[vanillaIndex % DEFAULT_SKINS.length];
        } else {
            boolean useAlex = (uuid.hashCode() & 1) == 1;
            return useAlex ? DEFAULT_SKINS_OLD[1] : DEFAULT_SKINS_OLD[0];
        }
    }

}
