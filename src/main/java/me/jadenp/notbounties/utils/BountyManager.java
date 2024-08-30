package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.Setter;
import me.jadenp.notbounties.bountyEvents.BountyClaimEvent;
import me.jadenp.notbounties.bountyEvents.BountySetEvent;
import me.jadenp.notbounties.ui.BountyTracker;
import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.ui.gui.GUIOptions;
import me.jadenp.notbounties.utils.configuration.*;
import me.jadenp.notbounties.utils.configuration.autoBounties.MurderBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.TimedBounties;
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
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.*;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.*;
import static me.jadenp.notbounties.utils.configuration.LanguageOptions.*;
import static me.jadenp.notbounties.utils.configuration.NumberFormatting.*;

public class BountyManager {

    public static final Map<UUID, Double> refundedBounties = new HashMap<>();
    public static final Map<UUID, List<ItemStack>> refundedItems = new HashMap<>();
    public static final Map<UUID, List<RewardHead>> headRewards = new HashMap<>();
    public static final Map<UUID, Long> bountyCooldowns = new HashMap<>();

    private static final int length = 10;

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
        List<Bounty> sortedList = getPublicBounties(sortType);
        for (int i = page * length; i < (page * length) + length; i++) {
            if (sortedList.size() > i) {
                sender.sendMessage(parse(listTotal, sortedList.get(i).getTotalDisplayBounty(), Bukkit.getOfflinePlayer(sortedList.get(i).getUUID())));
            } else {
                break;
            }
        }

