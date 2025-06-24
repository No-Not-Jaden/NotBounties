package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.bounty_events.BountyClaimEvent;
import me.jadenp.notbounties.bounty_events.BountySetEvent;
import me.jadenp.notbounties.data.player_data.AmountRefund;
import me.jadenp.notbounties.data.player_data.ItemRefund;
import me.jadenp.notbounties.data.player_data.PlayerData;
import me.jadenp.notbounties.data.player_data.RewardHead;
import me.jadenp.notbounties.features.settings.auto_bounties.BigBounty;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.integrations.BountyClaimRequirements;
import me.jadenp.notbounties.features.settings.money.NotEnoughCurrencyException;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.features.settings.auto_bounties.TrickleBounties;
import me.jadenp.notbounties.utils.tasks.BroadcastTask;
import me.jadenp.notbounties.utils.tasks.DelayedReward;
import me.jadenp.notbounties.features.*;
import me.jadenp.notbounties.features.settings.auto_bounties.MurderBounties;
import me.jadenp.notbounties.features.settings.auto_bounties.TimedBounties;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.integrations.external_api.MMOLibClass;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.*;


import static me.jadenp.notbounties.features.LanguageOptions.*;
import static me.jadenp.notbounties.features.settings.money.NumberFormatting.*;

public class BountyManager {

    private static final int BOUNTY_LIST_LENGTH = 10;

    private BountyManager(){}

