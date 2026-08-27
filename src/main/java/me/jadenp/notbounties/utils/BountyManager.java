package me.jadenp.notbounties.utils;

import me.jadenp.notbounties.bounty_events.DropRewardHead;
import me.jadenp.notbounties.data.*;
import me.jadenp.notbounties.Leaderboard;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.bounty_events.BountyClaimEvent;
import me.jadenp.notbounties.bounty_events.BountySetEvent;
import me.jadenp.notbounties.data.player_data.*;
import me.jadenp.notbounties.features.settings.auto_bounties.BigBounty;
import me.jadenp.notbounties.features.settings.databases.BountySortType;
import me.jadenp.notbounties.features.settings.display.BountyHunt;
import me.jadenp.notbounties.features.settings.display.BountyTracker;
import me.jadenp.notbounties.features.settings.display.WantedTags;
import me.jadenp.notbounties.features.settings.immunity.ImmunityManager;
import me.jadenp.notbounties.features.settings.integrations.BountyClaimRequirements;
import me.jadenp.notbounties.features.settings.money.NotEnoughCurrencyException;
import me.jadenp.notbounties.features.settings.money.NumberFormatting;
import me.jadenp.notbounties.ui.Head;
import me.jadenp.notbounties.ui.SkinManager;
import me.jadenp.notbounties.ui.gui.GUI;
import me.jadenp.notbounties.features.settings.auto_bounties.TrickleBounties;
import me.jadenp.notbounties.utils.tasks.BroadcastTask;
import me.jadenp.notbounties.utils.tasks.DelayedReward;
import me.jadenp.notbounties.features.*;
import me.jadenp.notbounties.features.settings.auto_bounties.MurderBounties;
import me.jadenp.notbounties.features.settings.auto_bounties.TimedBounties;
import me.jadenp.notbounties.features.settings.integrations.external_api.LocalTime;
import me.jadenp.notbounties.features.settings.integrations.external_api.MMOLibClass;
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
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;


import static me.jadenp.notbounties.features.LanguageOptions.*;
import static me.jadenp.notbounties.features.settings.money.NumberFormatting.*;

public class BountyManager {

    private static final int BOUNTY_LIST_LENGTH = 10;
    private static final List<BountyClaimInstance> delayedBountyClaims = Collections.synchronizedList(new LinkedList<>());

    private BountyManager() {
    }

    public static void listBounties(CommandSender sender, int page) {

        int sortNum;
        Player parser;
        if (sender instanceof Player player) {
            sortNum = ImpersistentPlayerData.get(player.getUniqueId()).getGUISortType("bounty-gui");
            parser = player;
        } else {
            parser = null;
            sortNum = 0;
        }
        BountySortType sortType = BountySortType.values()[Math.clamp(sortNum, 0, BountySortType.values().length - 1)];

        DataManager.getPublicBountiesAsync(sortType, (long) page * BOUNTY_LIST_LENGTH, BOUNTY_LIST_LENGTH).thenAccept(sortedList -> {
            if (parser != null) {
                if (parser.isOnline()) {
                    NotBounties.getServerImplementation().entity(parser).run(() -> sendBountyList(parser, sortedList, page, parser));
                }
            } else {
                NotBounties.getServerImplementation().global().run(() -> sendBountyList(sender, sortedList, page, parser));
            }
        });

    }

    private static void sendBountyList(CommandSender sender, List<Bounty> sortedList, int page, Player parser) {
        String title = LanguageOptions.getMessage("bounty-list-title").replace("{page}", page + 1 + "");
        title = parse(title, parser);
        sender.sendMessage(title);
        for (int i = 0; i <= BOUNTY_LIST_LENGTH; i++) {
            if (sortedList.size() > i) {
                sender.sendMessage(parse(getMessage("list-total"), sortedList.get(i).getTotalDisplayBounty(), Bukkit.getOfflinePlayer(sortedList.get(i).getUUID())));
            } else {
                break;
            }
        }

        Tutorial.sendUnifiedPageLine(sender, page + 1, parser, page, page + 2, "list", (int) Math.ceil(((double) sortedList.size()) / BOUNTY_LIST_LENGTH) + 1);
    }

