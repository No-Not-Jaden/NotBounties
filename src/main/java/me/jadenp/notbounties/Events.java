package me.jadenp.notbounties;

import me.jadenp.notbounties.API.BountyEvents.BountyClaimEvent;
import me.jadenp.notbounties.map.BountyBoard;
import me.jadenp.notbounties.utils.*;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.regex.Matcher;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.API.BountyManager.*;
import static me.jadenp.notbounties.utils.ConfigOptions.*;

public class Events implements Listener {

    private static Map<UUID, Map<UUID, Long>> playerKills = new HashMap<>();

    public Events() {
    }


    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (immunePerms.contains(event.getPlayer().getUniqueId().toString())) {
            if (!event.getPlayer().hasPermission("notbounties.immune")) {
                immunePerms.remove(event.getPlayer().getUniqueId().toString());
            }
        } else {
            if (event.getPlayer().hasPermission("notbounties.immune")) {
                immunePerms.add(event.getPlayer().getUniqueId().toString());
            }
        }
        displayParticle.remove(event.getPlayer());
        if (NotBounties.wantedText.containsKey(event.getPlayer().getUniqueId())) {
            NotBounties.wantedText.get(event.getPlayer().getUniqueId()).removeStand();
            NotBounties.wantedText.remove(event.getPlayer().getUniqueId());
        }

    }

    public static void cleanPlayerKills() {
        Map<UUID, Map<UUID, Long>> updatedMap = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, Long>> entry : playerKills.entrySet()) {
            Map<UUID, Long> deaths = entry.getValue();
            deaths.entrySet().removeIf(entry1 -> entry1.getValue() < System.currentTimeMillis() - murderCooldown * 1000L);
            updatedMap.put(entry.getKey(), deaths);
        }
        updatedMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        playerKills = updatedMap;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        if (event.getEntity().getKiller() == null)
            return;
        // check world filter
        if (worldFilter && !worldFilterNames.contains(event.getEntity().getWorld().getName()))
            return;
        if (!worldFilter && worldFilterNames.contains(event.getEntity().getWorld().getName()))
            return;
        Player player = (Player) event.getEntity();
        Player killer = event.getEntity().getKiller();
        assert killer != null;
        // check for teams
        if (betterTeamsEnabled) {
            BetterTeamsClass betterTeamsClass = new BetterTeamsClass();
            if ((!btClaim && betterTeamsClass.onSameTeam(player, killer)) || (!btAllies && betterTeamsClass.areAllies(player, killer)))
                return;
        }
        if (!scoreboardTeamClaim) {
            if (Bukkit.getScoreboardManager() != null) {
                Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
                for (Team team : sb.getTeams()) {
                    if (team.hasEntry(player.getDisplayName()) && team.hasEntry(killer.getDisplayName()))
                        return;
                }
            }
        }
        if (papiEnabled && !teamsPlaceholder.isEmpty()) {
            PlaceholderAPIClass placeholderAPIClass = new PlaceholderAPIClass();
            if (placeholderAPIClass.parse(player, teamsPlaceholder).equals(placeholderAPIClass.parse(killer, teamsPlaceholder)))
                return;
        }
        // check if we should increase the killer's bounty
        if (murderBountyIncrease > 0) {
            if (!killer.hasMetadata("NPC")) { // don't raise bounty on a npc
                if (!playerKills.containsKey(killer.getUniqueId()) ||
                        !playerKills.get(killer.getUniqueId()).containsKey(player.getUniqueId()) ||
                        playerKills.get(killer.getUniqueId()).get(player.getUniqueId()) < System.currentTimeMillis() - murderCooldown * 1000L) {
                    if (!murderExcludeClaiming || !hasBounty(player) || Objects.requireNonNull(getBounty(player)).getTotalBounty(killer) < 0.01) {
                        // increase
                        addBounty(killer, murderBountyIncrease, new Whitelist(new ArrayList<>(), false));
                        killer.sendMessage(parse(speakings.get(0) + speakings.get(59), player.getName(), Objects.requireNonNull(getBounty(killer)).getTotalBounty(), killer));
                        Map<UUID, Long> kills = playerKills.containsKey(killer.getUniqueId()) ? playerKills.get(killer.getUniqueId()) : new HashMap<>();
                        kills.put(player.getUniqueId(), System.currentTimeMillis());
                        playerKills.put(killer.getUniqueId(), kills);
                    }
                }
            }
        }
        if (!hasBounty(player) || player == event.getEntity().getKiller())
            return;

        // check if it is a npc
        if (!npcClaim && killer.hasMetadata("NPC"))
            return;
        Bounty bounty = getBounty(player);
        assert bounty != null;
        // check if killer can claim it
        if (bounty.getTotalBounty(killer) < 0.01)
            return;
        BountyClaimEvent event1 = new BountyClaimEvent(event.getEntity().getKiller(), bounty);
        Bukkit.getPluginManager().callEvent(event1);
        if (event1.isCancelled())
            return;

        List<Setter> claimedBounties = new ArrayList<>(bounty.getSetters());
        claimedBounties.removeIf(setter -> !setter.canClaim(killer));

        displayParticle.remove(player);

        // broadcast message
        String message = parse(speakings.get(0) + speakings.get(7), player.getName(), killer.getName(), bounty.getTotalBounty(killer), player);
        Bukkit.getConsoleSender().sendMessage(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if ((!disableBroadcast.contains(p.getUniqueId().toString()) && bounty.getTotalBounty(killer) >= minBroadcast) || p.getUniqueId().equals(event.getEntity().getUniqueId()) || p.getUniqueId().equals(Objects.requireNonNull(event.getEntity().getKiller()).getUniqueId())) {
                p.sendMessage(message);
            }
        }

        // reward head
        RewardHead rewardHead = new RewardHead(player.getName(), player.getUniqueId(), bounty.getTotalBounty(killer));

        if (rewardHeadSetter) {
            for (Setter setter : claimedBounties) {
                if (!setter.getUuid().equals(new UUID(0, 0))) {
                    Player p = Bukkit.getPlayer(setter.getUuid());
                    if (p != null) {
                        if (!rewardHeadClaimed || !Objects.requireNonNull(event.getEntity().getKiller()).getUniqueId().equals(setter.getUuid())) {
                            NumberFormatting.givePlayer(p, rewardHead.getItem(), 1);
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
                    }
                }
            }
        }
        if (rewardHeadClaimed) {
            NumberFormatting.givePlayer(killer, rewardHead.getItem(), 1);
        }
        // do commands
        if (deathTax > 0) {
            Map<Material, Long> removedItems = NumberFormatting.doRemoveCommands(player, bounty.getTotalBounty(killer) * deathTax, event.getDrops());
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
                    player.sendMessage(parse(speakings.get(0) + speakings.get(64).replaceAll("\\{items}", Matcher.quoteReplacement(builder.toString())), player));
                    // modify drops
                    ListIterator<ItemStack> dropsIterator = event.getDrops().listIterator();
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
        if (!redeemRewardLater) {
            NumberFormatting.doAddCommands(event.getEntity().getKiller(), bounty.getTotalBounty(killer));
        } else {
            // give voucher
            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(parse(speakings.get(40), event.getEntity().getName(), Objects.requireNonNull(event.getEntity().getKiller()).getName(), bounty.getTotalBounty(killer), (Player) event.getEntity()));
            ArrayList<String> lore = new ArrayList<>();
            for (String str : voucherLore) {
                lore.add(parse(str, event.getEntity().getName(), Objects.requireNonNull(event.getEntity().getKiller()).getName(), bounty.getTotalBounty(killer), (Player) event.getEntity()));
            }
            lore.add(ChatColor.BLACK + "" + ChatColor.STRIKETHROUGH + ChatColor.UNDERLINE + ChatColor.ITALIC + "@" + bounty.getTotalBounty(killer));
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 0);
            NumberFormatting.givePlayer(event.getEntity().getKiller(), item, 1);
        }
        if (SQL.isConnected()) {
            data.addData(player.getUniqueId().toString(), 0, 0, 1, bounty.getTotalBounty(killer), 0, 0);
            data.addData(killer.getUniqueId().toString(), 1, 0, 0, 0, 0, bounty.getTotalBounty(killer));
        } else {
            allTimeBounties.put(player.getUniqueId().toString(), Leaderboard.ALL.getStat(player.getUniqueId()) + bounty.getTotalBounty(killer));
            killBounties.put(killer.getUniqueId().toString(), Leaderboard.KILLS.getStat(killer.getUniqueId()) + 1);
            deathBounties.put(player.getUniqueId().toString(), Leaderboard.DEATHS.getStat(player.getUniqueId()) + 1);
            allClaimedBounties.put(killer.getUniqueId().toString(), Leaderboard.CLAIMED.getStat(killer.getUniqueId()) + bounty.getTotalBounty(killer));
        }
        bounty.claimBounty(killer);
        updateBounty(bounty);

        if (bounty.getTotalBounty() < minWanted) {
            // remove bounty tag
            NotBounties.removeWantedTag(bounty.getUUID());
        }

        for (Setter setter : claimedBounties) {
            if (!setter.getUuid().equals(new UUID(0, 0))) {
                if (SQL.isConnected()) {
                    data.addData(setter.getUuid().toString(), 0, 1, 0, 0, 0, 0);
                } else {
                    setBounties.put(setter.getUuid().toString(), Leaderboard.SET.getStat(setter.getUuid()) + 1);
                }
                Player p = Bukkit.getPlayer(setter.getUuid());
                if (p != null) {
                    p.playSound(p.getEyeLocation(), Sound.BLOCK_BEEHIVE_SHEAR, 1, 1);
                }
            }
        }
        gracePeriod.put(event.getEntity().getUniqueId().toString(), System.currentTimeMillis());

        if (trackerRemove > 0)
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack[] contents = p.getInventory().getContents();
                boolean change = false;
                for (int x = 0; x < contents.length; x++) {
                    if (contents[x] != null)
                        if (contents[x].getItemMeta() != null) {
                            if (Objects.requireNonNull(contents[x].getItemMeta()).getLore() != null) {
                                if (!Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).isEmpty()) {
                                    String lastLine = Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).get(Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).size() - 1);
                                    if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                        int number;
                                        try {
                                            number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                        } catch (NumberFormatException ignored) {
                                            return;
                                        }
                                        if (trackedBounties.containsKey(number)) {
                                            if (!hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                contents[x] = null;
                                                change = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                }
                if (change) {
                    p.getInventory().setContents(contents);
                }
            }
        Commands.reopenBountiesGUI();

        new BukkitRunnable() {
            @Override
            public void run() {
                // {player} is replaced with the person whose bounty was claimed
                // {killer} is replaced with the person who claimed the bounty
                // starting with [player] will run the command for the person whose bounty was claimed
                // starting with [killer] will run the command for the person who claimed the bounty\
                // starting with nothing will run the command as console
                // adding @(player/killer)permission or !@(player/killer)permission before everything with check for a permission of the player or killer to run the command
                // This will check if the killer has the permission and will message the person they killed
                // ex: @(killer)notbounties.view [killer] msg {player} It wasn't personal.
                // a command that is just <respawn> will force the player to respawn. This is useful if you want to tp them somewhere after they die
                // You can use placeholders, but they will always be parsed for the player who died
                for (String command : bountyClaimCommands) {
                    if (command.equalsIgnoreCase("<respawn>")) {
                        if (player.isDead())
                            player.spigot().respawn();
                        continue;
                    }
                    command = command.replaceAll("\\{player}", Matcher.quoteReplacement(player.getName()));
                    command = command.replaceAll("\\{killer}", Matcher.quoteReplacement(killer.getName()));
                    if (papiEnabled)
                        command = new PlaceholderAPIClass().parse(player, command);
                    if (command.startsWith("@(player)")) {
                        String permission = command.substring(9, command.indexOf(" "));
                        if (!player.hasPermission(permission))
                            continue;
                        command = command.substring(command.indexOf(" ") + 1);
                    } else if (command.startsWith("!@(player)")) {
                        String permission = command.substring(10, command.indexOf(" "));
                        if (player.hasPermission(permission))
                            continue;
                        command = command.substring(command.indexOf(" ") + 1);
                    } else if (command.startsWith("@(killer)")) {
                        String permission = command.substring(9, command.indexOf(" "));
                        if (!killer.hasPermission(permission))
                            continue;
                        command = command.substring(command.indexOf(" ") + 1);
                    } else if (command.startsWith("!@(killer)")) {
                        String permission = command.substring(10, command.indexOf(" "));
                        if (killer.hasPermission(permission))
                            continue;
                        command = command.substring(command.indexOf(" ") + 1);
                    }

                    if (command.startsWith("[player]")) {
                        Bukkit.dispatchCommand(player, command.substring(8));
                    } else if (command.startsWith("[killer]")) {
                        Bukkit.dispatchCommand(killer, command.substring(8));
                    } else if (command.startsWith("[sound_player] ")) {
                        command = command.substring(8);
                        double volume = 1;
                        double pitch = 1;
                        String sound;
                        if (command.contains(" ")) {
                            sound = command.substring(0, command.indexOf(" "));
                            command = command.substring(sound.length() + 1);
                            try {
                                if (command.contains(" ")) {
                                    volume = NumberFormatting.tryParse(command.substring(0, command.indexOf(" ")));
                                    command = command.substring(command.indexOf(" ") + 1);
                                    pitch = NumberFormatting.tryParse(command);
                                } else {
                                    volume = NumberFormatting.tryParse(command);
                                }
                            } catch (NumberFormatException e) {
                                Bukkit.getLogger().warning("[NotBounties] Unknown number for [sound_player] command in bounty-claim-commands : " + command);
                                continue;
                            }
                        } else {
                            sound = command;
                        }
                        Sound realSound;
                        try {
                            realSound = Sound.valueOf(sound.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            Bukkit.getLogger().warning("[NotBounties] Unknown sound for [sound_player] command in bounty-claim-commands : " + sound);
                            continue;
                        }
                        player.playSound(player.getEyeLocation(), realSound, (float) volume, (float) pitch);
                    } else if (command.startsWith("[sound_killer] ")) {
                        command = command.substring(8);
                        double volume = 1;
                        double pitch = 1;
                        String sound;
                        if (command.contains(" ")) {
                            sound = command.substring(0, command.indexOf(" "));
                            command = command.substring(sound.length() + 1);
                            try {
                                if (command.contains(" ")) {
                                    volume = NumberFormatting.tryParse(command.substring(0, command.indexOf(" ")));
                                    command = command.substring(command.indexOf(" ") + 1);
                                    pitch = NumberFormatting.tryParse(command);
                                } else {
                                    volume = NumberFormatting.tryParse(command);
                                }
                            } catch (NumberFormatException e) {
                                Bukkit.getLogger().warning("[NotBounties] Unknown number for [sound_killer] command in bounty-claim-commands : " + command);
                                continue;
                            }
                        } else {
                            sound = command;
                        }
                        Sound realSound;
                        try {
                            realSound = Sound.valueOf(sound.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            Bukkit.getLogger().warning("[NotBounties] Unknown sound for [sound_killer] command in bounty-claim-commands : " + sound);
                            continue;
                        }
                        killer.playSound(killer.getEyeLocation(), realSound, (float) volume, (float) pitch);
                    } else if (command.startsWith("[sound] ")) {
                        command = command.substring(8);
                        double volume = 1;
                        double pitch = 1;
                        String sound;
                        if (command.contains(" ")) {
                            sound = command.substring(0, command.indexOf(" "));
                            command = command.substring(sound.length() + 1);
                            try {
                                if (command.contains(" ")) {
                                    volume = NumberFormatting.tryParse(command.substring(0, command.indexOf(" ")));
                                    command = command.substring(command.indexOf(" ") + 1);
                                    pitch = NumberFormatting.tryParse(command);
                                } else {
                                    volume = NumberFormatting.tryParse(command);
                                }
                            } catch (NumberFormatException e) {
                                Bukkit.getLogger().warning("[NotBounties] Unknown number for [sound] command in bounty-claim-commands : " + command);
                                continue;
                            }
                        } else {
                            sound = command;
                        }
                        Sound realSound;
                        try {
                            realSound = Sound.valueOf(sound.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            Bukkit.getLogger().warning("[NotBounties] Unknown sound for [sound] command in bounty-claim-commands : " + sound);
                            continue;
                        }
                        killer.getWorld().playSound(killer.getLocation(), realSound, (float) volume, (float) pitch);
                    } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }

            }
        }.runTaskLater(NotBounties.getInstance(), 10);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (tracker)
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getItem() != null) {
                    ItemStack item = event.getItem();
                    Player player = event.getPlayer();
                    if (item.getType() == Material.COMPASS) {
                        if (item.getItemMeta() != null) {
                            if (item.getItemMeta().getLore() != null) {
                                if (!item.getItemMeta().getLore().isEmpty()) {
                                    String lastLine = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
                                    if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                        if (event.getPlayer().hasPermission("notbounties.tracker")) {
                                            int number;
                                            try {
                                                number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                            } catch (NumberFormatException ignored) {
                                                return;
                                            }
                                            if (trackedBounties.containsKey(number)) {
                                                if (hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                    CompassMeta compassMeta = (CompassMeta) item.getItemMeta();

                                                    String actionBar;
                                                    Bounty bounty = getBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))));
                                                    assert bounty != null;
                                                    Player p = Bukkit.getPlayer(bounty.getUUID());
                                                    if (p != null) {
                                                        compassMeta.setLodestone(p.getLocation());
                                                        compassMeta.setLodestoneTracked(false);
                                                        if (trackerGlow > 0) {
                                                            if (p.getWorld().equals(player.getWorld())) {
                                                                if (player.getLocation().distance(p.getLocation()) < trackerGlow) {
                                                                    p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 15, 0));
                                                                }
                                                            }
                                                        }
                                                        actionBar = ChatColor.DARK_GRAY + "|";
                                                        if (TABPlayerName) {
                                                            actionBar += " " + ChatColor.YELLOW + p.getName() + ChatColor.DARK_GRAY + " |";
                                                        }
                                                        if (TABDistance) {
                                                            if (p.getWorld().equals(player.getWorld())) {
                                                                actionBar += " " + ChatColor.GOLD + ((int) player.getLocation().distance(p.getLocation())) + "m" + ChatColor.DARK_GRAY + " |";
                                                            } else {
                                                                actionBar += " ?m |";
                                                            }
                                                        }
                                                        if (TABPosition) {
                                                            actionBar += " " + ChatColor.RED + p.getLocation().getBlockX() + "x " + p.getLocation().getBlockY() + "y " + p.getLocation().getBlockZ() + "z" + ChatColor.DARK_GRAY + " |";
                                                        }
                                                        if (TABWorld) {
                                                            actionBar += " " + ChatColor.LIGHT_PURPLE + p.getWorld().getName() + ChatColor.DARK_GRAY + " |";
                                                        }
                                                    } else {
                                                        actionBar = ChatColor.GRAY + "*offline*";
                                                        if (compassMeta.getLodestone() != null)
                                                            if (!Objects.equals(compassMeta.getLodestone().getWorld(), player.getWorld()))
                                                                if (Bukkit.getWorlds().size() > 1) {
                                                                    for (World world : Bukkit.getWorlds()) {
                                                                        if (!world.equals(player.getWorld())) {
                                                                            compassMeta.setLodestone(new Location(world, world.getSpawnLocation().getX(), world.getSpawnLocation().getY(), world.getSpawnLocation().getZ()));
                                                                        }
                                                                    }
                                                                } else {
                                                                    //compassMeta.setLodestone(null);
                                                                    compassMeta.setLodestoneTracked(true);
                                                                }
                                                    }
                                                    item.setItemMeta(compassMeta);
                                                    // display action bar
                                                    if (trackerActionBar) {
                                                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBar));
                                                    }
                                                }
                                            }
                                        } else {
                                            if (trackerActionBar) {
                                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "No Permission."));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            if (event.getItem() != null) {
                ItemStack item = event.getItem();
                Player player = event.getPlayer();
                if (item.getType() == Material.PAPER) {
                    if (item.getItemMeta() != null) {
                        if (item.getItemMeta().getLore() != null) {
                            if (!item.getItemMeta().getLore().isEmpty()) {
                                String lastLine = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
                                if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                    String reward = ChatColor.stripColor(lastLine).substring(1);
                                    double amount;
                                    try {
                                        amount = Double.parseDouble(reward);
                                    } catch (NumberFormatException ignored) {
                                        player.sendMessage(ChatColor.RED + "Error redeeming reward");
                                        return;
                                    }
                                    NumberFormatting.doAddCommands(player, amount);
                                    player.getInventory().remove(item);
                                    player.sendMessage(parse(speakings.get(0) + speakings.get(41), amount, player));
                                }
                            }
                        }
                    }
                }
            }
        }
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (NotBounties.boardSetup.containsKey(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                if (NotBounties.boardSetup.get(event.getPlayer().getUniqueId()) == -1) {
                    NotBounties.boardSetup.remove(event.getPlayer().getUniqueId());
                    event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.RED + "Canceled board removal.", event.getPlayer()));
                    return;
                }
                Location location = Objects.requireNonNull(event.getClickedBlock()).getRelative(event.getBlockFace()).getLocation();
                addBountyBoard(new BountyBoard(location, event.getBlockFace(), NotBounties.boardSetup.get(event.getPlayer().getUniqueId())));
                event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.GREEN + "Registered bounty board at " + location.getX() + " " + location.getY() + " " + location.getZ() + ".", event.getPlayer()));
                NotBounties.boardSetup.remove(event.getPlayer().getUniqueId());
            }
        }
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (NotBounties.boardSetup.containsKey(event.getPlayer().getUniqueId()) && NotBounties.boardSetup.get(event.getPlayer().getUniqueId()) == -1 && (event.getRightClicked().getType() == EntityType.ITEM_FRAME || (serverVersion >= 17 && event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME))) {
            event.setCancelled(true);
            int removes = removeSpecificBountyBoard((ItemFrame) event.getRightClicked());
            NotBounties.boardSetup.remove(event.getPlayer().getUniqueId());
            event.getPlayer().sendMessage(parse(speakings.get(0) + ChatColor.GREEN + "Removed " + removes + " bounty boards.", event.getPlayer()));
        }
    }

    @EventHandler
    public void onHold(PlayerItemHeldEvent event) {
        if (trackerRemove > 0) {
            ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
            if (item != null) {
                if (item.getType() == Material.COMPASS) {
                    if (item.getItemMeta() != null) {
                        if (item.getItemMeta().getLore() != null) {
                            if (!item.getItemMeta().getLore().isEmpty()) {
                                String lastLine = item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1);
                                if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                    int number;
                                    try {
                                        number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                    } catch (NumberFormatException ignored) {
                                        return;
                                    }
                                    if (trackedBounties.containsKey(number)) {
                                        if (!hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                            event.getPlayer().getInventory().setItem(event.getNewSlot(), null);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onOpenInv(InventoryOpenEvent event) {
        if (tracker)
            if (trackerRemove == 3)
                if (event.getInventory().getType() != InventoryType.CRAFTING) {
                    ItemStack[] contents = event.getInventory().getContents();
                    boolean change = false;
                    for (int x = 0; x < contents.length; x++) {
                        if (contents[x] != null) {
                            if (contents[x].getType() == Material.COMPASS) {
                                if (contents[x].getItemMeta() != null) {
                                    if (Objects.requireNonNull(contents[x].getItemMeta()).getLore() != null) {
                                        if (!Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).isEmpty()) {
                                            String lastLine = Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).get(Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).size() - 1);
                                            if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                                int number;
                                                try {
                                                    number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                                } catch (NumberFormatException ignored) {
                                                    return;
                                                }
                                                if (trackedBounties.containsKey(number)) {
                                                    if (!hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                        contents[x] = null;
                                                        change = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (change) {
                        event.getInventory().setContents(contents);
                    }
                    contents = event.getView().getPlayer().getInventory().getContents();
                    change = false;
                    for (int x = 0; x < contents.length; x++) {
                        if (contents[x] != null) {
                            if (contents[x].getType() == Material.COMPASS) {
                                if (contents[x].getItemMeta() != null) {
                                    if (Objects.requireNonNull(contents[x].getItemMeta()).getLore() != null) {
                                        if (!Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).isEmpty()) {
                                            String lastLine = Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).get(Objects.requireNonNull(Objects.requireNonNull(contents[x].getItemMeta()).getLore()).size() - 1);
                                            if (lastLine.contains(ChatColor.BLACK + "") && ChatColor.stripColor(lastLine).charAt(0) == '@') {
                                                int number;
                                                try {
                                                    number = Integer.parseInt(ChatColor.stripColor(lastLine).substring(1));
                                                } catch (NumberFormatException ignored) {
                                                    return;
                                                }
                                                if (trackedBounties.containsKey(number)) {
                                                    if (!hasBounty(Bukkit.getOfflinePlayer(UUID.fromString(trackedBounties.get(number))))) {
                                                        contents[x] = null;
                                                        change = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (change) {
                        event.getPlayer().getInventory().setContents(contents);
                    }
                }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // check if they are logged yet
        if (!loggedPlayers.containsValue(event.getPlayer().getUniqueId().toString())) {
            // if not, add them
            loggedPlayers.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer().getUniqueId().toString());
        } else {
            // if they are, check if their username has changed, and update it
            if (!loggedPlayers.containsKey(event.getPlayer().getName().toLowerCase(Locale.ROOT))) {
                String nameToRemove = "";
                for (Map.Entry<String, String> entry : loggedPlayers.entrySet()) {
                    if (entry.getValue().equals(event.getPlayer().getUniqueId().toString())) {
                        nameToRemove = entry.getKey();
                    }
                }
                if (!Objects.equals(nameToRemove, "")) {
                    loggedPlayers.remove(nameToRemove);
                }
                loggedPlayers.put(event.getPlayer().getName().toLowerCase(Locale.ROOT), event.getPlayer().getUniqueId().toString());
            }
        }

        if (hasBounty(event.getPlayer())) {
            Bounty bounty = getBounty(event.getPlayer());
            assert bounty != null;
            bounty.setDisplayName(event.getPlayer().getName());
            double addedAmount = 0;
            for (Setter setter : bounty.getSetters()) {
                if (!setter.isNotified()) {
                    event.getPlayer().sendMessage(parse(speakings.get(0) + speakings.get(12), setter.getName(), setter.getAmount(), event.getPlayer()));
                    setter.setNotified(true);
                    addedAmount += setter.getAmount();
                }
            }
            bounty.combineSetters();
            updateBounty(bounty);
            if (bounty.getTotalBounty() - addedAmount < bBountyThreshold && bounty.getTotalBounty() > bBountyThreshold) {
                event.getPlayer().sendMessage(parse(speakings.get(0) + speakings.get(43), event.getPlayer()));
                if (bBountyCommands != null && !bBountyCommands.isEmpty()) {
                    for (String command : bBountyCommands) {
                        if (papiEnabled)
                            command = new PlaceholderAPIClass().parse(event.getPlayer(), command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replaceAll("\\{player}", Matcher.quoteReplacement(event.getPlayer().getName())).replaceAll("\\{amount}", Matcher.quoteReplacement(bounty.getTotalBounty() + "")));
                    }
                }
            }
            if (bounty.getTotalBounty() > bBountyThreshold) {
                displayParticle.add(event.getPlayer());
            }
            if (wanted && bounty.getTotalBounty() >= minWanted) {
                if (!NotBounties.wantedText.containsKey(event.getPlayer().getUniqueId())) {
                    NotBounties.wantedText.put(event.getPlayer().getUniqueId(), new AboveNameText(event.getPlayer()));
                }
            }
        }

        if (headRewards.containsKey(event.getPlayer().getUniqueId())) {
            for (RewardHead rewardHead : headRewards.get(event.getPlayer().getUniqueId())) {
                NumberFormatting.givePlayer(event.getPlayer(), rewardHead.getItem(), 1);
            }
            headRewards.remove(event.getPlayer().getUniqueId());
        }
        // if they have expire-time enabled
        if (bountyExpire > 0) {
            boolean change = false;
            // go through all the bounties and remove setters if it has been more than expire time
            ListIterator<Bounty> bountyIterator = expiredBounties.listIterator();
            while (bountyIterator.hasNext()) {
                Bounty bounty = bountyIterator.next();
                ListIterator<Setter> setterIterator = bounty.getSetters().listIterator();
                while (setterIterator.hasNext()) {
                    Setter setter = setterIterator.next();
                    // check if past expire time
                    if (System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire) {
                        // refund money
                        NumberFormatting.doAddCommands(event.getPlayer(), setter.getAmount());
                        event.getPlayer().sendMessage(parse(speakings.get(0) + speakings.get(31), bounty.getName(), setter.getAmount(), event.getPlayer()));
                        setterIterator.remove();
                    }
                }
                //bounty.getSetters().removeIf(setter -> System.currentTimeMillis() - setter.getTimeCreated() > 1000L * 60 * 60 * 24 * bountyExpire);
                // remove bounty if all the setters have been removed
                if (bounty.getSetters().isEmpty()) {
                    bountyIterator.remove();
                    change = true;
                }
            }
            if (change) {
                Commands.reopenBountiesGUI();
            }
        }

        // check for updates
        if (updateNotification && !NotBounties.latestVersion) {
            if (event.getPlayer().hasPermission("notbounties.admin")) {
                new UpdateChecker(NotBounties.getInstance(), 104484).getVersion(version -> {
                    if (NotBounties.getInstance().getDescription().getVersion().contains("dev"))
                        return;
                    if (NotBounties.getInstance().getDescription().getVersion().equals(version))
                        return;
                    event.getPlayer().sendMessage(parse(speakings.get(0), event.getPlayer()) + ChatColor.YELLOW + "A new update is available. Current version: " + ChatColor.GOLD + NotBounties.getInstance().getDescription().getVersion() + ChatColor.YELLOW + " Latest version: " + ChatColor.GREEN + version);
                    event.getPlayer().sendMessage(ChatColor.YELLOW + "Download a new version here:" + ChatColor.GRAY + " https://www.spigotmc.org/resources/104484/");
                });
            }
        }

        // check if they had a bounty refunded
        if (refundedBounties.containsKey(event.getPlayer().getUniqueId())) {
            NumberFormatting.doAddCommands(event.getPlayer(), refundedBounties.get(event.getPlayer().getUniqueId()));
            refundedBounties.remove(event.getPlayer().getUniqueId());
        }
    }



    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (serverVersion <= 16)
            if (wanted || !bountyBoards.isEmpty())
                for (Entity entity : event.getChunk().getEntities()) {
                    if (entity == null)
                        return;
                    if (entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.ITEM_FRAME)
                        continue;
                    PersistentDataContainer container = entity.getPersistentDataContainer();
                    if (container.has(namespacedKey, PersistentDataType.STRING)) {
                        String value = container.get(namespacedKey, PersistentDataType.STRING);
                        if (value == null)
                            continue;
                        if (!value.equals(sessionKey)) {
                            entity.remove();
                        }
                    }
                }
    }

}
