package me.jadenp.notbounties.features.settings.money;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.data.Bounty;
import me.jadenp.notbounties.data.Setter;
import me.jadenp.notbounties.features.ConfigOptions;
import me.jadenp.notbounties.features.LanguageOptions;
import me.jadenp.notbounties.features.MessageContext;
import me.jadenp.notbounties.features.Messages;
import me.jadenp.notbounties.utils.DataManager;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static me.jadenp.notbounties.features.LanguageOptions.getListMessage;
import static me.jadenp.notbounties.features.LanguageOptions.getMessage;

public class Vouchers {

    private static final NamespacedKey VOUCHER_UNIQUE_KEY = new NamespacedKey(NotBounties.getInstance(), "voucher_key");
    private static final NamespacedKey VOUCHER_PRICE_KEY = new NamespacedKey(NotBounties.getInstance(), "voucher_price");
    private static final Set<UUID> usedKeys = new HashSet<>();

    /**
     * Checks if the item held is a voucher and if the key is valid.
     * If the voucher is valid, the money is redeemed and the item removed.
     * @param player Player who is redeeming the voucher.
     * @return True if the voucher was redeemed.
     */
    public static boolean redeemHeldKey(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.PAPER || item.getItemMeta() == null)
            return false;
        ItemMeta meta = item.getItemMeta();
        String uniqueKey = meta.getPersistentDataContainer().get(VOUCHER_UNIQUE_KEY, PersistentDataType.STRING);
        String price = meta.getPersistentDataContainer().get(VOUCHER_PRICE_KEY, PersistentDataType.STRING);
        if (uniqueKey == null || price == null)
            return false;
        UUID uniqueId;
        try {
            uniqueId = UUID.fromString(uniqueKey);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (usedKeys.contains(uniqueId)) {
            Bukkit.getLogger().warning(() -> player.getName() + "{0} has a duplicate bounty voucher key of {1}" + price);
            return false;
        }

        // voucher has been validated
        Map<Integer, ItemStack> unRemoved = player.getInventory().removeItem(item);
        if (!unRemoved.isEmpty()) {
            // failed to remove item
            return false;
        }
        usedKeys.add(uniqueId);
        // TODO: Give player voucher same way old version did

        return true;
    }

    public static void giveVoucherPerSetter(@NonNull Player player, Player killer, Bounty rewardedBounty) {
        // must be called in sync to get setter offline player
        NotBounties.debugMessage("Handing out vouchers to setters.", false);
        // multiple vouchers

        for (Setter setter : rewardedBounty.getSetters()) {
            if (!setter.canClaim(killer)
                    || setter.getAmount() <= 0.01
                    || (setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID) && NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.PARTIAL))
                continue;
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            assert meta != null;
            OfflinePlayer setterPlayer = Bukkit.getOfflinePlayer(setter.getUuid());
            List<CompletableFuture<String>> loreFutures = new ArrayList<>();
            CompletableFuture<String> displayNameFuture = Messages.parse(LanguageOptions.getMessage("bounty-voucher-name"), MessageContext.builder().bounty(rewardedBounty.getTotalBounty(killer)).player(killer).amount(setter.getAmount()).receiver(player).build());
            loreFutures.add(displayNameFuture);
            for (String str : getListMessage("bounty-voucher-lore")) {
                loreFutures.add(Messages.parse(str, MessageContext.builder().bounty(rewardedBounty.getTotalDisplayBounty()).player(setterPlayer).amount(setter.getAmount()).receiver(player).build()));
            }
            if (!ConfigOptions.getMoney().getRedeemRewardLater().getSetterLoreAddition().isEmpty()) {
                loreFutures.add(Messages.parse(ConfigOptions.getMoney().getRedeemRewardLater().getSetterLoreAddition(), MessageContext.builder().bounty(rewardedBounty.getTotalBounty(killer)).amount(setter.getAmount()).receiver(setterPlayer).build()));
            }
            meta.getPersistentDataContainer().set(VOUCHER_UNIQUE_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
            meta.getPersistentDataContainer().set(VOUCHER_PRICE_KEY, PersistentDataType.DOUBLE, setter.getAmount());
            sendFutureItem(player, item, meta, loreFutures);
        }
    }

    public static void giveOneVoucher(@NonNull Player player, Player killer, Bounty rewardedBounty) {
        NotBounties.debugMessage("Handing out a voucher.", false);
        // one voucher
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        List<CompletableFuture<String>> loreFutures = new ArrayList<>();
        CompletableFuture<String> displayNameFuture = Messages.parse(LanguageOptions.getMessage("bounty-voucher-name"), MessageContext.builder().amount(rewardedBounty.getTotalBounty()).bounty(rewardedBounty).player(killer).receiver(player).build());
        loreFutures.add(displayNameFuture);
        for (String str : getListMessage("bounty-voucher-lore")) {
            loreFutures.add(Messages.parse(str, MessageContext.builder().amount(rewardedBounty.getTotalBounty()).bounty(rewardedBounty).player(killer).receiver(player).build()));
        }

        if (!ConfigOptions.getMoney().getRedeemRewardLater().getSetterLoreAddition().isEmpty()) {
            for (Setter setter : rewardedBounty.getSetters()) {
                if (!setter.canClaim(killer) || setter.getAmount() <= 0.01 || (setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID) && NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.PARTIAL))
                    continue;
                OfflinePlayer setterPlayer = Bukkit.getOfflinePlayer(setter.getUuid());
                loreFutures.add(Messages.parse(ConfigOptions.getMoney().getRedeemRewardLater().getSetterLoreAddition(), MessageContext.builder().amount(setter.getAmount()).bounty(rewardedBounty).player(killer).receiver(setterPlayer).build()));
            }
        }
        meta.getPersistentDataContainer().set(VOUCHER_UNIQUE_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(VOUCHER_PRICE_KEY, PersistentDataType.DOUBLE, rewardedBounty.getTotalBounty());
        sendFutureItem(killer, item, meta, loreFutures);
    }

    private static void sendFutureItem(Player killer, ItemStack item, ItemMeta meta, List<CompletableFuture<String>> loreFutures) {
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.addUnsafeEnchantment(Enchantment.CHANNELING, 0);
        CompletableFuture.allOf(loreFutures.toArray(new CompletableFuture[0])).thenAccept(v -> {
            meta.setDisplayName(loreFutures.getFirst().join());
            List<String> lore = new ArrayList<>();
            for (int i = 1; i < loreFutures.size(); i++) {
                lore.add(loreFutures.get(i).join());
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            NumberFormatting.givePlayer(killer, item, 1);
        });
    }
}