    public static CompletableFuture<Bounty> addBounty(OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        return addBounty(null, receiver, amount, items, whitelist);
    }


    public static CompletableFuture<Bounty> addBounty(@Nullable Player setter, OfflinePlayer receiver, double amount, List<ItemStack> items, Whitelist whitelist) {
        if (!Bukkit.isPrimaryThread()) {
            CompletableFuture<Bounty> result = new CompletableFuture<>();

            double finalAmount = amount;
            NotBounties.getServerImplementation().global().run(() -> {
                        addBounty(setter, receiver, finalAmount, items, whitelist)
                                .whenComplete((bounty, throwable) -> {
                                    if (throwable != null) {
                                        result.completeExceptionally(throwable);
                                    } else {
                                        result.complete(bounty);
                                    }
                                });
                    });

            return result;
        }

        double displayAmount = amount;

        // You can only set bounties by items or amount.
        // If there are items, the amount is just the value of the items.
        if (!items.isEmpty()) {
            amount = 0;
        }

        Bounty bounty;

        if (setter != null) {
            bounty = new Bounty(setter, receiver, amount, items, whitelist);
        } else {
            bounty = new Bounty(receiver, amount, items, whitelist);
        }

        BountySetEvent event = new BountySetEvent(bounty);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            Bounty refundBounty;

            if (setter != null) {
                refundBounty = new Bounty(setter, receiver, amount + amount * ConfigOptions.getMoney().getBountyTax() + Whitelist.getCost() * whitelist.getList().size(), items, whitelist);
            } else {
                refundBounty = new Bounty(receiver, amount + amount * ConfigOptions.getMoney().getBountyTax() + Whitelist.getCost() * whitelist.getList().size(), items, whitelist);
            }

            refundBounty(refundBounty, LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-bounty-cancel"), receiver));

            return CompletableFuture.completedFuture(null);
        }

        // Only do these if a player actually set the bounty.
        if (setter != null) {
            // Unlock recipe
            if (!setter.hasDiscoveredRecipe(BountyTracker.getBountyTrackerRecipe())) {
                setter.discoverRecipe(BountyTracker.getBountyTrackerRecipe());
            }

            // Add setter stat
            DataManager.changeStat(setter.getUniqueId(), Leaderboard.SET, 1);
        }

        // Add receiver stat
        DataManager.changeStat(receiver.getUniqueId(), Leaderboard.ALL, displayAmount);

        return DataManager.insertBountyAsync(setter, receiver, amount, items, whitelist).thenApply(insertedBounty -> {
            registerBounty(receiver, insertedBounty);

            BroadcastTask broadcastTask = new BroadcastTask(setter, receiver, displayAmount, insertedBounty.getTotalDisplayBounty(), whitelist);

            broadcastTask.setTaskImplementation(NotBounties.getServerImplementation().async().runAtFixedRate(broadcastTask, 1, 4));

            if (setter != null) {
                ActionCommands.executeBountySet(receiver.getUniqueId(), setter, insertedBounty);

                DataManager.setBountyCooldown(setter.getUniqueId());
            }

            return insertedBounty;
        });
    }

    /**
     * Registers the bounty with other features.
     *
     * @param receiver Player that the bounty was set on.
     * @param bounty   Bounty that was set.
     */
    private static void registerBounty(OfflinePlayer receiver, Bounty bounty) {
        if (ConfigOptions.getIntegrations().isMmoLibEnabled() && receiver.isOnline()) {
            MMOLibClass.removeStats(receiver.getPlayer());
            MMOLibClass.addStats(receiver.getPlayer(), bounty.getTotalDisplayBounty());
        }

        if (receiver.isOnline()) {
            Player onlineReceiver = receiver.getPlayer();
            assert onlineReceiver != null;
            // check for big bounty
            BigBounty.setBounty(onlineReceiver, bounty, bounty.getTotalDisplayBounty());
            // add wanted tag
            if (WantedTags.isEnabled() && bounty.getTotalDisplayBounty() >= WantedTags.getMinWanted()) {
                WantedTags.addWantedTag(onlineReceiver);
            }
        }
    }