        TextComponent rightArrow = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⋙⋙⋙");
        rightArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + pluginBountyCommands.get(0) + " list " + page + 2));
        rightArrow.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(net.md_5.bungee.api.ChatColor.of(new Color(232, 26, 225)) + "Next Page")));
        TextComponent leftArrow = new TextComponent(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "⋘⋘⋘");
        leftArrow.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + pluginBountyCommands.get(0) + " list " + page));
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
        if (sortedList.size() > (page * length) + length) {
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
            refundBounty(new Bounty(setter, receiver, amount + amount * bountyTax + bountyWhitelistCost * whitelist.getList().size(), items, whitelist));
            return;
        }
        // unlock recipies
        if (!setter.hasDiscoveredRecipe(BountyTracker.getBountyTrackerRecipe()))
            setter.discoverRecipe(BountyTracker.getBountyTrackerRecipe());

        // add stat
        DataManager.changeStat(setter.getUniqueId(), Leaderboard.SET, 1, false);

        DataManager.changeStat(receiver.getUniqueId(), Leaderboard.ALL, displayAmount, false);
        Bounty bounty = DataManager.insertBounty(setter, receiver, amount, items, whitelist);

        if (receiver.isOnline()) {
            Player onlineReceiver = receiver.getPlayer();
            assert onlineReceiver != null;
            // check for big bounty
            BigBounty.setBounty(onlineReceiver, bounty, displayAmount);
            // send messages
            onlineReceiver.sendMessage(parse(prefix + bountyReceiver, displayAmount, bounty.getTotalDisplayBounty(), Bukkit.getOfflinePlayer(setter.getUniqueId())));

            if (serverVersion <= 16) {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
            } else {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1, 1);
            }
            // add wanted tag
            if (wanted && bounty.getTotalDisplayBounty() >= minWanted) {
                if (!NotBounties.wantedText.containsKey(onlineReceiver.getUniqueId())) {
                    NotBounties.wantedText.put(onlineReceiver.getUniqueId(), new AboveNameText(onlineReceiver));
                }
            }
        }
        // send messages
        setter.sendMessage(parse(prefix + bountySuccess, displayAmount, bounty.getTotalDisplayBounty(), receiver));

        if (serverVersion <= 16) {
            setter.playSound(setter.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
        } else {
            setter.playSound(setter.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 1, 1);
        }

        String message = parse(prefix + bountyBroadcast, setter, displayAmount, bounty.getTotalDisplayBounty(), receiver);

        Bukkit.getConsoleSender().sendMessage(message);
        if (whitelist.getList().isEmpty()) {
            if (displayAmount >= minBroadcast)
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!NotBounties.disableBroadcast.contains(player.getUniqueId()) && !player.getUniqueId().equals(receiver.getUniqueId()) && !player.getUniqueId().equals(setter.getUniqueId())) {
                        player.sendMessage(message);
                    }
                }
        } else {
            if (whitelist.isBlacklist()) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getUniqueId().equals(receiver.getUniqueId()) || player.getUniqueId().equals(setter.getUniqueId()) || whitelist.getList().contains(player.getUniqueId()))
                        continue;
                    whitelistNotify.stream().filter(str -> !str.isEmpty()).forEach(str -> player.sendMessage(parse(prefix + str, player)));
                }
            } else {
                for (UUID uuid : whitelist.getList()) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || player.getUniqueId().equals(receiver.getUniqueId()) || player.getUniqueId().equals(setter.getUniqueId()))
                        continue;
                    player.sendMessage(message);
                    whitelistNotify.stream().filter(str -> !str.isEmpty()).forEach(str -> player.sendMessage(parse(prefix + str, player)));
                }
            }
        }
        ActionCommands.executeBountySet(receiver.getUniqueId(), setter, bounty);
        if (bountyCooldown > 0)
            bountyCooldowns.put(setter.getUniqueId(), System.currentTimeMillis());
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
            refundBounty(new Bounty(receiver, amount + amount * bountyTax + bountyWhitelistCost * whitelist.getList().size(), items, whitelist));
            return;
        }

        DataManager.changeStat(receiver.getUniqueId(), Leaderboard.ALL, displayAmount, false);
        Bounty bounty = DataManager.insertBounty(null, receiver, amount, items, whitelist);

        if (receiver.isOnline()) {
            Player onlineReceiver = receiver.getPlayer();
            assert onlineReceiver != null;
            // check for big bounty
            BigBounty.setBounty(onlineReceiver, bounty, displayAmount);
            // send messages
            onlineReceiver.sendMessage(parse(prefix + bountyReceiver, consoleName, displayAmount, bounty.getTotalDisplayBounty(), receiver));

            if (serverVersion <= 16) {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.ITEM_CROSSBOW_LOADING_END, 1, 1);
            } else {
                onlineReceiver.playSound(onlineReceiver.getEyeLocation(), Sound.BLOCK_AMETHYST_BLOCK_FALL, 1, 1);
            }
            // add wanted tag
            if (wanted && bounty.getTotalDisplayBounty() >= minWanted) {
                if (!NotBounties.wantedText.containsKey(onlineReceiver.getUniqueId())) {
                    NotBounties.wantedText.put(onlineReceiver.getUniqueId(), new AboveNameText(onlineReceiver));
                }
            }
        }
        // send messages
        String message = parse(prefix + bountyBroadcast, consoleName, displayAmount, bounty.getTotalDisplayBounty(), receiver);
        Bukkit.getConsoleSender().sendMessage(message);
        if (whitelist.getList().isEmpty()) {
            if (displayAmount >= minBroadcast)
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!NotBounties.disableBroadcast.contains(player.getUniqueId()) && !player.getUniqueId().equals(receiver.getUniqueId())) {
                        player.sendMessage(message);
                    }
                }
        } else {
            for (UUID uuid : whitelist.getList()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || player.getUniqueId().equals(receiver.getUniqueId()))
                    continue;
                player.sendMessage(message);
                player.sendMessage(parse(prefix + whitelistToggle, player));
            }
        }

    }




    public static void refundBounty(Bounty bounty) {
        for (Setter setter : bounty.getSetters()) {
            refundSetter(setter);
        }
    }

    public static void refundSetter(Setter setter) {
        refundPlayer(setter.getUuid(), setter.getAmount(), setter.getItems());
    }

    public static void refundPlayer(UUID uuid, double amount, List<ItemStack> items) {
        items = new ArrayList<>(items); // make the arraylist modifiable
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (amount > 0) {
            if (vaultEnabled && !overrideVault) {
                if (!NumberFormatting.getVaultClass().deposit(player, amount)) {
                    Bukkit.getLogger().warning("[NotBounties] Error depositing currency with vault!");
                    addRefund(player.getUniqueId(), amount);
                }
            } else {
                if (player.isOnline() && manualEconomy != ManualEconomy.PARTIAL && ((Plugin) NotBounties.getInstance()).isEnabled()) {
                    NumberFormatting.doAddCommands(player.getPlayer(), amount);
                } else {
                    addRefund(uuid, amount);
                }
            }
        }
        items.removeIf(Objects::isNull);
        if (!items.isEmpty() && manualEconomy == ManualEconomy.AUTOMATIC) {
                if (player.isOnline() && ((Plugin) NotBounties.getInstance()).isEnabled()) {
                    NumberFormatting.givePlayer(player.getPlayer(), items, false);
                } else {
                    addRefund(uuid, items);
                }
            }


    }

    private static void addRefund(UUID uuid, double amount) {
        if (refundedBounties.containsKey(uuid)){
            refundedBounties.replace(uuid, refundedBounties.get(uuid) + amount);
        } else {
            refundedBounties.put(uuid, amount);
        }
    }

    private static void addRefund(UUID uuid, List<ItemStack> items) {
        if (items.isEmpty())
            return;
        if (refundedItems.containsKey(uuid)){
            refundedItems.get(uuid).addAll(items);
        } else {
            refundedItems.put(uuid, items);
        }
    }

    public static List<Bounty> getPublicBounties(int sortType) {
        List<Bounty> bounties = DataManager.getAllBounties(sortType);
        bounties.removeIf(bounty -> hiddenNames.contains(bounty.getName()));
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
        for (Player player : Bukkit.getOnlinePlayers())
            BountyTracker.removeTracker(player);
        NotBounties.removeWantedTag(uuid);
        displayParticle.remove(uuid);
        DataManager.deleteBounty(uuid);
    }

    public static void removeSetters(UUID bountyUUID, List<Setter> setters) {
        Bounty bounty = getBounty(bountyUUID);
        if (bounty != null) {
            double change = setters.stream().mapToDouble(Setter::getDisplayAmount).sum();
            bigBountyCheck(bounty, change);
            DataManager.removeSetters(bounty, setters);
        }
    }

    public static boolean editBounty(@NotNull Bounty bounty, @Nullable UUID setterUUID, double change) {
        // remove particle if bounty reduced under threshold
        bigBountyCheck(bounty, change);
        return DataManager.editBounty(bounty, setterUUID, change) != null;
    }

    /**
     * Checks if the bounty is still a big bounty after the change.
     * If it is no longer a big bounty, the player will be removed from the displayParticle list.
     * @param bounty Bounty to be changed.
     * @param change Change in the bounty amount.
     */
    private static void bigBountyCheck(@NotNull Bounty bounty, double change) {
        if (displayParticle.contains(bounty.getUUID()) && bounty.getTotalDisplayBounty() + change < BigBounty.getThreshold())
            displayParticle.remove(bounty.getUUID());
    }


    public static void claimBounty(@NotNull Player player, Player killer, List<ItemStack> drops, boolean forceEditDrops) {
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Received a bounty claim request.");
        Item droppedHead = null;
        if (rewardHeadAnyKill)
            droppedHead = player.getWorld().dropItemNaturally(player.getLocation(), Head.createPlayerSkull(player.getUniqueId(), SkinManager.getSkin(player.getUniqueId()).getUrl()));
        // possible remove this later when the other functions allow null killers
        if (killer == null)
            return;
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] " + killer.getName() + " killed " + player.getName());
        TimedBounties.onDeath(player);
        // check if a bounty can be claimed
        if (!BountyClaimRequirements.canClaim(player, killer)) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] An external plugin, world filter, or a shared team is preventing this bounty from being claimed.");
            return;
        }
        MurderBounties.killPlayer(player, killer);

        if (player == killer) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Player killed themself.");
            return;
        }

        // check if killer can steal a bounty
        if (hasBounty(killer.getUniqueId()) && stealBounties) {
            Bounty bounty = getBounty(killer.getUniqueId());
            assert bounty != null;
            Bounty stolenBounty = bounty.getBounty(player.getUniqueId());
            //bounty.removeBounty(player.getUniqueId());
            // update the bounty
            removeSetters(stolenBounty.getUUID(), stolenBounty.getSetters());
            if (!stolenBounty.getSetters().isEmpty()) {
                // bounty has been stolen
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Killer stole a bounty!");
                if (manualEconomy == ManualEconomy.AUTOMATIC) {
                    // give rewards
                    if (debug)
                        Bukkit.getLogger().info("[NotBountiesDebug] Giving stolen bounty.");
                    NumberFormatting.doAddCommands(killer, stolenBounty.getTotalBounty());
                    NumberFormatting.givePlayer(killer, bounty.getTotalItemBounty(), false);
                }
                // send messages
                killer.sendMessage(parse(prefix + LanguageOptions.stolenBounty, stolenBounty.getTotalDisplayBounty(), player));
                // send messages
                String message = parse(prefix + stolenBountyBroadcast, player, stolenBounty.getTotalDisplayBounty(), bounty.getTotalDisplayBounty(), killer);
                Bukkit.getConsoleSender().sendMessage(message);
                if (stolenBounty.getTotalDisplayBounty() >= minBroadcast)
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!NotBounties.disableBroadcast.contains(p.getUniqueId()) && !p.getUniqueId().equals(killer.getUniqueId())) {
                            p.sendMessage(message);
                        }
                    }
                // play sound
                killer.getWorld().playSound(player.getLocation(), Sound.ENTITY_CAT_HISS, 1, 1);
            }
        }
        Bounty bounty = DataManager.getGuarrenteedBounty(player.getUniqueId());
        if (bounty == null) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Player doesn't have a bounty.");
            return;
        }

        // check if it is a npc
        if (!npcClaim && killer.hasMetadata("NPC")) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] This is an NPC which bounty claiming is disabled for in the config.");
            return;
        }

        // check if killer can claim it
        if (bounty.getTotalDisplayBounty(killer) < 0.01 && bounty.getTotalItemBounty(killer).isEmpty()) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] This bounty is too small, and there are no items attached!");
            return;
        }
        if (debug)
            Bukkit.getLogger().info(() -> "[NotBountiesDebug] Bounty to be claimed: " + bounty.getTotalDisplayBounty(killer));
        BountyClaimEvent event1 = new BountyClaimEvent(killer, new Bounty(bounty));
        Bukkit.getPluginManager().callEvent(event1);
        if (event1.isCancelled()) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] The bounty event got canceled by an external plugin.");
            return;
        }
        PVPRestrictions.onBountyClaim(player);
        Bounty bountyCopy = new Bounty(bounty, killer.getUniqueId());

        List<Setter> claimedBounties = new ArrayList<>(bounty.getSetters());
        claimedBounties.removeIf(setter -> !setter.canClaim(killer));

        displayParticle.remove(player.getUniqueId());

        // broadcast message
        String message = parse(prefix + claimBountyBroadcast, killer, bounty.getTotalDisplayBounty(killer), player);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if ((!NotBounties.disableBroadcast.contains(p.getUniqueId()) && bounty.getTotalDisplayBounty(killer) >= minBroadcast) || p.getUniqueId().equals(player.getUniqueId()) || p.getUniqueId().equals(Objects.requireNonNull(killer).getUniqueId())) {
                p.sendMessage(message);
            }
        }
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Claim messages sent to all players.");

        // reward head
        RewardHead rewardHead = new RewardHead(player.getUniqueId(), bounty.getTotalDisplayBounty(killer));

        if (rewardHeadSetter) {
            for (Setter setter : claimedBounties) {
                if (!setter.getUuid().equals(new UUID(0, 0))) {
                    Player p = Bukkit.getPlayer(setter.getUuid());
                    if (p != null) {
                        if (!rewardHeadClaimed || !Objects.requireNonNull(killer).getUniqueId().equals(setter.getUuid())) {
                            NumberFormatting.givePlayer(p, rewardHead.getItem(), 1);
                            if (debug)
                                Bukkit.getLogger().info("[NotBountiesDebug] Gave "  + p.getName() + " a player skull for the bounty.");
                        }
                    } else {
                        if (headRewards.containsKey(setter.getUuid())) {
                            // I think this could be replaced with headRewards.get(setter.getUuid()).add(rewardHead)
                            List<RewardHead> heads = new ArrayList<>(headRewards.get(setter.getUuid()));
                            heads.add(rewardHead);
                            headRewards.replace(setter.getUuid(), heads);
                        } else {
                            headRewards.put(setter.getUuid(), Collections.singletonList(rewardHead));
                        }
                        if (debug)
                            Bukkit.getLogger().info("[NotBountiesDebug] Will give " + NotBounties.getPlayerName(setter.getUuid()) + " a player skull when they log on next for the bounty.");
                    }
                }
            }
        }
        if (rewardHeadClaimed) {
            if (droppedHead != null)
                droppedHead.remove();
            NumberFormatting.givePlayer(killer, rewardHead.getItem(), 1);
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Gave " + killer.getName() + " a player skull for the bounty.");
        }
        // death tax
        if (ConfigOptions.deathTax > 0 && NumberFormatting.manualEconomy != NumberFormatting.ManualEconomy.PARTIAL) {
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Removing " + bounty.getTotalDisplayBounty(killer) * ConfigOptions.deathTax + " currency for death tax");
            Map<Material, Long> removedItems = NumberFormatting.doRemoveCommands(player, bounty.getTotalDisplayBounty(killer) * ConfigOptions.deathTax, drops);
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
                    if (debug)
                        Bukkit.getLogger().info("[NotBountiesDebug] Removing " + totalLoss + " currency for the death tax.");
                    player.sendMessage(parse(prefix + LanguageOptions.deathTax.replace("{items}", (builder.toString())), player));
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
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] Redeeming Reward: ");
        Bounty rewardedBounty = TrickleBounties.getRewardedBounty(bounty, killer);
        if (!redeemRewardLater) {
            if (NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL) {
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Directly giving auto-bounty reward.");
                NumberFormatting.doAddCommands(killer, rewardedBounty.getBounty(new UUID(0,0)).getTotalBounty(killer));
            } else {
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Directly giving total claimed bounty.");
                NumberFormatting.doAddCommands(killer, rewardedBounty.getTotalBounty(killer));
                if (manualEconomy == ManualEconomy.AUTOMATIC)
                    NumberFormatting.givePlayer(killer, bounty.getTotalItemBounty(killer), false); // give bountied items
            }
        } else {
            // will add these to a voucher later
            if (manualEconomy == ManualEconomy.AUTOMATIC)
                NumberFormatting.givePlayer(killer, bounty.getTotalItemBounty(killer), false); // give bountied items
            // give voucher
            if (NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL) {
                // auto bounty reward
                NumberFormatting.doAddCommands(killer, rewardedBounty.getBounty(new UUID(0,0)).getTotalBounty(killer));
            }
            if (RRLVoucherPerSetter) {
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Handing out vouchers. ");
                // multiple vouchers
                for (Setter setter : rewardedBounty.getSetters()) {
                    if (!setter.canClaim(killer))
                        continue;
                    if (setter.getAmount() <= 0.01)
                        continue;
                    if (setter.getUuid().equals(new UUID(0,0)) && NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL)
                        continue;
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    assert meta != null;
                    ArrayList<String> lore = new ArrayList<>();
                    for (String str : voucherLore) {
                        lore.add(parse(str.replace("{bounty}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(rewardedBounty.getTotalBounty(killer)) + NumberFormatting.currencySuffix)), Bukkit.getOfflinePlayer(setter.getUuid()),setter.getAmount(), player));
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    meta.setDisplayName(parse(bountyVoucherName.replace("{bounty}", (NumberFormatting.currencyPrefix + NumberFormatting.formatNumber(rewardedBounty.getTotalBounty(killer)) + NumberFormatting.currencySuffix)), killer, setter.getAmount(), player));
                    ArrayList<String> setterLore = new ArrayList<>(lore);
                    if (!RRLSetterLoreAddition.isEmpty()) {
                        setterLore.add(parse(RRLSetterLoreAddition, setter.getAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    }
                    setterLore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + setter.getAmount());
                    meta.setLore(setterLore);
                    item.setItemMeta(meta);
                    item.addUnsafeEnchantment(Enchantment.CHANNELING, 0);
                    NumberFormatting.givePlayer(killer, item, 1);
                }
            } else {
                if (debug)
                    Bukkit.getLogger().info("[NotBountiesDebug] Handing out a voucher. ");
                // one voucher
                ItemStack item = new ItemStack(Material.PAPER);
                ItemMeta meta = item.getItemMeta();
                assert meta != null;
                ArrayList<String> lore = new ArrayList<>();
                for (String str : voucherLore) {
                    lore.add(parse(str, player.getKiller(), rewardedBounty.getTotalBounty(killer), player));
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.setDisplayName(parse(bountyVoucherName, killer, rewardedBounty.getTotalBounty(killer), player));
                if (!RRLSetterLoreAddition.isEmpty()) {
                    for (Setter setter : rewardedBounty.getSetters()) {
                        if (!setter.canClaim(killer) || setter.getAmount() <= 0.01 || (setter.getUuid().equals(new UUID(0,0)) && NumberFormatting.manualEconomy == NumberFormatting.ManualEconomy.PARTIAL))
                            continue;
                        lore.add(parse(RRLSetterLoreAddition, setter.getAmount(), Bukkit.getOfflinePlayer(setter.getUuid())));
                    }
                }
                lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + bounty.getTotalBounty(killer));
                meta.setLore(lore);
                item.setItemMeta(meta);
                item.addUnsafeEnchantment(Enchantment.CHANNELING, 0);
                NumberFormatting.givePlayer(killer, item, 1);
            }
        }
        DataManager.changeStat(player.getUniqueId(), Leaderboard.ALL, bounty.getTotalDisplayBounty(killer), false);
        DataManager.changeStat(player.getUniqueId(), Leaderboard.DEATHS, 1, false);
        DataManager.changeStat(killer.getUniqueId(), Leaderboard.KILLS, 1, false);
        DataManager.changeStat(killer.getUniqueId(), Leaderboard.CLAIMED, bounty.getTotalDisplayBounty(killer), false);
        if (debug)
            Bukkit.getLogger().info("[NotBountiesDebug] stats. ");
        TrickleBounties.transferBounty(bounty, killer);
        //bounty.claimBounty(killer);
        removeSetters(bounty.getUUID(), claimedBounties);


        if (bounty.getTotalDisplayBounty() < minWanted) {
            // remove bounty tag
            NotBounties.removeWantedTag(bounty.getUUID());
            if (debug)
                Bukkit.getLogger().info("[NotBountiesDebug] Removed wanted tag. ");
        }

        for (Setter setter : claimedBounties) {
            if (!setter.getUuid().equals(new UUID(0, 0))) {
                Player p = Bukkit.getPlayer(setter.getUuid());
                if (p != null) {
                    p.playSound(p.getEyeLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 1, 1);
                }
            }
        }
        Immunity.startGracePeriod(player);
        BountyTracker.stopTracking(player.getUniqueId());
        for (Player p : Bukkit.getOnlinePlayers()) {
            BountyTracker.removeTracker(p);
        }
        GUI.reopenBountiesGUI();
        ActionCommands.executeBountyClaim(player, killer, bountyCopy);
    }

}
