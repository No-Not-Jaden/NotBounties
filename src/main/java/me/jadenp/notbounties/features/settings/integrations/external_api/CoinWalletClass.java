package me.jadenp.notbounties.features.settings.integrations.external_api;

import me.hboj.api.CoinWalletAPI;
import me.hboj.api.CoinWalletAPIProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class CoinWalletClass {
    private CoinWalletAPI api;
    private boolean working;

    public CoinWalletClass() {
        try {
            if (!CoinWalletAPIProvider.isAvailable()) {
                Bukkit.getLogger().warning("[NotBounties-CoinWallet] API not available");
                working = false;
                return;
            }
            api = CoinWalletAPIProvider.get();
            if (api == null) {
                Bukkit.getLogger().warning("[NotBounties-CoinWallet] get() returned null");
                working = false;
                return;
            }
            working = true;
            Bukkit.getLogger().info("[NotBounties-CoinWallet] Initialized OK via direct API");
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[NotBounties-CoinWallet] Init failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            working = false;
        }
    }

    public boolean isWorking() {
        return working;
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!working) return false;
        try {
            int intAmount = (int) amount;
            CoinWalletAPI.TransactionResult result = api.addCoins(player, CoinWalletAPI.CoinUnit.GOLD, intAmount);
            boolean success = result.isSuccess();
            if (!success)
                Bukkit.getLogger().warning("[NotBounties-CoinWallet] deposit FAILED: " + result.getMessage() + " (balance=" + result.getNewBalance() + ")");
            return success;
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[NotBounties-CoinWallet] deposit EXCEPTION: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            working = false;
            return false;
        }
    }

    public boolean withdraw(Player player, double amount) {
        if (!working) return false;
        try {
            int intAmount = (int) amount;
            CoinWalletAPI.TransactionResult result = api.removeCoins(player, CoinWalletAPI.CoinUnit.GOLD, intAmount);
            boolean success = result.isSuccess();
            if (!success)
                Bukkit.getLogger().warning("[NotBounties-CoinWallet] withdraw FAILED: " + result.getMessage() + " (balance=" + result.getNewBalance() + ")");
            return success;
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[NotBounties-CoinWallet] withdraw EXCEPTION: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            working = false;
            return false;
        }
    }

    public boolean checkBalance(OfflinePlayer player, double amount) {
        if (!working) return false;
        try {
            int goldValue = CoinWalletAPI.CoinUnit.GOLD.getValue();
            if (player.isOnline() && player.getPlayer() != null)
                return api.has(player.getPlayer(), (int) amount * goldValue);
            return api.getBalance(player) >= (int) amount * goldValue;
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[NotBounties-CoinWallet] checkBalance EXCEPTION: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            working = false;
            return false;
        }
    }

    public double getBalance(OfflinePlayer player) {
        if (!working) return 0;
        try {
            return api.getBalance(player) / (double) CoinWalletAPI.CoinUnit.GOLD.getValue();
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[NotBounties-CoinWallet] getBalance EXCEPTION: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            working = false;
            return 0;
        }
    }
}