    public static void refundBounty(Bounty bounty, String reason) {
        for (Setter setter : bounty.getSetters()) {
            refundSetter(setter, reason);
        }
    }

    public static void refundSetter(Setter setter, String reason) {
        setter.getItems().thenAccept(items -> NotBounties.getServerImplementation().global().run(() ->
                refundPlayer(setter.getUuid(), setter.getAmount(), items, reason)));
    }

    public static void refundPlayer(UUID uuid, double amount, List<ItemStack> items, String reason) {
        if (uuid.equals(DataManager.GLOBAL_SERVER_ID))
            return;
        items = new ArrayList<>(items); // make the arraylist modifiable
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        // refund amount
        if (amount > 0) {
            if (NumberFormatting.isVaultEnabled() && !NumberFormatting.isOverrideVault()) {
                if (!NumberFormatting.getVaultClass().deposit(player, amount)) {
                    NotBounties.getInstance().getLogger().warning("Error depositing currency with vault for " + LoggedPlayers.getPlayerName(uuid) + "! Will retry when player joins next.");
                    DataManager.addRefund(uuid, new AmountRefund(amount, reason));
                }
            } else {
                if (player.isOnline() && NotBounties.getInstance().isEnabled()) {
                    if (NumberFormatting.getManualEconomy() != ManualEconomy.PARTIAL)
                        NumberFormatting.doAddCommands(Objects.requireNonNull(player.getPlayer()), amount);
                } else {
                    DataManager.addRefund(uuid, new AmountRefund(amount, reason));
                }
            }
        }
        // refund items
        items.removeIf(Objects::isNull);
        if (!items.isEmpty() && NumberFormatting.getManualEconomy() == ManualEconomy.AUTOMATIC) {
            if (player.isOnline() && NotBounties.getInstance().isEnabled()) {
                NumberFormatting.givePlayer(player.getPlayer(), items, false);
            } else {
                DataManager.addRefund(uuid, new ItemRefund(items, reason));
            }
        }
    }

    @Deprecated(since = "1.23.0")
    public static List<Bounty> getPublicBounties(int sortType) {
        return DataManager.getPublicBountiesAsync(BountySortType.values()[sortType], 0, 999).join();
    }

    @Deprecated(since = "1.23.0")
    public static List<Bounty> getAllBounties(int sortType) {
        return DataManager.getAllBounties(sortType);
    }

    @Deprecated(since = "1.23.0")
    public static Bounty getBounty(UUID uuid) {
        return DataManager.getBounty(uuid);
    }

