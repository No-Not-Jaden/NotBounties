package me.jadenp.notbounties.features.settings.integrations;

import me.jadenp.notbounties.features.settings.integrations.external_api.BountyContextCalculator;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public class LuckPermsClass {

    public static void onEnable() {
        LuckPerms api = LuckPermsProvider.get();
        api.getContextManager().registerCalculator(new BountyContextCalculator());
    }

}
