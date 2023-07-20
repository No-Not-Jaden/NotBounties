package me.jadenp.notbounties;

import me.clip.placeholderapi.PlaceholderAPI;
import me.jadenp.notbounties.gui.CustomItem;
import me.jadenp.notbounties.gui.GUI;
import me.jadenp.notbounties.gui.GUIOptions;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class ConfigOptions {
    public static boolean autoConnect;
    public static boolean migrateLocalData;
    public static List<String> bBountyCommands = new ArrayList<>();
    public static boolean tracker;
    public static boolean giveOwnTracker;
    public static int trackerRemove;
    public static int trackerGlow;
    public static boolean trackerActionBar;
    public static boolean TABShowAlways;
    public static boolean TABPlayerName;
    public static boolean TABDistance;
    public static boolean TABPosition;
    public static boolean TABWorld;
    public static int minBroadcast;
    public static int bBountyThreshold;
    public static boolean bBountyParticle;
    public static boolean buyImmunity;
    public static boolean permanentImmunity;
    public static int permanentCost;
    public static double scalingRatio;
    public static int graceTime;
    public static int minBounty;
    public static double bountyTax;
    public static boolean rewardHeadClaimed;
    public static boolean redeemRewardLater;
    public static boolean rewardHeadSetter;
    public static boolean buyBack;
    public static double buyBackInterest;
    public static String buyBackLore;
    public static boolean usingPapi;
    public static String currency;
    public static List<String> removeCommands;
    public static List<String> addCommands;
    public static int bountyExpire;
    public static boolean papiEnabled;
    public static String currencyPrefix;
    public static String currencySuffix;
    public static File language;
    public static List<String> trackerLore = new ArrayList<>();
    public static List<String> voucherLore = new ArrayList<>();
    public static List<String> speakings = new ArrayList<>();
    public static List<String> hiddenNames = new ArrayList<>();
    public static boolean updateNotification;
    public static Map<String, CustomItem> customItems = new HashMap<>();
    public static int numberFormatting;
    public static char nfThousands;
    public static int nfDecimals;
    public static LinkedHashMap<Long, String> nfDivisions = new LinkedHashMap<>();
    public static char decimalSymbol;
    public static DecimalFormat decimalFormat;
    public static DecimalFormat divisionFormat;
    public static int decimals;

    public static void reloadOptions() throws IOException {
        NotBounties bounties = NotBounties.getInstance();

        papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        language = new File(bounties.getDataFolder() + File.separator + "language.yml");

        bounties.reloadConfig();

        if (bounties.getConfig().isString("currency")) {
            String prevOption = bounties.getConfig().getString("currency");
            bounties.getConfig().set("currency", null);
            bounties.getConfig().set("currency.object", prevOption);
            bounties.getConfig().set("currency.prefix", "");
            bounties.getConfig().set("currency.suffix", "");
        }
        if (!bounties.getConfig().isSet("currency.decimals"))
            bounties.getConfig().set("currency.decimals", 2);
        if (!bounties.getConfig().isSet("currency.prefix"))
            bounties.getConfig().set("currency.prefix", "&f");
        if (!bounties.getConfig().isSet("currency.suffix"))
            bounties.getConfig().set("currency.suffix", "&b◆");
        if (!bounties.getConfig().isSet("minimum-bounty"))
            bounties.getConfig().set("minimum-bounty", 1);
        if (!bounties.getConfig().isSet("bounty-tax"))
            bounties.getConfig().set("bounty-tax", 0.0);
        if (bounties.getConfig().isSet("add-currency-commands")) {
            bounties.getConfig().set("currency.add-commands", bounties.getConfig().getStringList("add-currency-commands"));
            bounties.getConfig().set("currency.remove-commands", bounties.getConfig().getStringList("remove-currency-commands"));
            bounties.getConfig().set("add-currency-commands", null);
            bounties.getConfig().set("remove-currency-commands", null);
        }
        if (!bounties.getConfig().isSet("currency.add-commands"))
            bounties.getConfig().set("currency.add-commands", new ArrayList<>());
        if (!bounties.getConfig().isSet("currency.remove-commands"))
            bounties.getConfig().set("currency.remove-commands", new ArrayList<>());
        if (!bounties.getConfig().isSet("bounty-expire")) bounties.getConfig().set("bounty-expire", -1);
        if (bounties.getConfig().isBoolean("reward-heads")) {
            boolean prevOption = bounties.getConfig().getBoolean("reward-heads");
            bounties.getConfig().set("reward-heads", null);
            bounties.getConfig().set("reward-heads.setters", prevOption);
        }
        if (!bounties.getConfig().isSet("reward-heads.setters"))
            bounties.getConfig().set("reward-heads.setters", false);
        if (!bounties.getConfig().isSet("reward-heads.claimed"))
            bounties.getConfig().set("reward-heads.claimed", false);
        if (!bounties.getConfig().isSet("buy-own-bounties.enabled"))
            bounties.getConfig().set("buy-own-bounties.enabled", false);
        if (!bounties.getConfig().isSet("buy-own-bounties.cost-multiply"))
            bounties.getConfig().set("buy-own-bounties.cost-multiply", 1.25);
        if (!bounties.getConfig().isSet("buy-own-bounties.lore-addition"))
            bounties.getConfig().set("buy-own-bounties.lore-addition", "&9Left Click &7to buy back for &a{amount}");
        if (!bounties.getConfig().isSet("immunity.buy-immunity"))
            bounties.getConfig().set("immunity.buy-immunity", false);
        if (!bounties.getConfig().isSet("immunity.permanent-immunity.enabled"))
            bounties.getConfig().set("immunity.permanent-immunity.enabled", false);
        if (!bounties.getConfig().isSet("immunity.permanent-immunity.cost"))
            bounties.getConfig().set("immunity.permanent-immunity.cost", 128);
        if (!bounties.getConfig().isSet("immunity.scaling-immunity.ratio"))
            bounties.getConfig().set("immunity.scaling-immunity.ratio", 1.0);
        if (!bounties.getConfig().isSet("immunity.grace-period")) bounties.getConfig().set("immunity.grace-period", 10);

        if (!bounties.getConfig().isSet("bounty-tracker.enabled"))
            bounties.getConfig().set("bounty-tracker.enabled", true);
        if (!bounties.getConfig().isSet("bounty-tracker.give-own"))
            bounties.getConfig().set("bounty-tracker.give-own", false);
        if (!bounties.getConfig().isSet("bounty-tracker.remove"))
            bounties.getConfig().set("bounty-tracker.remove", 2);
        if (!bounties.getConfig().isSet("bounty-tracker.glow"))
            bounties.getConfig().set("bounty-tracker.glow", 10);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.enabled"))
            bounties.getConfig().set("bounty-tracker.action-bar.enabled", true);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.show-always"))
            bounties.getConfig().set("bounty-tracker.action-bar.show-always", true);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.player-name"))
            bounties.getConfig().set("bounty-tracker.action-bar.player-name", true);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.distance"))
            bounties.getConfig().set("bounty-tracker.action-bar.distance", true);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.position"))
            bounties.getConfig().set("bounty-tracker.action-bar.position", false);
        if (!bounties.getConfig().isSet("bounty-tracker.action-bar.world"))
            bounties.getConfig().set("bounty-tracker.action-bar.world", false);
        if (!bounties.getConfig().isSet("redeem-reward-later"))
            bounties.getConfig().set("redeem-reward-later", false);
        if (!bounties.getConfig().isSet("minimum-broadcast"))
            bounties.getConfig().set("minimum-broadcast", 100);
        if (!bounties.getConfig().isSet("big-bounties.bounty-threshold"))
            bounties.getConfig().set("big-bounties.bounty-threshold", -1);
        if (!bounties.getConfig().isSet("big-bounties.particle"))
            bounties.getConfig().set("big-bounties.particle", true);
        if (!bounties.getConfig().isSet("big-bounties.commands"))
            bounties.getConfig().set("big-bounties.commands", new ArrayList<>(Collections.singletonList("execute run effect give {player} minecraft:glowing 10 0")));
        if (!bounties.getConfig().isSet("database.host"))
            bounties.getConfig().set("database.host", "localhost");
        if (!bounties.getConfig().isSet("database.port"))
            bounties.getConfig().set("database.port", "3306");
        if (!bounties.getConfig().isSet("database.database"))
            bounties.getConfig().set("database.database", "db");
        if (!bounties.getConfig().isSet("database.user"))
            bounties.getConfig().set("database.user", "username");
        if (!bounties.getConfig().isSet("database.password"))
            bounties.getConfig().set("database.password", "");
        if (!bounties.getConfig().isSet("database.use-ssl"))
            bounties.getConfig().set("database.use-ssl", false);
        if (!bounties.getConfig().isSet("database.migrate-local-data"))
            bounties.getConfig().set("database.migrate-local-data", true);
        if (!bounties.getConfig().isSet("database.auto-connect"))
            bounties.getConfig().set("database.auto-connect", false);
        if (!bounties.getConfig().isSet("hide-stats"))
            bounties.getConfig().set("hide-stats", new ArrayList<>());
        if (!bounties.getConfig().isSet("update-notification"))
            bounties.getConfig().set("update-notification", true);
        if (!bounties.getConfig().isSet("number-formatting.type"))
            bounties.getConfig().set("number-formatting.type", 1);
        if (!bounties.getConfig().isSet("number-formatting.thousands"))
            bounties.getConfig().set("number-formatting.thousands", ",");
        if (!bounties.getConfig().isSet("number-formatting.divisions.decimals")) {
            bounties.getConfig().set("number-formatting.divisions.decimals", 2);
            bounties.getConfig().set("number-formatting.divisions.1000", "K");
        }
        if (!bounties.getConfig().isSet("number-formatting.decimal-symbol"))
            bounties.getConfig().set("number-formatting.decimal-symbol", ".");

        bounties.saveConfig();

        currency = bounties.getConfig().getString("currency.object");
        usingPapi = Objects.requireNonNull(bounties.getConfig().getString("currency.object")).contains("%");

        currencyPrefix = color(Objects.requireNonNull(bounties.getConfig().getString("currency.prefix")));
        currencySuffix = color(Objects.requireNonNull(bounties.getConfig().getString("currency.suffix")));
        if (bounties.getConfig().isList("currency.add-commands")){
            addCommands = bounties.getConfig().getStringList("currency.add-commands");
        } else {
            addCommands = Collections.singletonList(bounties.getConfig().getString("currency.add-commands"));
        }
        if (bounties.getConfig().isList("currency.remove-commands")){
            removeCommands = bounties.getConfig().getStringList("currency.remove-commands");
        } else {
            removeCommands = Collections.singletonList(bounties.getConfig().getString("currency.remove-commands"));
        }
        bountyExpire = bounties.getConfig().getInt("bounty-expire");
        rewardHeadSetter = bounties.getConfig().getBoolean("reward-heads.setters");
        rewardHeadClaimed = bounties.getConfig().getBoolean("reward-heads.claimed");
        buyBack = bounties.getConfig().getBoolean("buy-own-bounties.enabled");
        buyBackInterest = bounties.getConfig().getDouble("buy-own-bounties.cost-multiply");
        buyBackLore = color(Objects.requireNonNull(bounties.getConfig().getString("buy-own-bounties.lore-addition")));
        buyImmunity = bounties.getConfig().getBoolean("immunity.buy-immunity");
        permanentImmunity = bounties.getConfig().getBoolean("immunity.permanent-immunity.enabled");
        permanentCost = bounties.getConfig().getInt("immunity.permanent-immunity.cost");
        scalingRatio = bounties.getConfig().getDouble("immunity.scaling-immunity.ratio");
        graceTime = bounties.getConfig().getInt("immunity.grace-period");
        minBounty = bounties.getConfig().getInt("minimum-bounty");
        bountyTax = bounties.getConfig().getDouble("bounty-tax");
        tracker = bounties.getConfig().getBoolean("bounty-tracker.enabled");
        giveOwnTracker = bounties.getConfig().getBoolean("bounty-tracker.give-own");
        trackerRemove = bounties.getConfig().getInt("bounty-tracker.remove");
        trackerGlow = bounties.getConfig().getInt("bounty-tracker.glow");
        trackerActionBar = bounties.getConfig().getBoolean("bounty-tracker.action-bar.enabled");
        TABShowAlways = bounties.getConfig().getBoolean("bounty-tracker.action-bar.show-always");
        TABPlayerName = bounties.getConfig().getBoolean("bounty-tracker.action-bar.player-name");
        TABDistance = bounties.getConfig().getBoolean("bounty-tracker.action-bar.distance");
        TABPosition = bounties.getConfig().getBoolean("bounty-tracker.action-bar.position");
        TABWorld = bounties.getConfig().getBoolean("bounty-tracker.action-bar.world");
        redeemRewardLater = bounties.getConfig().getBoolean("redeem-reward-later");
        minBroadcast = bounties.getConfig().getInt("minimum-broadcast");
        bBountyThreshold = bounties.getConfig().getInt("big-bounties.bounty-threshold");
        bBountyParticle = bounties.getConfig().getBoolean("big-bounties.particle");
        bBountyCommands = bounties.getConfig().getStringList("big-bounties.commands");
        migrateLocalData = bounties.getConfig().getBoolean("database.migrate-local-data");
        autoConnect = bounties.getConfig().getBoolean("database.auto-connect");
        hiddenNames = bounties.getConfig().getStringList("hide-stats");
        updateNotification = bounties.getConfig().getBoolean("update-notification");
        numberFormatting = bounties.getConfig().getInt("number-formatting.type");
        nfDecimals = bounties.getConfig().getInt("number-formatting.divisions.decimals");
        decimals = bounties.getConfig().getInt("currency.decimals");
        String thousandsSymbol = bounties.getConfig().getString("number-formatting.thousands");
        assert thousandsSymbol != null;
        if (thousandsSymbol.isEmpty())
            thousandsSymbol = " ";
        nfThousands = thousandsSymbol.charAt(0);
        String decimalSymbolString = bounties.getConfig().getString("number-formatting.decimal-symbol");
        assert decimalSymbolString != null;
        if (decimalSymbolString.isEmpty())
            decimalSymbolString = " ";
        decimalSymbol = decimalSymbolString.charAt(0);

        Locale locale = new Locale("en", "US");

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        symbols.setDecimalSeparator(decimalSymbol);
        symbols.setGroupingSeparator(nfThousands);

        StringBuilder pattern = new StringBuilder("#");
        if (numberFormatting == 1)
            pattern.append(',');
        pattern.append("###");
        pattern.append('.');
        if (decimals > 0) {
            for (int i = 0; i < decimals; i++) {
                pattern.append("#");
            }
        }
        decimalFormat = new DecimalFormat(pattern.toString(), symbols);

        StringBuilder divisionPattern = new StringBuilder("#");
        divisionPattern.append('.');
        if (nfDecimals > 0) {
            for (int i = 0; i < nfDecimals; i++) {
                divisionPattern.append("#");
            }
        }
        divisionFormat = new DecimalFormat(divisionPattern.toString(), symbols);

        nfDivisions.clear();
        Map<Long, String> preDivisions = new HashMap<>();
        for (String s : Objects.requireNonNull(bounties.getConfig().getConfigurationSection("number-formatting.divisions")).getKeys(false)) {
            if (s.equals("decimals"))
                continue;
            try {
                preDivisions.put(Long.parseLong(s), bounties.getConfig().getString("number-formatting.divisions." + s));
            } catch (NumberFormatException e) {
                Bukkit.getLogger().warning("Division is not a number: " + s);
            }
        }
        nfDivisions = sortByValue(preDivisions);

        File guiFile = new File(bounties.getDataFolder() + File.separator + "gui.yml");
        if (!guiFile.exists()) {
            bounties.saveResource("gui.yml", false);
        }
        if (bounties.getConfig().isConfigurationSection("advanced-gui")) {
            // migrate everything to gui.yml
            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(guiFile);
            configuration.set("bounty-gui", null);
            ConfigurationSection section = bounties.getConfig().getConfigurationSection("advanced-gui");
            assert section != null;
            configuration.set("bounty-gui.sort-type", section.get("sort-type"));
            configuration.set("bounty-gui.size", section.get("size"));
            for (String key : Objects.requireNonNull(section.getConfigurationSection("custom-items")).getKeys(false)) {
                configuration.set("custom-items." + key, section.get("custom-items." + key));
            }
            configuration.set("bounty-gui.player-slots", section.get("bounty-slots"));
            configuration.set("bounty-gui.remove-page-items", true);
            configuration.set("bounty-gui.head-lore", Arrays.asList("&7<&m                        &7>", "&4Bounty: &6%notbounties_bounty_formatted%", "&4&oKill this player to", "&4&oreceive this reward", "&7<&m                        &7>"));
            configuration.set("bounty-gui.head-name", "&4☠ &c&l{player} &4☠");

            configuration.set("bounty-gui.gui-name", "&d&lBounties &9&lPage");
            configuration.set("bounty-gui.add-page", true);

            if (language.exists()) {
                YamlConfiguration languageConfig = YamlConfiguration.loadConfiguration(language);
                if (languageConfig.isSet("bounty-item-name") && languageConfig.isSet("bounty-item-lore") && languageConfig.isSet("bounty-item-lore")) {
                    // convert {amount} to %notbounties_bounty_formatted%
                    String unformatted = languageConfig.getString("bounty-item-name");
                    assert unformatted != null;
                    unformatted = unformatted.replaceAll("\\{amount}", "%notbounties_bounty_formatted%");
                    configuration.set("bounty-gui.head-name", unformatted);

                    List<String> unformattedList = languageConfig.getStringList("bounty-item-lore");
                    unformattedList.replaceAll(s -> s.replaceAll("\\{amount}", "%notbounties_bounty_formatted%"));
                    configuration.set("bounty-gui.head-lore", unformattedList);
                    configuration.set("bounty-gui.gui-name", languageConfig.getString("gui-name"));
                    languageConfig.set("gui-name", null);
                    languageConfig.set("bounty-item-name", null);
                    languageConfig.set("bounty-item-lore", null);
                    languageConfig.save(language);
                }
            }

            for (String key : Objects.requireNonNull(section.getConfigurationSection("layout")).getKeys(false)) {
                configuration.set("bounty-gui.layout." + key, section.get("layout." + key));
            }

            bounties.getConfig().set("advanced-gui", null);
            configuration.save(guiFile);
        }

        customItems.clear();
        YamlConfiguration guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        for (String key : Objects.requireNonNull(guiConfig.getConfigurationSection("custom-items")).getKeys(false)) {

            Material material = Material.STONE;
            String mat = guiConfig.getString("custom-items." + key + ".material");
            if (mat != null)
                try {
                    material = Material.valueOf(mat.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("Unknown material \"" + mat + "\" in " + guiConfig.getName());
                }
            int amount = guiConfig.isInt("custom-items." + key + ".amount") ? guiConfig.getInt("custom-items." + key + ".amount") : 1;

            ItemStack itemStack = new ItemStack(material, amount);
            ItemMeta meta = itemStack.getItemMeta();
            assert meta != null;
            if (guiConfig.isSet("custom-items." + key + ".name")) {
                meta.setDisplayName(guiConfig.getString("custom-items." + key + ".name"));
            }
            if (guiConfig.isSet("custom-items." + key + ".custom-model-data")) {
                meta.setCustomModelData(guiConfig.getInt("custom-items." + key + ".custom-model-data"));
            }
            if (guiConfig.isSet("custom-items." + key + ".lore")) {
                meta.setLore(guiConfig.getStringList("custom-items." + key + ".lore"));
            }
            if (guiConfig.isSet("custom-items." + key + ".enchanted")) {
                if (guiConfig.getBoolean("custom-items." + key + ".enchanted")) {
                    itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            }
            if (guiConfig.getBoolean("custom-items." + key + ".hide-nbt")) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            }
            itemStack.setItemMeta(meta);

            List<String> itemCommands = guiConfig.isSet("custom-items." + key + ".commands") ? guiConfig.getStringList("custom-items." + key + ".commands") : new ArrayList<>();
            CustomItem customItem = new CustomItem(itemStack, itemCommands);
            customItems.put(key, customItem);
        }
        for (String key : guiConfig.getKeys(false)) {
            if (key.equals("custom-items"))
                continue;
            try {
                GUI.addGUI(new GUIOptions(Objects.requireNonNull(guiConfig.getConfigurationSection(key))), key);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("Unknown GUI in gui.yml: \"" + key + "\"");
            }
        }


        if (!speakings.isEmpty())
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getOpenInventory().getType() != InventoryType.CRAFTING) {
                    if (player.getOpenInventory().getTitle().contains(speakings.get(35))) {
                        player.closeInventory();
                    }
                }
            }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(language);

        if (!configuration.isSet("prefix"))
            configuration.set("prefix", "&7[&9Not&dBounties&7] &8» &r");
        if (!configuration.isSet("unknown-number"))
            configuration.set("unknown-number", "&cUnknown number!");
        if (!configuration.isSet("bounty-success"))
            configuration.set("bounty-success", "&aBounty placed on &e{player}&a for &e{amount}&a!");
        if (!configuration.isSet("unknown-player"))
            configuration.set("unknown-player", "&cCould not find the player &4{player}&c!");
        if (!configuration.isSet("bounty-broadcast"))
            configuration.set("bounty-broadcast", "&e{player}&6 has placed a bounty of &f{amount}&6 on &e{receiver}&6! Total Bounty: &f{bounty}");
        if (!configuration.isSet("no-permission"))
            configuration.set("no-permission", "&cYou do not have permission to execute this command!");
        if (!configuration.isSet("broke"))
            configuration.set("broke", "&cYou do not have enough currency for this! &8Required: &7{amount}");
        if (!configuration.isSet("claim-bounty-broadcast"))
            configuration.set("claim-bounty-broadcast", "&e{player}&6 has claimed the bounty of &f{amount}&6 on &e{receiver}&6!");
        if (!configuration.isSet("no-bounty"))
            configuration.set("no-bounty", "&4{receiver} &cdoesn't have a bounty!");
        if (!configuration.isSet("check-bounty"))
            configuration.set("check-bounty", "&e{receiver}&a has a bounty of &e{amount}&a.");
        if (!configuration.isSet("list-setter"))
            configuration.set("list-setter", "&e{player} &7> &a{amount}");
        if (!configuration.isSet("list-total"))
            configuration.set("list-total", "&e{player} &7> &a{amount}");
        if (!configuration.isSet("offline-bounty"))
            configuration.set("offline-bounty", "&e{player}&6 has set a bounty on you while you were offline!");
        if (!configuration.isSet("success-remove-bounty"))
            configuration.set("success-remove-bounty", "&cSuccessfully removed &4{receiver}'s &cbounty.");
        if (!configuration.isSet("success-edit-bounty"))
            configuration.set("success-edit-bounty", "&cSuccessfully edited &4{receiver}'s &cbounty.");
        if (!configuration.isSet("no-setter"))
            configuration.set("no-setter", "&4{player} &chas not set a bounty on {receiver}");
        if (!configuration.isSet("repeat-command-bounty"))
            configuration.set("repeat-command-bounty", "&6Please type this command in again in the next 30 seconds to confirm buying your bounty for &e{amount}&6.");
        if (!configuration.isSet("repeat-command-immunity"))
            configuration.set("repeat-command-immunity", "&6Please type this command in again in the next 30 seconds to confirm buying immunity for &e{amount}&6.");
        if (!configuration.isSet("permanent-immunity"))
            configuration.set("permanent-immunity", "&6{player} &eis immune to bounties!");
        if (!configuration.isSet("scaling-immunity"))
            configuration.set("scaling-immunity", "&6{player} &eis immune to bounties less than &e{amount}&6.");
        if (!configuration.isSet("buy-permanent-immunity"))
            configuration.set("buy-permanent-immunity", "&aYou have bought immunity from bounties.");
        if (!configuration.isSet("buy-scaling-immunity"))
            configuration.set("buy-scaling-immunity", "&aYou have bought immunity from bounties under the amount of &2{amount}&a.");
        if (!configuration.isSet("grace-period"))
            configuration.set("grace-period", "&cA bounty had just been claimed on &4{player}&c. Please wait &4{time}&c until you try again.");
        if (!configuration.isSet("min-bounty"))
            configuration.set("min-bounty", "&cThe bounty must be at least &4{amount}&c.");
        if (!configuration.isSet("unknown-command"))
            configuration.set("unknown-command", "&dUse &9/bounty help &dfor a list of commands.");
        if (!configuration.isSet("already-bought-perm"))
            configuration.set("already-bought-perm", "&cYou already have permanent immunity!");
        if (!configuration.isSet("removed-immunity"))
            configuration.set("removed-immunity", "&aSuccessfully removed your immunity to bounties.");
        if (!configuration.isSet("removed-other-immunity"))
            configuration.set("removed-other-immunity", "&aSuccessfully removed &2{receiver}''s &aimmunity to bounties.");
        if (!configuration.isSet("no-immunity"))
            configuration.set("no-immunity", "&cYou do not have purchased immunity!");
        if (!configuration.isSet("no-immunity-other"))
            configuration.set("no-immunity-other", "&4{receiver} &cdoes not have purchased immunity!");
        if (!configuration.isSet("expired-bounty"))
            configuration.set("expired-bounty", "&eYour bounty on &6{player}&e has expired. You have been refunded &2{amount}&e.");
        if (!configuration.isSet("bounty-tracker-name"))
            configuration.set("bounty-tracker-name", "&eBounty Tracker: &6&l{player}");
        if (!configuration.isSet("bounty-tracker-lore"))
            configuration.set("bounty-tracker-lore", Arrays.asList("", "&7Follow this compass", "&7to find {player}", ""));
        if (!configuration.isSet("tracker-give"))
            configuration.set("tracker-give", "&eYou have given &6{receiver}&e a compass that tracks &6{player}&e.");
        if (!configuration.isSet("tracker-receive"))
            configuration.set("tracker-receive", "&eYou have been given a bounty tracker for &6{player}&e.");
        if (!configuration.isSet("tracked-notify"))
            configuration.set("tracked-notify", "&c&lYou are being tracked!");
        if (!configuration.isSet("bounty-top"))
            configuration.set("bounty-top", "&9&l{rank}. &d{player} &7> &a{amount}");
        if (!configuration.isSet("bounty-top-title"))
            configuration.set("bounty-top-title", "&7&m               &r &d&lBounties &9&lTop &7&m               ");
        if (!configuration.isSet("enable-broadcast"))
            configuration.set("enable-broadcast", "&eYou have &aenabled &ebounty broadcast!");
        if (!configuration.isSet("disable-broadcast"))
            configuration.set("disable-broadcast", "&eYou have &cdisabled &ebounty broadcast!");
        if (!configuration.isSet("bounty-voucher-name"))
            configuration.set("bounty-voucher-name", "&6{player}'s&e claimed bounty of &a{amount}&e.");
        if (!configuration.isSet("bounty-voucher-lore"))
            configuration.set("bounty-voucher-lore", Arrays.asList("", "&2Awarded to {receiver}", "&7Right click to redeem", "&7this player's bounty", ""));
        if (!configuration.isSet("redeem-voucher"))
            configuration.set("redeem-voucher", "&aSuccessfully redeemed voucher for {amount}!");
        if (!configuration.isSet("bounty-receiver"))
            configuration.set("bounty-receiver", "&4{player} &cset a bounty on you for &4{amount}&c! Total Bounty: &4{bounty}");
        if (!configuration.isSet("big-bounty"))
            configuration.set("big-bounty", "&eYour bounty is very impressive!");
        if (configuration.isSet("bounty-stat-all")) {
            configuration.set("bounty-stat.all.long", configuration.getString("bounty-stat-all"));
            configuration.set("bounty-stat.kills.long", configuration.getString("bounty-stat-kills"));
            configuration.set("bounty-stat.claimed.long", configuration.getString("bounty-stat-claimed"));
            configuration.set("bounty-stat.deaths.long", configuration.getString("bounty-stat-deaths"));
            configuration.set("bounty-stat.set.long", configuration.getString("bounty-stat-set"));
            configuration.set("bounty-stat.immunity.long", configuration.getString("bounty-stat-immunity"));
        }
        if (!configuration.isSet("bounty-stat.all.long"))
            configuration.set("bounty-stat.all.long", "&eYour all-time bounty is &2{amount}&e.");
        if (!configuration.isSet("bounty-stat.kills.long"))
            configuration.set("bounty-stat.kills.long", "&eYou have killed &6{amount}&e players with bounties.");
        if (!configuration.isSet("bounty-stat.claimed.long"))
            configuration.set("bounty-stat.claimed.long", "&eYou have claimed &2{amount}&e from bounties.");
        if (!configuration.isSet("bounty-stat.deaths.long"))
            configuration.set("bounty-stat.deaths.long", "&eYou have died &6{amount}&e times with a bounty.");
        if (!configuration.isSet("bounty-stat.set.long"))
            configuration.set("bounty-stat.set.long", "&eYou have set &6{amount}&e successful bounties.");
        if (!configuration.isSet("bounty-stat.immunity.long"))
            configuration.set("bounty-stat.immunity.long", "&eYou have spent &2{amount}&e on immunity.");
        if (!configuration.isSet("bounty-stat.all.short"))
            configuration.set("bounty-stat.all.short", "'&6All-time bounty: &e{amount}");
        if (!configuration.isSet("bounty-stat.kills.short"))
            configuration.set("bounty-stat.kills.short", "&6Bounty kills: &e{amount}");
        if (!configuration.isSet("bounty-stat.claimed.short"))
            configuration.set("bounty-stat.claimed.short", "&6Bounty rewards: &e{amount}");
        if (!configuration.isSet("bounty-stat.deaths.short"))
            configuration.set("bounty-stat.deaths.short", "&6Bounty deaths: &e{amount}");
        if (!configuration.isSet("bounty-stat.set.short"))
            configuration.set("bounty-stat.set.short", "&6Bounties set: &e{amount}");
        if (!configuration.isSet("bounty-stat.immunity.short"))
            configuration.set("bounty-stat.immunity.short", "&6Bounty immunity: &e{amount}");

        configuration.save(language);

        // 0 prefix
        speakings.add(color(Objects.requireNonNull(configuration.getString("prefix"))));
        // 1 unknown-number
        speakings.add(color(Objects.requireNonNull(configuration.getString("unknown-number"))));
        // 2 bounty-success
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-success"))));
        // 3 unknown-player
        speakings.add(color(Objects.requireNonNull(configuration.getString("unknown-player"))));
        // 4 bounty-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-broadcast"))));
        // 5 no-permission
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-permission"))));
        // 6 broke
        speakings.add(color(Objects.requireNonNull(configuration.getString("broke"))));
        // 7 claim-bounty-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("claim-bounty-broadcast"))));
        // 8 no-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-bounty"))));
        // 9 check-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("check-bounty"))));
        // 10 list-setter
        speakings.add(color(Objects.requireNonNull(configuration.getString("list-setter"))));
        // 11 list-total
        speakings.add(color(Objects.requireNonNull(configuration.getString("list-total"))));
        // 12 offline-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("offline-bounty"))));
        // 13 bounty-item-name
        speakings.add("");
        // 14 success-remove-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("success-remove-bounty"))));
        // 15 success-edit-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("success-edit-bounty"))));
        // 16 no-setter
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-setter"))));
        // 17 repeat-command-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("repeat-command-bounty"))));
        // 18 permanent-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("permanent-immunity"))));
        // 19 scaling-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("scaling-immunity"))));
        // 20 buy-permanent-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("buy-permanent-immunity"))));
        // 21 buy-scaling-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("buy-scaling-immunity"))));
        // 22 grace-period
        speakings.add(color(Objects.requireNonNull(configuration.getString("grace-period"))));
        // 23 min-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("min-bounty"))));
        // 24 unknown-command
        speakings.add(color(Objects.requireNonNull(configuration.getString("unknown-command"))));
        // 25 already-bought-perm
        speakings.add(color(Objects.requireNonNull(configuration.getString("already-bought-perm"))));
        // 26 removed-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("removed-immunity"))));
        // 27 removed-other-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("removed-other-immunity"))));
        // 28 no-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-immunity"))));
        // 29 no-immunity-other
        speakings.add(color(Objects.requireNonNull(configuration.getString("no-immunity-other"))));
        // 30 repeat-command-immunity
        speakings.add(color(Objects.requireNonNull(configuration.getString("repeat-command-immunity"))));
        // 31 expired-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("expired-bounty"))));
        // 32 bounty-tracker-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-tracker-name"))));
        // 33 tracker-give
        speakings.add(color(Objects.requireNonNull(configuration.getString("tracker-give"))));
        // 34 tracker-receive
        speakings.add(color(Objects.requireNonNull(configuration.getString("tracker-receive"))));
        // 35 tracked-notify
        speakings.add(color(Objects.requireNonNull(configuration.getString("tracked-notify"))));
        // 36 bounty-top
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-top"))));
        // 37 bounty-top-title
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-top-title"))));
        // 38 enable-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("enable-broadcast"))));
        // 39 disable-broadcast
        speakings.add(color(Objects.requireNonNull(configuration.getString("disable-broadcast"))));
        // 40 bounty-voucher-name
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-voucher-name"))));
        // 41 redeem-voucher
        speakings.add(color(Objects.requireNonNull(configuration.getString("redeem-voucher"))));
        // 42 bounty-receiver
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-receiver"))));
        // 43 big-bounty
        speakings.add(color(Objects.requireNonNull(configuration.getString("big-bounty"))));
        // bounty-stat
        // 44 all.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.all.long"))));
        // 45 kills.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.kills.long"))));
        // 46 claimed.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.claimed.long"))));
        // 47 deaths.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.deaths.long"))));
        // 48 set.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.set.long"))));
        // 49 immunity.long
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.immunity.long"))));
        // 50 all.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.all.short"))));
        // 51 kills.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.kills.short"))));
        // 52 claimed.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.claimed.short"))));
        // 53 deaths.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.deaths.short"))));
        // 54 set.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.set.short"))));
        // 55 immunity.short
        speakings.add(color(Objects.requireNonNull(configuration.getString("bounty-stat.immunity.short"))));


        voucherLore.clear();
        for (String str : configuration.getStringList("bounty-voucher-lore")) {
            voucherLore.add(color(str));
        }
        trackerLore.clear();
        for (String str : configuration.getStringList("bounty-tracker-lore")) {
            trackerLore.add(color(str));
        }
    }


    public static String color(String str) {
        str = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', str);
        return translateHexColorCodes("&#", "", str);
    }

    public static String translateHexColorCodes(String startTag, String endTag, String message) {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
    }

    public static String parse(String str, String player, OfflinePlayer receiver) {
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        while (str.contains("{player}")) {
            str = str.replace("{player}", player);
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public static String parse(String str, OfflinePlayer receiver) {
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public static String parse(String str, double amount, OfflinePlayer receiver) {
        while (str.contains("{amount}")) {
            str = str.replace("{amount}", currencyPrefix + formatNumber(amount) + currencySuffix);
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public static String parse(String str, String player, double amount, OfflinePlayer receiver) {
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        while (str.contains("{player}")) {
            str = str.replace("{player}", player);
        }
        while (str.contains("{amount}")) {
            str = str.replace("{amount}", currencyPrefix + formatNumber(amount) + currencySuffix);
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public static String parse(String str, String player, double amount, double bounty, OfflinePlayer receiver) {
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        while (str.contains("{player}")) {
            str = str.replace("{player}", player);
        }
        while (str.contains("{amount}")) {
            str = str.replace("{amount}", currencyPrefix + formatNumber(amount) + currencySuffix);
        }
        while (str.contains("{bounty}")) {
            str = str.replace("{bounty}", currencyPrefix + formatNumber(bounty) + currencySuffix);
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }


    public static String parse(String str, String sender, String player, double amount, OfflinePlayer receiver) {
        while (str.contains("{player}")) {
            str = str.replace("{player}", sender);
        }
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        while (str.contains("{amount}")) {
            str = str.replace("{amount}", currencyPrefix + formatNumber(amount) + currencySuffix);
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    public static String parse(String str, String sender, String player, OfflinePlayer receiver) {
        while (str.contains("{player}")) {
            str = str.replace("{player}", sender);
        }
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }


    public static String parse(String str, String sender, String player, double amount, double totalBounty, OfflinePlayer receiver) {
        while (str.contains("{player}")) {
            str = str.replace("{player}", sender);
        }
        while (str.contains("{receiver}")) {
            str = str.replace("{receiver}", player);
        }
        while (str.contains("{amount}")) {
            str = str.replace("{amount}", currencyPrefix + formatNumber(amount) + currencySuffix);
        }
        while (str.contains("{bounty}")) {
            str = str.replace("{bounty}", currencyPrefix + formatNumber(totalBounty) + currencySuffix);
        }
        if (papiEnabled && receiver != null) {
            return PlaceholderAPI.setPlaceholders(receiver, str);
        }
        return str;
    }

    /**
     * Get the balance of a player. Checks if the currency is a placeholder and parses it, otherwise, gets the amount of items matching the currency material
     *
     * @param player Player to get balance from
     * @return Balance of player
     */
    public static double getBalance(Player player) {
        if (usingPapi) {
            // check if papi is enabled - parse to check
            if (papiEnabled) {
                double balance;
                try {
                    balance = Double.parseDouble(PlaceholderAPI.setPlaceholders(player, currency));
                } catch (NumberFormatException ignored) {
                    Bukkit.getLogger().warning("Error getting a number from currency placeholder!");
                    return 0;
                }
                return balance;
            } else {
                Bukkit.getLogger().warning("Currency for bounties currently set as placeholder but PlaceholderAPI is not enabled!");
            }
        } else {
            return checkAmount(player, Material.valueOf(currency));
        }
        return 0;
    }

    /**
     * Check the amount of items matching a material
     *
     * @param player   Player whose inventory will be searched
     * @param material Material to check for
     * @return amount of items in the players inventory that are a certain material
     */
    public static int checkAmount(Player player, Material material) {
        int amount = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack content : contents) {
            if (content != null) {
                if (content.getType().equals(material)) {
                    amount += content.getAmount();
                }
            }
        }
        return amount;
    }


    public static String formatNumber(String number){
        if (number.length() == 0)
            return "";
        if (number.startsWith(currencyPrefix) && currencyPrefix.length() > 0)
            return currencyPrefix + formatNumber(number.substring(currencyPrefix.length()));
        if (number.endsWith(currencySuffix) && currencySuffix.length() > 0)
            return formatNumber(number.substring(0, number.length() - currencySuffix.length())) + currencySuffix;
        if (isNumber(number))
            return formatNumber(Double.parseDouble(number));
        if (!isNumber(number.substring(0,1))){
            // first digit isn't a number
            return number.charAt(0) + formatNumber(number.substring(1));
        }
        return formatNumber(number.substring(0, number.length()-1)) + number.charAt(number.length()-1);

    }

    public static double findFirstNumber(String str){
        if (str.length() == 0)
            return 0;
        if (isNumber(str))
            return Double.parseDouble(str);
        if (isNumber(str.substring(0,1)))
            return findFirstNumber(str.substring(0, str.length()-1));
        return findFirstNumber(str.substring(1));
    }

    public static boolean isNumber(String str){
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException e){
            return false;
        }
        return true;
    }
    /**
     * Format a number with number formatting options in the config
     *
     * @param number Number to be formatted
     * @return formatted number
     */
    public static String formatNumber(Double number){
        if (numberFormatting == 2){
            // set divisions
            return setDivision(number);
        }
        String strNum = decimalFormat.format(number);
        if (decimals == 0)
            if (strNum.contains(decimalSymbol + ""))
                strNum = strNum.substring(0, strNum.indexOf(decimalSymbol));
        return removeUnnecessaryZeros(strNum);
    }

    public static String setDivision(Double number){
        for (Map.Entry<Long, String> entry : nfDivisions.entrySet()){
            if (number / entry.getKey() >= 1){
                String strCost = divisionFormat.format((double) number / entry.getKey());
                if (nfDecimals == 0) {
                    if (strCost.contains(decimalSymbol + ""))
                        strCost = strCost.substring(0, strCost.indexOf(decimalSymbol));
                }
                return removeUnnecessaryZeros(strCost) + entry.getValue();
            }
        }
        return removeUnnecessaryZeros(decimalFormat.format(number));
    }

    public static String removeUnnecessaryZeros(String value){
        if (value.isEmpty())
            return "";
        while (value.contains(Character.toString(decimalSymbol)) && (value.charAt(value.length()-1) == '0' || value.charAt(value.length()-1) == decimalSymbol))
            value = value.substring(0, value.length()-1);
        return value;
    }


    public static LinkedHashMap<Long, String> sortByValue(Map<Long, String> hm) {
        // Create a list from elements of HashMap
        List<Map.Entry<Long, String>> list =
                new LinkedList<>(hm.entrySet());

        // Sort the list
        list.sort((o1, o2) -> (o2.getKey()).compareTo(o1.getKey()));

        // put data from sorted list to hashmap
        LinkedHashMap<Long, String> temp = new LinkedHashMap<>();
        for (Map.Entry<Long, String> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }
        return temp;
    }

}