    @Deprecated(since = "1.23.0")
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
        BountyHunt.endHunt(uuid);
    }


    public static boolean editBounty(@NotNull Bounty bounty, @Nullable UUID setterUUID, double change) {
        // remove particle if bounty reduced under threshold
        BigBounty.bigBountyCheck(bounty, change);
        DataManager.editBounty(bounty, setterUUID, change);
        return true;

    }

    public static void checkDelayedBountyClaim() {
        if (!delayedBountyClaims.isEmpty()) {
            List<BountyClaimInstance> delayedBountyClaimsCopy = new LinkedList<>(delayedBountyClaims);
            delayedBountyClaims.clear();
            for (int i = 0; i < delayedBountyClaimsCopy.size(); i++) {
                BountyClaimInstance bountyClaimInstance = delayedBountyClaimsCopy.get(i);
                NotBounties.getServerImplementation().global().runDelayed(bountyClaimInstance::claimBounty, 1 + i * 10L);
            }
        }
    }

    /**
     * Checks if the killer has all the required permissions and integrations to claim a bounty on the player.
     *
     * @param player Player that was killed. Does not need to have a bounty.
     * @param killer Player that committed the murder.
     * @return True if a claim is allowed.
     */
    private static boolean canClaimPreCheck(@NotNull Player player, @NotNull Player killer) {
        if (!BountyClaimRequirements.canClaim(player, killer)) {
            NotBounties.debugMessage("An external plugin, world filter, or a shared team is preventing this bounty from being claimed.", false);
            return false;
        }
        if (player == killer) {
            NotBounties.debugMessage("Player killed themself. D:", false);
            return false;
        }
        if (!killer.hasPermission("notbounties.claim")) {
            NotBounties.debugMessage("Player doesn't have the notbounties.claim permission.", false);
            return false;
        }
        // check if it is a npc
        if (!ConfigOptions.isNpcClaim() && BountyManager.isNPC(killer)) {
            NotBounties.debugMessage("This is an NPC, which bounty claiming is disabled for in the config.", false);
            return false;
        }
        return true;
    }

    /**
     * Try to steal a bounty from the player.
     * Stealing a bounty is the killer murdering someone that placed a bounty on them and taking those rewards.
     *
     * @param player Player who was killed.
     * @param killer Player who committed the murder.
     * @apiNote Async-safe.
     */
    private static void tryStealBounty(@NotNull Player player, @NotNull Player killer, @Nullable Bounty killerBounty) {
        if (ConfigOptions.isStealBounties() && killerBounty != null) {
            Bounty stolenBounty = killerBounty.getBounty(player.getUniqueId());
            // update the bounty
            DataManager.removeSetters(killerBounty, stolenBounty.getSetters());
            if (!stolenBounty.getSetters().isEmpty()) {
                // bounty has been stolen
                NotBounties.debugMessage("Killer stole a bounty!", false);
                if (NumberFormatting.getManualEconomy() == ManualEconomy.AUTOMATIC) {
                    // give rewards
                    NotBounties.debugMessage("Giving stolen bounty.", false);
                    NumberFormatting.doAddCommands(killer, stolenBounty.getTotalBounty());
                    killerBounty.getTotalItemBountyAsync().thenAccept(items -> NumberFormatting.givePlayer(killer, items, false));
                }
                // send messages
                killer.sendMessage(parse(getPrefix() + LanguageOptions.getMessage("stolen-bounty"), stolenBounty.getTotalDisplayBounty(), player));
                // send messages
                String message = parse(getPrefix() + getMessage("stolen-bounty-broadcast"), player, stolenBounty.getTotalDisplayBounty(), killerBounty.getTotalDisplayBounty(), killer);
                Bukkit.getConsoleSender().sendMessage(message);
                if (stolenBounty.getTotalDisplayBounty() >= ConfigOptions.getMoney().getMinBroadcast()) {
                    if (!Bukkit.isPrimaryThread()) {
                        NotBounties.getServerImplementation().global().run(() -> broadcastMessage(message, uuid -> uuid.equals(killer.getUniqueId())));
                    } else {
                        broadcastMessage(message, uuid -> uuid.equals(killer.getUniqueId()));
                    }
                }
                // play sound
                killer.getWorld().playSound(player.getLocation(), Sound.ENTITY_CAT_HISS, 1, 1);
            }
        }
    }

    /**
     * Broadcast a message to the server. Must be called on the main thread.
     *
     * @param message   Message to broadcast.
     * @param operation Players to exclude from the broadcast.
     */
    private static void broadcastMessage(String message, ExcludePlayersOperation operation) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!operation.isExcluded(p.getUniqueId())) {
                DataManager.getPlayerDataAsync(p.getUniqueId()).thenAccept(playerData -> {
                    if (playerData.getBroadcastSettings() != PlayerData.BroadcastSettings.DISABLE) {
                        NotBounties.getServerImplementation().entity(p).run(() -> p.sendMessage(message));
                    }
                });
            }
        }
    }

    /**
     * Called when a player dies, this function determines if a bounty can be claimed and hands out rewards if so.
     *
     * @param player         Player that was killed.
     * @param killer         Player that killed.
     * @param drops          Items that were dropped/
     * @param forceEditDrops Whether the player's inventory should be forcibly edited after the drops are modified.
     */
    public static void claimBounty(@NotNull Player player, Player killer, List<ItemStack> drops, boolean forceEditDrops, double deathTax) {
        NotBounties.debugMessage("Received a bounty claim request.", false);
        Item droppedHead;
        if (RewardHead.isRewardAnyKill()) {
            ItemStack head = Head.createPlayerSkull(player.getUniqueId(), SkinManager.getSkin(player.getUniqueId()).url());
            ItemMeta headMeta = head.getItemMeta();
            assert headMeta != null;
            headMeta.setDisplayName(LanguageOptions.parse(LanguageOptions.getMessage("any-kill-head-name"), player));
            List<String> lore = new ArrayList<>();
            LanguageOptions.getListMessage("any-kill-head-lore").forEach(str -> lore.add(LanguageOptions.parse(str, player)));
            headMeta.setLore(lore);
            head.setItemMeta(headMeta);
            droppedHead = player.getWorld().dropItemNaturally(player.getLocation(), head);
        } else {
            droppedHead = null;
        }
        // possible remove this later when the other functions allow null killers aka non-player deaths
        if (killer == null)
            return;
        NotBounties.debugMessage(killer.getName() + " killed " + player.getName(), false);

        // check if a bounty can be claimed from integrations
        NotBounties.getServerImplementation().global().run(() -> {
            if (canClaimPreCheck(player, killer)) { // some integrations are not async safe
                NotBounties.getServerImplementation().async().runNow(() -> claimBountyPostIntegrations(player, killer, drops, forceEditDrops, deathTax, droppedHead));
            }
        });


    }

    private static void claimBountyPostIntegrations(@NonNull Player player, Player killer, List<ItemStack> drops, boolean forceEditDrops, double deathTax, Item droppedHead) {
        TimedBounties.onDeath(player); // reset next bounty timer for being killed

        Bounty bounty = DataManager.getGuarrenteedBounty(player.getUniqueId());
        Bounty killerBounty = DataManager.getBountyAsync(killer.getUniqueId()).join();
        boolean cancelTrickle = MurderBounties.killPlayer(player, bounty, killer, killerBounty); // possibly add bounty on killer

        // check if killer can steal a bounty
        tryStealBounty(player, killer, killerBounty); // async-safe

        if (bounty == null) {
            NotBounties.debugMessage("Player doesn't have a bounty.", false);
            return;
        }

        // check if killer can claim it
        if (bounty.getTotalDisplayBounty(killer) < 0.01 && bounty.getTotalItemBounty(killer).isEmpty()) {
            NotBounties.debugMessage("This bounty is too small, and there are no items attached!", false);
            return;
        }
        NotBounties.debugMessage("Bounty to be claimed: " + bounty.getTotalDisplayBounty(killer), false);

        NotBounties.getServerImplementation().global().run(() -> {
            BountyClaimEvent bountyClaimEvent = new BountyClaimEvent(killer, new Bounty(bounty));
            Bukkit.getPluginManager().callEvent(bountyClaimEvent);
            if (bountyClaimEvent.isCancelled()) {
                NotBounties.debugMessage("The bounty event got canceled by an external plugin.", false);
                // could print stack trace if the specific external plugin is needed
            } else {
                // can now begin the bounty claim
                claimBountyForce(player, killer, drops, forceEditDrops, deathTax, droppedHead, bounty, killerBounty, bountyClaimEvent.getDropRewardHead(), cancelTrickle);
            }
        });
    }

    private static void claimBountyForce(@NonNull Player player, Player killer, List<ItemStack> drops, boolean forceEditDrops, double deathTax, Item droppedHead, Bounty bounty, Bounty killerBounty, DropRewardHead dropRewardHead, boolean cancelTrickle) {
        PVPRestrictions.onBountyClaim(player); // make combat safe if enabled
        final Bounty claimedBounty = new Bounty(bounty, killer.getUniqueId()); // create a copy of the claimed part of the bounty (for reference later)
        // get a copy of all the setters that are to be claimed
        List<Setter> claimedBounties = new ArrayList<>(claimedBounty.getSetters());
        //claimedBounties.removeIf(setter -> !setter.canClaim(killer)); // this shouldn't do anything

        // broadcast message
        String message = parse(getPrefix() + getMessage("claim-bounty-broadcast"), killer, bounty.getTotalDisplayBounty(killer), player);
        Bukkit.getConsoleSender().sendMessage(message);
        boolean aboveMinBounty = bounty.getTotalDisplayBounty(killer) >= ConfigOptions.getMoney().getMinBroadcast();
        broadcastMessage(message, uuid -> !aboveMinBounty && !uuid.equals(killer.getUniqueId()) && !uuid.equals(player.getUniqueId()));
        NotBounties.debugMessage("Claim messages sent to all players.", false);

        // hand out reward heads
        ItemRefund rewardHead = new ItemRefund(Collections.singletonList(RewardHead.getItem(player.getUniqueId(), killer.getUniqueId(), bounty.getTotalDisplayBounty(killer))), LanguageOptions.parse(LanguageOptions.getMessage("refund-reason-reward-head"), player));
        if (bounty.getTotalDisplayBounty(killer) >= RewardHead.getMinimumBounty()) {
            if (dropRewardHead.isDropSettersHead()) {
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
                            DataManager.addRefund(setter.getUuid(), rewardHead);
                            NotBounties.debugMessage("Will give " + setter.getName() + " a player skull when they log on next for the bounty.", false);
                        }
                    }
                }
            }
            if (dropRewardHead.isDropKillerHead()) {
                // reward head for killer
                if (droppedHead != null)
                    // if a head was dropped for killing a player (any-kill), remove it to be replaced with a custom head
                    droppedHead.remove();

                rewardHead.giveRefund(killer);
                NotBounties.debugMessage("Gave killer " + killer.getName() + " a player skull for the bounty.", false);
            }
        }

        // death tax
        if (deathTax > 0 && NumberFormatting.getManualEconomy() != ManualEconomy.PARTIAL) {
            NotBounties.debugMessage("Removing " + bounty.getTotalDisplayBounty(killer) * deathTax + " currency for death tax", false);
            // attempt to remove currency/items from the player's inventory
            // I think these are the items that should have been removed from the player, but might not have been since they died.
            Map<Material, Long> removedItems = new EnumMap<>(Material.class);
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
                            NumberFormatting.removeItem(player, entry.getKey(), entry.getValue(), "-1");
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
        Bounty rewardedBounty = TrickleBounties.getRewardedBounty(claimedBounty, killer, killerBounty != null);
        NotBounties.debugMessage("Redeeming Reward: of " + rewardedBounty.getTotalDisplayBounty(), false);
        NotBounties.debugMessage(rewardedBounty.toString(), false);
        if (!ConfigOptions.getMoney().getRedeemRewardLater().isVouchers()) {
            // give currency
            // check if the player is in a duel or reward later is set in the config
            if (ConfigOptions.getIntegrations().isDuelsEnabled() && ConfigOptions.getIntegrations().getDuels().isDelayReward() && ConfigOptions.getIntegrations().getDuels().isInDuel(killer)) {
                // delayed reward because killer is in a duel
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
            if (NumberFormatting.getManualEconomy() == ManualEconomy.PARTIAL) {
                // auto bounty reward
                NumberFormatting.doAddCommands(killer, rewardedBounty.getBounty(DataManager.GLOBAL_SERVER_ID).getTotalBounty(killer));
            }
            if (ConfigOptions.getMoney().getRedeemRewardLater().isVoucherPerSetter()) {
                NotBounties.debugMessage("Handing out vouchers.", false);
                // multiple vouchers
                for (Setter setter : rewardedBounty.getSetters()) {
                    if (!setter.canClaim(killer)
                            || setter.getAmount() <= 0.01
                            || (setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID) && NumberFormatting.getManualEconomy() == ManualEconomy.PARTIAL))
                        continue;
                    ItemStack item = new ItemStack(Material.PAPER);
                    ItemMeta meta = item.getItemMeta();
                    assert meta != null;
                    ArrayList<String> lore = new ArrayList<>();
                    for (String str : getListMessage("bounty-voucher-lore")) {
                        lore.add(parse(str.replace("{bounty}", (NumberFormatting.getCurrencyPrefix() + NumberFormatting.formatNumber(rewardedBounty.getTotalBounty(killer)) + NumberFormatting.getCurrencySuffix())), Bukkit.getOfflinePlayer(setter.getUuid()), setter.getAmount(), player));
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
                    lore.add(parse(str, killer, rewardedBounty.getTotalBounty(killer), player));
                }
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                meta.setDisplayName(parse(getMessage("bounty-voucher-name"), killer, rewardedBounty.getTotalBounty(killer), player));
                if (!ConfigOptions.getMoney().getRedeemRewardLater().getSetterLoreAddition().isEmpty()) {
                    for (Setter setter : rewardedBounty.getSetters()) {
                        if (!setter.canClaim(killer) || setter.getAmount() <= 0.01 || (setter.getUuid().equals(DataManager.GLOBAL_SERVER_ID) && NumberFormatting.getManualEconomy() == ManualEconomy.PARTIAL))
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

        BountyHunt.claimBounty(player, killer);

        DataManager.changeStat(player.getUniqueId(), Leaderboard.DEATHS, 1);
        DataManager.changeStats(killer.getUniqueId(), new PlayerStat(1, 0, 0, 0, 0, bounty.getTotalDisplayBounty(killer)));
        NotBounties.debugMessage("Given stats.", false);
        List<Setter> removedSetters = new LinkedList<>(rewardedBounty.getSetters());
        if (!cancelTrickle) {
            Bounty transferedBounty = TrickleBounties.transferBounty(bounty, killer, killerBounty != null);
            removedSetters.addAll(transferedBounty.getSetters());
        }

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
     *
     * @param player  Player to give the reward to.
     * @param bounty  Bounty to be rewarded.
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
     *
     * @param uuid   UUID of the player to be rewarded.
     * @param bounty Bounty to reward the player.
     */
    public static void rewardBounty(UUID uuid, Bounty bounty) {
        if (NumberFormatting.getManualEconomy() == NumberFormatting.ManualEconomy.PARTIAL) {
            // partial economy means only auto bounties are given by the console
            NotBounties.debugMessage("(Partial Economy) Directly giving auto-bounty reward.", false);
            refundPlayer(uuid, bounty.getBounty(DataManager.GLOBAL_SERVER_ID).getTotalBounty(uuid), Collections.emptyList(), null);
        } else {
            NotBounties.debugMessage("Directly giving total claimed bounty.", false);
            double rewardAmount = bounty.getTotalBounty(uuid);
            if (getManualEconomy() == ManualEconomy.AUTOMATIC) {
                bounty.getTotalItemBountyAsync(uuid).thenAccept(items -> refundPlayer(uuid, rewardAmount, items, null));
            }
        }
    }

    public static boolean isNPC(Player player) {
        return player.hasMetadata("NPC") || player.getScoreboardTags().contains("CITIZENS_NPC");
    }

    @FunctionalInterface
    private interface ExcludePlayersOperation {
        boolean isExcluded(UUID uuid);
    }

}