    public static void listBounties(CommandSender sender, int page) {
        GUIOptions guiOptions = GUI.getGUI("bounty-gui");
        String title = "";
        if (guiOptions != null) {
            title = guiOptions.getName();
            if (guiOptions.isAddPage())
                title += " " + (page + 1);
        }
        sender.sendMessage(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "               " + ChatColor.RESET + " " + title + " " + ChatColor.GRAY + ChatColor.STRIKETHROUGH + "               ");
        int sortType = Objects.requireNonNull(GUI.getGUI("bounty-gui")).getSortType();
        List<Bounty> sortedList = getAllBounties(sortType);
        for (int i = page * BOUNTY_LIST_LENGTH; i < (page * BOUNTY_LIST_LENGTH) + BOUNTY_LIST_LENGTH; i++) {
            if (sortedList.size() > i) {
                sender.sendMessage(parse(getMessage("list-total"), sortedList.get(i).getTotalDisplayBounty(), Bukkit.getOfflinePlayer(sortedList.get(i).getUUID())));
            } else {
                break;
            }
        }

        TextComponent rightArrow = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⋙⋙⋙");
        rightArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " list " + page + 2));
        rightArrow.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(net.md_5.bungee.api.ChatColor.of(new Color(232, 26, 225)) + "Next Page")));
        TextComponent leftArrow = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⋘⋘⋘");
        leftArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + ConfigOptions.getPluginBountyCommands().get(0) + " list " + page));
        leftArrow.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(net.md_5.bungee.api.ChatColor.of(new Color(232, 26, 225)) + "Last Page")));
        TextComponent space = new TextComponent(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "                        ");
        TextComponent titleFill = new TextComponent(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + " ".repeat(title.length()));
        TextComponent replacement = new TextComponent(ChatColor.GRAY + "" + ChatColor.STRIKETHROUGH + "    ");

        TextComponent start = new TextComponent("");
        if (page > 0) {
            start.addExtra(leftArrow);
        } else {
            start.addExtra(replacement);
        }
        start.addExtra(space);
        start.addExtra(titleFill);
        if (sortedList.size() > (page * BOUNTY_LIST_LENGTH) + BOUNTY_LIST_LENGTH) {
            start.addExtra(rightArrow);
        } else {
            start.addExtra(replacement);
        }
        sender.spigot().sendMessage(start);
    }


    public static void addBounty(Player setter, OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        double displayAmount = amount;
        // you can only set bounties by items or amount, if there are items, the amount is just the value of the items
        if (!items.isEmpty())
            amount = 0;
        // add to all time bounties
        BountySetEvent event = new BountySetEvent(new Bounty(setter, receiver, amount, items, whitelist));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            refundBounty(new Bounty(setter, receiver, amount + amount * ConfigOptions.getMoney().getBountyTax() + Whitelist.getCost() * whitelist.getList().size(), items, whitelist), LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-bounty-cancel"), receiver));
            return;
        }
        // unlock recipes
        if (!setter.hasDiscoveredRecipe(BountyTracker.getBountyTrackerRecipe()))
            setter.discoverRecipe(BountyTracker.getBountyTrackerRecipe());

        // add stat
        DataManager.changeStat(setter.getUniqueId(), Leaderboard.SET, 1);

        DataManager.changeStat(receiver.getUniqueId(), Leaderboard.ALL, displayAmount);
        Bounty bounty = DataManager.insertBounty(setter, receiver, amount, items, whitelist);

        registerBounty(receiver, displayAmount, bounty);

        BroadcastTask broadcastTask = new BroadcastTask(setter, receiver, displayAmount, bounty.getTotalDisplayBounty(), whitelist);
        broadcastTask.setTaskImplementation(NotBounties.getServerImplementation().async().runAtFixedRate(broadcastTask,1,4));

        ActionCommands.executeBountySet(receiver.getUniqueId(), setter, bounty);
        if (ConfigOptions.getBountyCooldown() > 0)
            DataManager.getPlayerData(setter.getUniqueId()).setBountyCooldown(System.currentTimeMillis());
    }

    private static void registerBounty(OfflinePlayer receiver, double displayAmount, Bounty bounty) {
        if (ConfigOptions.getIntegrations().isMmoLibEnabled() && receiver.isOnline()) {
            MMOLibClass.removeStats(receiver.getPlayer());
            MMOLibClass.addStats(receiver.getPlayer(), displayAmount);
        }

        if (receiver.isOnline()) {
            Player onlineReceiver = receiver.getPlayer();
            assert onlineReceiver != null;
            // check for big bounty
            BigBounty.setBounty(onlineReceiver, bounty, displayAmount);
            // add wanted tag
            if (WantedTags.isEnabled() && bounty.getTotalDisplayBounty() >= WantedTags.getMinWanted()) {
                WantedTags.addWantedTag(onlineReceiver);
            }
        }
    }

    public static void addBounty(OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {

        double displayAmount = amount;
        // you can only set bounties by items or amount, if there are items, the amount is just the value of the items
        if (!items.isEmpty())
            amount = 0;
        // add to all time bounties
        BountySetEvent event = new BountySetEvent(new Bounty(receiver, amount, items, whitelist));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            refundBounty(new Bounty(receiver, amount + amount * ConfigOptions.getMoney().getBountyTax() + Whitelist.getCost() * whitelist.getList().size(), items, whitelist), LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-bounty-cancel"), receiver));
            return;
        }

        DataManager.changeStat(receiver.getUniqueId(), Leaderboard.ALL, displayAmount);
        Bounty bounty = DataManager.insertBounty(null, receiver, amount, items, whitelist);

        registerBounty(receiver, displayAmount, bounty);

        BroadcastTask broadcastTask = new BroadcastTask(null, receiver, displayAmount, bounty.getTotalDisplayBounty(), whitelist);
        broadcastTask.setTaskImplementation(NotBounties.getServerImplementation().async().runAtFixedRate(broadcastTask,1,4));
    }




    public static void refundBounty(Bounty bounty, String reason) {
        for (Setter setter : bounty.getSetters()) {
            refundSetter(setter, reason);
        }
    }

    public static void refundSetter(Setter setter, String reason) {
        refundPlayer(setter.getUuid(), setter.getAmount(), setter.getItems(), reason);
    }

    public static void refundPlayer(UUID uuid, double amount, List<ItemStack> items, String reason) {
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID))
            return;
        items = new ArrayList<>(items); // make the arraylist modifiable
        Player player = Bukkit.getPlayer(uuid);
        boolean refund = false;
        // refund amount
        if (amount > 0) {
            if (NumberFormatting.isVaultEnabled() && !NumberFormatting.isOverrideVault()) {
                if (!NumberFormatting.getVaultClass().deposit(player, amount)) {
                    Bukkit.getLogger().warning("[NotBounties] Error depositing currency with vault!");
                    addRefund(uuid, amount, reason);
                    refund = true;
                }
            } else {
                if (player != null && NotBounties.getInstance().isEnabled()) {
                    if (NumberFormatting.getManualEconomy() != ManualEconomy.PARTIAL)
                        NumberFormatting.doAddCommands(player, amount);
                } else {
                    addRefund(uuid, amount, reason);
                    refund = true;
                }
            }
        }
        // refund items
        items.removeIf(Objects::isNull);
        if (!items.isEmpty() && NumberFormatting.getManualEconomy() == ManualEconomy.AUTOMATIC) {
            if (player != null && NotBounties.getInstance().isEnabled()) {
                NumberFormatting.givePlayer(player.getPlayer(), items, false);
            } else {
                addRefund(uuid, items, reason);
                refund = true;
            }
        }

        if (refund)
            // sync player data with the most up-to-date database
            NotBounties.getServerImplementation().async().runNow(() -> DataManager.syncPlayerData(uuid, null));
    }

    private static void addRefund(UUID uuid, double amount, String reason) {
            DataManager.getPlayerData(uuid).addRefund(new AmountRefund(amount, reason));
    }

    private static void addRefund(UUID uuid, List<ItemStack> items, String reason) {
            DataManager.getPlayerData(uuid).addRefund(new ItemRefund(items, reason));
    }

    public static List<Bounty> getPublicBounties(int sortType) {
        List<Bounty> bounties = DataManager.getAllBounties(sortType);
        bounties.removeIf(bounty -> ConfigOptions.getHiddenNames().contains(bounty.getName()));
        return bounties;
    }

    public static List<Bounty> getAllBounties(int sortType) {
        return DataManager.getAllBounties(sortType);
    }

    public static Bounty getBounty(UUID uuid) {
        return DataManager.getBounty(uuid);
    }

    public static boolean hasBounty(UUID receiver) {
        return DataManager.hasBounty(receiver);
    }


    public static void removeBounty(UUID uuid) {
        BountyTracker.stopTracking(uuid);
        for (Player player : Bukkit.getOnlinePlayers()) {
            BountyTracker.removeTracker(player);
            if (player.getUniqueId().equals(uuid) && ConfigOptions.getIntegrations().isMmoLibEnabled()) {
                MMOLibClass.removeStats(player);
            }
        }
        WantedTags.removeWantedTag(uuid);
        BigBounty.removeParticle(uuid);
        DataManager.deleteBounty(uuid);
    }


    public static boolean editBounty(@NotNull Bounty bounty, @Nullable UUID setterUUID, double change) {
        // remove particle if bounty reduced under threshold
        BigBounty.bigBountyCheck(bounty, change);
        return DataManager.editBounty(bounty, setterUUID, change) != null;

    }




    /**
     * Called when a player dies, this function determines if a bounty can be claimed, and hands out rewards if so.
     * @param player Player that was killed.
     * @param killer Player that killed.
     * @param drops Items that were dropped/
     * @param forceEditDrops Whether the player's inventory should be forcibly edited after the drops are modified.
     */
    public static void claimBounty(@NotNull Player player, Player killer, List<ItemStack> drops, boolean forceEditDrops, double deathTax) {
        NotBounties.debugMessage("Received a bounty claim request.", false);
        Item droppedHead = null;
        if (RewardHead.isRewardAnyKill())
            droppedHead = player.getWorld().dropItemNaturally(player.getLocation(), Head.createPlayerSkull(player.getUniqueId(), SkinManager.getSkin(player.getUniqueId()).url()));
        // possible remove this later when the other functions allow null killers aka non-player deaths
        if (killer == null)
            return;
        NotBounties.debugMessage(killer.getName() + " killed " + player.getName(), false);

        // check if a bounty can be claimed
        if (!BountyClaimRequirements.canClaim(player, killer)) {
            NotBounties.debugMessage("An external plugin, world filter, or a shared team is preventing this bounty from being claimed.", false);
            return;
        }
        if (player == killer) {
            NotBounties.debugMessage("Player killed themself. D:", false);
            return;
        }

        TimedBounties.onDeath(player); // reset next bounty timer for being killed
        MurderBounties.killPlayer(player, killer); // possibly add bounty on killer

        // check if killer can steal a bounty
        // stealing a bounty is killing someone that placed a bounty on you, and taking those rewards.
        if (hasBounty(killer.getUniqueId()) && ConfigOptions.isStealBounties()) {
            Bounty bounty = getBounty(killer.getUniqueId());
            assert bounty != null;
            Bounty stolenBounty = bounty.getBounty(player.getUniqueId());
            // update the bounty
            DataManager.removeSetters(bounty, stolenBounty.getSetters());
            if (!stolenBounty.getSetters().isEmpty()) {
                // bounty has been stolen
                NotBounties.debugMessage("Killer stole a bounty!", false);
                if (NumberFormatting.getManualEconomy() == ManualEconomy.AUTOMATIC) {
                    // give rewards
                    NotBounties.debugMessage("Giving stolen bounty.", false);
                    NumberFormatting.doAddCommands(killer, stolenBounty.getTotalBounty());
                    NumberFormatting.givePlayer(killer, bounty.getTotalItemBounty(), false);
                }
                // send messages
                killer.sendMessage(parse(getPrefix() + LanguageOptions.getMessage("stolen-bounty"), stolenBounty.getTotalDisplayBounty(), player));
                // send messages
                String message = parse(getPrefix() + getMessage("stolen-bounty-broadcast"), player, stolenBounty.getTotalDisplayBounty(), bounty.getTotalDisplayBounty(), killer);
                Bukkit.getConsoleSender().sendMessage(message);
                if (stolenBounty.getTotalDisplayBounty() >= ConfigOptions.getMoney().getMinBroadcast())
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (DataManager.getPlayerData(player.getUniqueId()).getBroadcastSettings() != PlayerData.BroadcastSettings.DISABLE && !p.getUniqueId().equals(killer.getUniqueId())) {
                            p.sendMessage(message);
                        }
                    }
                // play sound
                killer.getWorld().playSound(player.getLocation(), Sound.ENTITY_CAT_HISS, 1, 1);
            }
        }
        Bounty bounty = DataManager.getGuarrenteedBounty(player.getUniqueId());
        if (bounty == null) {
            NotBounties.debugMessage("Player doesn't have a bounty.", false);
            return;
        }

        // check if it is a npc
        if (!ConfigOptions.isNpcClaim() && killer.hasMetadata("NPC")) {
            NotBounties.debugMessage("This is an NPC, which bounty claiming is disabled for in the config.", false);
            return;
        }

        // check if killer can claim it
        if (bounty.getTotalDisplayBounty(killer) < 0.01 && bounty.getTotalItemBounty(killer).isEmpty()) {
            NotBounties.debugMessage("This bounty is too small, and there are no items attached!", false);
            return;
        }
        NotBounties.debugMessage("Bounty to be claimed: " + bounty.getTotalDisplayBounty(killer), false);

        BountyClaimEvent event1 = new BountyClaimEvent(killer, new Bounty(bounty));
        Bukkit.getPluginManager().callEvent(event1);
        if (event1.isCancelled()) {
            NotBounties.debugMessage("The bounty event got canceled by an external plugin.", false);
            return;
        }

        // can now begin the bounty claim

        PVPRestrictions.onBountyClaim(player); // make combat safe if enabled
        final Bounty claimedBounty = new Bounty(bounty, killer.getUniqueId()); // create a copy of the claimed part of the bounty (for reference later)
        // get a copy of all the setters that are to be claimed
        List<Setter> claimedBounties = new ArrayList<>(claimedBounty.getSetters());
        //claimedBounties.removeIf(setter -> !setter.canClaim(killer)); // this shouldn't do anything

        // broadcast message
        String message = parse(getPrefix() + getMessage("claim-bounty-broadcast"), killer, bounty.getTotalDisplayBounty(killer), player);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if ((DataManager.getPlayerData(player.getUniqueId()).getBroadcastSettings() != PlayerData.BroadcastSettings.DISABLE && bounty.getTotalDisplayBounty(killer) >= ConfigOptions.getMoney().getMinBroadcast()) || p.getUniqueId().equals(player.getUniqueId()) || p.getUniqueId().equals(Objects.requireNonNull(killer).getUniqueId())) {
                p.sendMessage(message);
            }
        }
        NotBounties.debugMessage("Claim messages sent to all players.", false);

        // hand out reward heads
        RewardHead rewardHead = new RewardHead(player.getUniqueId(), killer.getUniqueId(), bounty.getTotalDisplayBounty(killer), LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-reward-head"), player));
        if (RewardHead.isRewardSetters()) {
            // reward head for setters
            Set<UUID> givenHead = new HashSet<>(); // record whose head has been given out
            givenHead.add(DataManager.GLOBAL_SERVER_ID); // console id added so a head isn't attempted to be given to it
            for (Setter setter : claimedBounties) {
                if (!givenHead.contains(setter.getUuid())) {
                    givenHead.add(setter.getUuid());
                    Player p = Bukkit.getPlayer(setter.getUuid());
                    if (p != null) {
                        // setter is online
                        if (killer.getUniqueId().equals(setter.getUuid()) && droppedHead != null)
                            // if the killer is a setter, remove the dropped head, so they only get one for being a setter
                            droppedHead.remove();
                        // check to make sure the setter isn't the killer and won't get another head for claiming
                        if (!RewardHead.isRewardKiller() || !Objects.requireNonNull(killer).getUniqueId().equals(setter.getUuid())) {
                            rewardHead.giveRefund(p); // give head ;)
                            NotBounties.debugMessage("Gave setter " + p.getName() + " a player skull for the bounty.", false);
                        }
                    } else {
                        // Setter is offline.
                        // Save reward head to player data.
                        PlayerData playerData = DataManager.getPlayerData(setter.getUuid());
                        playerData.addRefund(rewardHead);
                        NotBounties.getServerImplementation().async().runNow(() -> DataManager.syncPlayerData(playerData.getUuid(), null));
                        NotBounties.debugMessage("Will give " + playerData.getPlayerName() + " a player skull when they log on next for the bounty.", false);
                    }
                }
            }
        }
        if (RewardHead.isRewardKiller()) {
            // reward head for killer
            if (droppedHead != null)
                // if a head was dropped for killing a player, remove it to be replaced with a custom head
                droppedHead.remove();

            rewardHead.giveRefund(killer);
            NotBounties.debugMessage("Gave killer " + killer.getName() + " a player skull for the bounty.", false);
        }

        // death tax
        if (deathTax > 0 && NumberFormatting.getManualEconomy() != NumberFormatting.ManualEconomy.PARTIAL) {
            NotBounties.debugMessage("Removing " + bounty.getTotalDisplayBounty(killer) * deathTax + " currency for death tax", false);
            // attempt to remove currency/items from the player's inventory
            // I think these are the items that should have been removed from the player, but might not have been since they died.
            Map<Material, Long> removedItems = new HashMap<>();
            try {
                removedItems = NumberFormatting.doRemoveCommands(player, bounty.getTotalDisplayBounty(killer) * deathTax, drops);
            } catch (NotEnoughCurrencyException e) {
                NotBounties.debugMessage("Player does not have enough currency for the death tax", false);
                try {
                    removedItems = NumberFormatting.doRemoveCommands(player, Math.min(bounty.getTotalDisplayBounty(killer) * deathTax, getBalance(killer)), drops);
                } catch (NotEnoughCurrencyException e1) {
                    NotBounties.debugMessage("Could not remove player's balance.", false);
                }
            }
            if (!removedItems.isEmpty()) {
                // send message
                long totalLoss = 0;
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<Material, Long> entry : removedItems.entrySet()) {
                    builder.append(entry.getValue()).append("x").append(entry.getKey().toString()).append(", ");
                    totalLoss += entry.getValue();
                }
                builder.replace(builder.length() - 2, builder.length(), "");
                if (totalLoss > 0) {
                    NotBounties.debugMessage("Removing " + totalLoss + " currency for the death tax.", false);
                    player.sendMessage(parse(getPrefix() + LanguageOptions.getMessage("death-tax").replace("{items}", (builder.toString())), player));
                    // modify drops
                    if (forceEditDrops)
                        for (Map.Entry<Material, Long> entry : removedItems.entrySet())
                            NumberFormatting.removeItem(player, entry.getKey(), entry.getValue(), -1);
                    ListIterator<ItemStack> dropsIterator = drops.listIterator();
                    while (dropsIterator.hasNext()) {
                        ItemStack drop = dropsIterator.next();
                        if (removedItems.containsKey(drop.getType())) {
                            if (removedItems.get(drop.getType()) > drop.getAmount()) {
                                removedItems.replace(drop.getType(), removedItems.get(drop.getType()) - drop.getAmount());
                                dropsIterator.remove();
                            } else if (removedItems.get(drop.getType()) == drop.getAmount()) {
                                removedItems.remove(drop.getType());
                                dropsIterator.remove();
                            } else {
                                drop.setAmount((int) (drop.getAmount() - removedItems.get(drop.getType())));
                                dropsIterator.set(drop);
                                removedItems.remove(drop.getType());
                            }
                        }
                    }
                }
            }
        }
        Bounty rewardedBounty = TrickleBounties.getRewardedBounty(claimedBounty, killer);
        NotBounties.debugMessage("Redeeming Reward: of " + rewardedBounty.getTotalDisplayBounty(), false);
        NotBounties.debugMessage(rewardedBounty.toString(), false);
        if (!ConfigOptions.getMoney().getRedeemRewardLater().isVouchers()) {
            // give currency
            // check if the player is in a duel or reward later is set in the config
            if (ConfigOptions.getIntegrations().isDuelsEnabled() && ConfigOptions.getIntegrations().getDuels().isDelayReward() && ConfigOptions.getIntegrations().getDuels().isInDuel(killer)) {
                // delayed reward because killer is in duel
                giveDelayedReward(killer, rewardedBounty, ConfigOptions.getIntegrations().getDuels().getTeleportDelay() * 1000 + 100);
            } else if (ConfigOptions.getMoney().getRedeemRewardLater().getRewardDelay() > 0) {
                // delayed reward from config
                giveDelayedReward(killer, rewardedBounty, ConfigOptions.getMoney().getRedeemRewardLater().getRewardDelay() * 1000);
            } else {
                // give reward now
                rewardBounty(killer.getUniqueId(), rewardedBounty);
            }

        } else {
            // will add these to a voucher later, or will I?
            if (NumberFormatting.getManualEconomy() == ManualEconomy.AUTOMATIC)
                NumberFormatting.givePlayer(killer, bounty.getTotalItemBounty(killer), false); // give bountied items
            // give voucher
            if (NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.PARTIAL) {
                // auto bounty reward
                NumberFormatting.doAddCommands(killer, rewardedBounty.getBounty(DataManager.GLOBAL_SERVER_ID).getTotalBounty(killer));
            }
            if (ConfigOptions.getMoney().getRedeemRewardLater().isVoucherPerSetter()) {
                NotBounties.debugMessage("Handing out vouchers.", false);
                // multiple vouchers
                for (Setter setter : rewardedBounty.getSetters()) {
                    if (!setter.canClaim(killer))
                        continue;
                    if (setter.getAmount() <= 0.01)
                        continue;
                    if (setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID) && NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.PARTIAL)
                        continue;
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    assert meta != null;
                    ArrayList<String> lore = new ArrayList<>();
                    for (String str : getListMessage("bounty-voucher-lore")) {
                        lore.add(parse(str.replace("{bounty}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(rewardedBounty.getTotalBounty(killer)) + NumberFormatting.getCurrencySuffix())), Bukkit.getOfflinePlayer(setter.getUuid()),setter.getAmount(), player));
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.setDisplayName(parse(getMessage("bounty-voucher-name").replace("{bounty}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(rewardedBounty.getTotalBounty(killer)) + NumberFormatting.getCurrencySuffix())), killer, setter.getAmount(), player));
                    ArrayList<String> setterLore = new ArrayList<>(lore);
                    if (!ConfigOptions.getMoney().getRedeemRewardLater().getSetterLoreAddition().isEmpty()) {
                        setterLore.add(parse(ConfigOptions.getMoney().getRedeemRewardLater().getSetterLoreAddition(), setter.getAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    }
                    setterLore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + setter.getAmount());
                    meta.setLore(setterLore);
                    item.setItemMeta(meta);
                    item.addUnsafeEnchantment(Enchantment.CHANNELING, 0);
                    NumberFormatting.givePlayer(killer, item, 1);
                }
            } else {
                NotBounties.debugMessage("Handing out a voucher.", false);
                // one voucher
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                assert meta != null;
                ArrayList<String> lore = new ArrayList<>();
                for (String str : getListMessage("bounty-voucher-lore")) {
                    lore.add(parse(str, player.getKiller(), rewardedBounty.getTotalBounty(killer), player));
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.setDisplayName(parse(getMessage("bounty-voucher-name"), killer, rewardedBounty.getTotalBounty(killer), player));
                if (!ConfigOptions.getMoney().getRedeemRewardLater().getSetterLoreAddition().isEmpty()) {
                    for (Setter setter : rewardedBounty.getSetters()) {
                        if (!setter.canClaim(killer) || setter.getAmount() <= 0.01 || (setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID) && NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.PARTIAL))
                            continue;
                        lore.add(parse(ConfigOptions.getMoney().getRedeemRewardLater().getSetterLoreAddition(), setter.getAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    }
                }
                lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + bounty.getTotalBounty(killer));
                meta.setLore(lore);
                item.setItemMeta(meta);
                item.addUnsafeEnchantment(Enchantment.CHANNELING, 0);
                NumberFormatting.givePlayer(killer, item, 1);
            }
        }

        DataManager.changeStat(player.getUniqueId(), Leaderboard.DEATHS, 1);
        DataManager.changeStats(killer.getUniqueId(), new PlayerStat(1,0,0,0,0, bounty.getTotalDisplayBounty(killer)));
        NotBounties.debugMessage("Given stats.", false);
        Bounty transferedBounty = TrickleBounties.transferBounty(bounty, killer);
        // modify the bounty to remove the rewarded setters.
        List<Setter> removedSetters = new LinkedList<>(rewardedBounty.getSetters());
        removedSetters.addAll(transferedBounty.getSetters());

        DataManager.removeSetters(bounty, removedSetters);


        // the bounty object is no longer accurate of the current bounty



        // play sound for setters
        for (Setter setter : claimedBounties) {
            if (!setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID)) {
                Player p = Bukkit.getPlayer(setter.getUuid());
                if (p != null) {
                    p.playSound(p.getEyeLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 1, 1);
                }
            }
        }
        ImmunityManager.startGracePeriod(player);
        GUI.reopenBountiesGUI();
        ActionCommands.executeBountyClaim(player, killer, claimedBounty);
    }

    /**
     * Gives the player a reward at a later time. All delayed rewards are handed out if the server restarts.
     * @param player Player to give the reward to.
     * @param bounty Bounty to be rewarded.
     * @param delayMS The delay in milliseconds before the reward is given.
     */
    private static void giveDelayedReward(Player player, Bounty bounty, int delayMS) {
        NotBounties.debugMessage("Delaying the reward for " + player.getName() + " by "
                + LocalTime.formatTime(delayMS, LocalTime.TimeFormat.RELATIVE), false);

        DelayedReward task = new DelayedReward(bounty, player);
        task.setTaskImplementation(NotBounties.getServerImplementation().async().runDelayed(task, delayMS / 50));
    }

    /**
     * Reward a player with a claimed bounty
     * @param uuid
     * @param bounty
     */
    public static void rewardBounty(UUID uuid, Bounty bounty) {
        if (NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.PARTIAL) {
            // partial economy means only auto bounties are given by the console
            NotBounties.debugMessage("(Partial Economy) Directly giving auto-bounty reward.", false);
            refundPlayer(uuid, bounty.getBounty(DataManager.GLOBAL_SERVER_ID).getTotalBounty(uuid), Collections.emptyList(), null);
        } else {
            NotBounties.debugMessage("Directly giving total claimed bounty.", false);
            double rewardAmount = bounty.getTotalBounty(uuid);
            List<ItemStack> rewardItems = getManualEconomy() == ManualEconomy.AUTOMATIC ? bounty.getTotalItemBounty(uuid) : Collections.emptyList();
            refundPlayer(uuid, rewardAmount, rewardItems, null);
        }
    }

}
