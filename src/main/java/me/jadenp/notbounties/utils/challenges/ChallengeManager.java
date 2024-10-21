package me.jadenp.notbounties.utils.challenges;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.ui.gui.bedrock.GUIComponent;
import me.jadenp.notbounties.utils.DataManager;
import me.jadenp.notbounties.utils.configuration.ConfigOptions;
import me.jadenp.notbounties.utils.configuration.Immunity;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.autoBounties.MurderBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.RandomBounties;
import me.jadenp.notbounties.utils.configuration.autoBounties.TimedBounties;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * How to add a built-in challenge:
 * 1. Add challenge type to ChallengeType.java
 * 2. Add challenge to challenges.yml
 * 3. Add default case in Challenge.java
 * 3a. (Optional) Modify generateProgress() and getStartAmount() methods if the challenge isn't linear progression
 * 4. Use the ChallengeManager.updateProgress() method somewhere to update the progress
 * 4a. (Optional) Modify the ChallengeManager.updateProgress() method if your progress change shouldn't directly correlate to an increase of the challenge value
 */

public class ChallengeManager implements Listener {
    private static final Map<UUID, LinkedList<ChallengeData>> challengeDataMap = new HashMap<>(); // there will always be entries for players in this map
    private static final Map<UUID, LinkedList<ActiveChallenge>> activeChallenges = new HashMap<>(); // depending on the mode, only the console uuid may have an entry in this map
    private static final List<Challenge> allChallenges = new ArrayList<>(); // all challenges in the challenges.yml file

    private static long nextChallengeChange = 0; // the time of the next challenge - this should be set from the bounties.yml file when it is read from

    private static boolean enabled = false; // whether the challenges feature is enabled or not
    private static double timeLimitDays = -1; // the amount of time players have to complete these challenges before they expire
    private static int concurrentChallenges; // number of challenges that can be active
    private static boolean globalChallenges; // whether everyone gets the same challenges or not

    private static final Random random = new Random(System.currentTimeMillis());

    private ChallengeManager(){}

    public static void reloadOptions() {
        File challengeFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "challenges/challenges.yml");
        if (!challengeFile.exists()) {
            NotBounties.getInstance().saveResource("challenges/challenges.yml", false);
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(challengeFile);
        // load configuration options
        enabled = configuration.isSet("enabled") && configuration.getBoolean("enabled"); // default false
        if (!enabled)
            return;
        concurrentChallenges = configuration.isSet("concurrent-challenges") ? configuration.getInt("concurrent-challenges") : 3;
        globalChallenges = !configuration.isSet("global-challenges") || configuration.getBoolean("global-challenges"); // default true

        double prevTimeLimit = timeLimitDays;
        timeLimitDays = configuration.isSet("time-limit-days") ? configuration.getDouble("time-limit-days") : 7;
        if (prevTimeLimit != -1 && prevTimeLimit != timeLimitDays) {
            // adjust nextChallengeChange
            nextChallengeChange+= (long) ((timeLimitDays - prevTimeLimit) * 24 * 60 * 60 * 1000L);
        }

        if (!configuration.isConfigurationSection("challenge-types"))
            return;
        // load saved challenges
        allChallenges.clear();
        for (String key : Objects.requireNonNull(configuration.getConfigurationSection("challenge-types")).getKeys(false)) {
            // safety check if the value is a configuration section
            if (!configuration.isConfigurationSection("challenge-types." + key))
                return;
            try {
                // check for custom challenge section
                if (key.equals("custom-challenges")) {
                    if (ConfigOptions.papiEnabled) {
                        // custom challenges have a deeper configuration - configuration guaranteed to be not null at this point
                        for (String customKey : Objects.requireNonNull(configuration.getConfigurationSection("challenge-types.custom-challenges")).getKeys(false)) {
                            // make sure it is a configuration section
                            if (!configuration.isConfigurationSection("challenge-types.custom-challenges." + customKey))
                                return;
                            Challenge challenge = new Challenge(ChallengeType.CUSTOM, Objects.requireNonNull(configuration.getConfigurationSection("challenge-types.custom-challenges." + customKey)));
                            allChallenges.add(challenge);
                        }
                    } else {
                        NotBounties.debugMessage("Custom challenges are set in the config, but PlaceholderAPI is not enabled!", true);
                    }
                } else {
                    // built-in challenges
                    ChallengeType challengeType = ChallengeType.convertFromConfiguration(key); // get challenge type
                    Challenge challenge = new Challenge(challengeType, Objects.requireNonNull(configuration.getConfigurationSection("challenge-types." + key)));
                    allChallenges.add(challenge);
                }
            } catch (IllegalArgumentException ignored) {
                // Not a valid challenge type
                Bukkit.getLogger().warning("[NotBounties] Invalid challenge type: " + key);
            }
        }
    }

    /**
     * Reads the challenge data from the saved file. This should only be used when the plugin starts.
     * @throws IOException If there is a problem reading the challenge-data.yml file
     */
    public static void readChallengeData() throws IOException {
        // load in challenge data
        File challengeDataFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "challenges/challenge-data.yml");
        if (!challengeDataFile.exists()) {
            challengeDataFile.mkdirs();
            challengeDataFile.createNewFile();
            Bukkit.getLogger().info("[NotBounties] Created new challenge data file.");
            return;
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(challengeDataFile);
        // check if a console uuid exists - all challenges are the same for everyone
        String consoleUUID = DataManager.GLOBAL_SERVER_ID.toString();

        // iterate through every top level key
        for (String key : configuration.getKeys(false)) {
            UUID uuid;
            // the key should be a UUID
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[NotBounties] Could not convert string to uuid in challenge-data.yml: " + key);
                continue;
            }
            // should be configuration section
            if (!configuration.isConfigurationSection(key))
                continue;
            LinkedList<ChallengeData> challengeDataList = new LinkedList<>();
            LinkedList<ActiveChallenge> activeChallengeList = new LinkedList<>();
            for (String challengeIndexString : Objects.requireNonNull(configuration.getConfigurationSection(key)).getKeys(false)) {
                int challengeIndex;
                try {
                    challengeIndex = Integer.parseInt(challengeIndexString);
                } catch (NumberFormatException e) {
                    Bukkit.getLogger().warning("[NotBounties] Could not get challenge index for UUID: " + key);
                    continue;
                }
                // check for a valid challenge
                if (challengeIndex >= allChallenges.size()) {
                    Bukkit.getLogger().warning(() -> "[NotBounties] Saved challenge no longer exists! (" + challengeIndex + ")");
                    continue;
                }
                Challenge challenge =  allChallenges.get(challengeIndex);

                // variation
                if (configuration.isSet(key + "." + challengeIndexString + ".variation")) {
                    int variationIndex = configuration.getInt(key + "." + challengeIndexString + ".variation");
                    // check for a valid variation
                    if (variationIndex >= challenge.getVariations().size()) {
                        int finalVariationIndex = variationIndex;
                        Bukkit.getLogger().warning(() -> "[NotBounties] Saved challenge variation no longer exists! (" + challengeIndex + ":" + finalVariationIndex + ")");
                        variationIndex = 0;
                    }
                    activeChallengeList.add(new ActiveChallenge(challenge, variationIndex));
                }

                // console won't have anything else
                if (key.equals(consoleUUID))
                    continue;

                // player specific data
                if (configuration.isSet(key + "." + challengeIndexString + ".progress")) {
                    ChallengeData challengeData = new ChallengeData(configuration.getDouble(key + "." + challengeIndexString + ".progress"), configuration.getDouble(key + "." + challengeIndexString + ".goal"), configuration.getBoolean(key + "." + challengeIndexString + ".rewarded"));
                    if (challengeData.isRewarded())
                        challengeData.setNotified(true);
                    challengeDataList.add(challengeData);
                }

            }

            // activeChallengeList will be empty if global challenges is enabled and the current uuid is of a player
            if (!activeChallengeList.isEmpty()) {
                activeChallenges.put(uuid, activeChallengeList);
            }
            // challengeDataList will be empty if the uuid is from the console
            if (!challengeDataList.isEmpty()) {
                challengeDataMap.put(uuid, challengeDataList);
            }
        }
    }

    /**
     * Returns the index of the challenge in the all challenges list
     * @param challenge Challenge to find the index of
     * @return The index of the challenge in allChallenges or -1 if it doesn't contain this challenge
     */
    private static int getChallengeIndex(Challenge challenge) {
        for (int i = 0; i < allChallenges.size(); i++) {
            Challenge currentChallenge = allChallenges.get(i);
            if (challenge.getChallengeType() == currentChallenge.getChallengeType()) {
                if (challenge.getChallengeType() == ChallengeType.CUSTOM) {
                    // check if requirement matches
                    if (challenge.getRequirement().equals(currentChallenge.getRequirement())) {
                        return i;
                    }
                } else {
                    return i;
                }
            }
        }
        return -1;
    }

    public static void saveChallengeData() throws IOException {
        // make sure the file exists
        File challengeDataFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "challenges/challenge-data.yml");
        if (challengeDataFile.createNewFile()) {
            Bukkit.getLogger().info("[NotBounties] Created new challenge data file.");
        }
        YamlConfiguration configuration = new YamlConfiguration();
        UUID consoleUUID = DataManager.GLOBAL_SERVER_ID;
        if (globalChallenges) {
            // active challenges are only stored for the console uuid
            if (!activeChallenges.containsKey(consoleUUID))
                // no active challenges
                return;
            LinkedList<ActiveChallenge> challenges = activeChallenges.get(consoleUUID);
            for (int i = 0; i < challenges.size(); i++) {
                Challenge challenge = challenges.get(i).getChallenge();
                // get challenge index
                int challengeIndex = getChallengeIndex(challenge);
                if (challengeIndex == -1) {
                    // debug only because this may happen when a challenge is removed while the server is running
                    NotBounties.debugMessage("Error getting index of challenge: " + challenge.getChallengeType(), false);
                    continue;
                }
                // get variation index - already stored as indices in the list
                int variationIndex = challenges.get(i).getVariationIndex();
                if (challenge.getVariations().size() <= variationIndex) {
                    // debug only because this may happen when a variation is removed while the server is running
                    NotBounties.debugMessage("Error getting variation: " + challenge.getChallengeType() + ":" + i, true);
                    variationIndex = 0;
                }
                // save variation to configuration
                configuration.set(consoleUUID + "." + challengeIndex + ".variation", variationIndex);
                // get player progress and goal for this challenge
                for (Map.Entry<UUID, LinkedList<ChallengeData>> entry : challengeDataMap.entrySet()) {
                    ChallengeData challengeData = entry.getValue().get(i);
                    configuration.set(entry.getKey() + "." + challengeIndex + ".progress", challengeData.getProgress());
                    configuration.set(entry.getKey() + "." + challengeIndex + ".goal", challengeData.getGoal());
                    configuration.set(entry.getKey() + "." + challengeIndex + ".rewarded", challengeData.isRewarded());
                }

            }
        } else {
            // active challenges are different for everyone
            for (Map.Entry<UUID, LinkedList<ActiveChallenge>> entry : activeChallenges.entrySet()) {
                // these should both be valid and of the same size
                LinkedList<ActiveChallenge> activeChallengeList = entry.getValue();
                LinkedList<ChallengeData> challengeDataList = challengeDataMap.get(entry.getKey());
                for (int i = 0; i < activeChallengeList.size(); i++) {
                    Challenge challenge = activeChallengeList.get(i).getChallenge();
                    int challengeIndex = getChallengeIndex(challenge);
                    if (challengeIndex == -1) {
                        // debug only because this may happen when a challenge is removed while the server is running
                        NotBounties.debugMessage("Error getting index of challenge: " + challenge.getChallengeType(), true);
                        continue;
                    }

                    // get variation index - already stored as indices in the list
                    int variationIndex = activeChallengeList.get(i).getVariationIndex();
                    if (challenge.getVariations().size() <= variationIndex) {
                        // debug only because this may happen when a variation is removed while the server is running
                        NotBounties.debugMessage("Error getting variation: " + challenge.getChallengeType() + ":" + i, true);
                        variationIndex = 0;
                    }

                    // save variation to configuration
                    configuration.set(entry.getKey() + "." + challengeIndex + ".variation", variationIndex);
                    configuration.set(entry.getKey() + "." + challengeIndex + ".progress", challengeDataList.get(i).getProgress());
                    configuration.set(entry.getKey() + "." + challengeIndex + ".goal", challengeDataList.get(i).getGoal());
                    configuration.set(entry.getKey() + "." + challengeIndex + ".rewarded", challengeDataList.get(i).isRewarded());
                }

            }
        }

        configuration.save(challengeDataFile);

    }


    public static void login(Player player) {
        if (!enabled)
            return;
        // check if progress needs to be generated for this player
        if (!challengeDataMap.containsKey(player.getUniqueId()))
            generateProgress(player);
        else {
            // check for challenge notification
            List<ChallengeData> challengeData = challengeDataMap.get(player.getUniqueId());
            for (int i = 0; i < challengeData.size(); i++) {
                if (challengeData.get(i).getProgress() >= challengeData.get(i).getGoal() && !challengeData.get(i).isNotified()) {
                    notifyCompletion(player.getUniqueId(), i);
                }
            }
        }
    }

    /**
     * Generate progress data for the player. This is important to start a challenge.
     * @param player Player to generate progress for
     */
    private static void generateProgress(Player player) {
        if (!globalChallenges) {
            generateChallenges(player.getUniqueId()); // generate new challenges
        } else if (!activeChallenges.containsKey(DataManager.GLOBAL_SERVER_ID)) {
            generateChallenges(DataManager.GLOBAL_SERVER_ID); // generate challenges for the console
        }
        // create progress and goal list
        LinkedList<ChallengeData> challengeDataList = new LinkedList<>();

        // get created challenges
        LinkedList<ActiveChallenge> createdChallenges = globalChallenges ? activeChallenges.get(DataManager.GLOBAL_SERVER_ID) : activeChallenges.get(player.getUniqueId());
        // get the progress and goal for each challenge
        for (ActiveChallenge createdChallenge : createdChallenges) {
            Challenge challenge = createdChallenge.getChallenge();
            double progressValue = challenge.getDefaultProgress(player); // get default progress value
            // add default value and goal (default value + variation amount)
            double goal;
            if (challenge.getChallengeType() == ChallengeType.BOUNTY_MULTIPLE) {
                goal = progressValue * challenge.getVariations().get(createdChallenge.getVariationIndex());
            } else {
                goal = progressValue + challenge.getVariations().get(createdChallenge.getVariationIndex());
            }
            challengeDataList.add(new ChallengeData(progressValue, goal, false));
        }
        challengeDataMap.put(player.getUniqueId(), challengeDataList);
    }

    /**
     * Update any active challenges for the player that are of the specified challenge type
     * @param uuid UUID of the player to update progress for
     * @param challengeType Type of challenge that progress is being updated
     */
    public static void updateChallengeProgress(UUID uuid, ChallengeType challengeType, double progressChange) {
        if (!enabled)
            return;
        List<ActiveChallenge> playerChallenges = getActiveChallenges(uuid);
        for (int i = 0; i < playerChallenges.size(); i++) {
            ActiveChallenge challenge = playerChallenges.get(i);
            if (challenge.getChallenge().getChallengeType().equals(challengeType)) {
                ChallengeData challengeData = getChallengeData(uuid, i);
                if (challengeType == ChallengeType.CLOSE_BOUNTY && progressChange < challenge.getChallenge().getVariations().get(challenge.getVariationIndex())) {
                    challengeData.addProgress(1);
                } else {
                    challengeData.addProgress(progressChange);
                }
                // check completion
                if (challengeData.getProgress() >= challengeData.getGoal() && !challengeData.isNotified())
                    notifyCompletion(uuid, i);
            }
        }

    }

    /**
     * Notify an online player that they have completed a challenge. If the player is offline, no notification will be sent.
     * @param uuid UUID of the player.
     * @param challengeIndex Index of the challenge they completed.
     */
    private static void notifyCompletion(UUID uuid, int challengeIndex) {
        ChallengeData challengeData = getChallengeData(uuid, challengeIndex);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // player is online
            challengeData.setNotified(true);
            player.playSound(player, Sound.ENTITY_ARROW_HIT_PLAYER, 1, 1);
            player.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("challenge-completion").replace("{challenge}", getChallengeTitle(player, challengeIndex)), player));
        }
    }

    /**
     * Manually override the next challenge change
     * @param nextChallengeChange The new time that the challenges should change next in milliseconds
     */
    public static void setNextChallengeChange(long nextChallengeChange) {
        // When challenge change is set to 0, the challenges will not be changed
        ChallengeManager.nextChallengeChange = nextChallengeChange == 0L ? 1 : nextChallengeChange;
    }

    public static long getNextChallengeChange() {
        return nextChallengeChange;
    }

    /**
     * Check if the challenges need to be changed and change them if need be.
     * Also update challenge progress for custom challenges.
     */
    public static void checkChallengeChange() {
        if (!enabled)
            return;
        updateCustomChallenges();
        if (System.currentTimeMillis() < nextChallengeChange || nextChallengeChange == 0)
            return;

        nextChallengeChange = System.currentTimeMillis() + (long) (timeLimitDays * 24 * 60 * 60 * 1000);
        changeChallenges();
    }

    /**
     * Updates custom challenge progression for online players
     */
    private static void updateCustomChallenges() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            List<ActiveChallenge> activeChallengeList = ChallengeManager.getActiveChallenges(player.getUniqueId());
            if (!challengeDataMap.containsKey(player.getUniqueId()))
                generateProgress(player);
            List<ChallengeData> challengeDataList = challengeDataMap.get(player.getUniqueId());
            for (int i = 0; i < activeChallengeList.size(); i++) {
                ActiveChallenge activeChallenge = activeChallengeList.get(i);
                ChallengeData challengeData = challengeDataList.get(i);
                if (activeChallenge.getChallenge().getChallengeType() == ChallengeType.CUSTOM && !challengeData.isNotified()) {
                    // update progress
                    double currentValue = activeChallenge.getChallenge().getCustomProgress(player);
                    challengeData.addProgress(currentValue -  challengeData.getProgress());

                    // check for completion
                    if (activeChallenge.getChallenge().isCustomRequirementCompleted(player, challengeData.getGoal())) {
                        // completed challenge - notify
                        notifyCompletion(player.getUniqueId(), i);
                    }
                }
            }
        }
    }

    /**
     * Change the currently active challenges to random new ones
     */
    private static void changeChallenges() {
        activeChallenges.clear();
        challengeDataMap.clear();
        if (globalChallenges) {
            generateChallenges(DataManager.GLOBAL_SERVER_ID);
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                generateChallenges(player.getUniqueId());
            }
        }
    }

    /**
     * Get the active challenges for a player
     * @param uuid UUID of the player
     * @return A LinkedList of the player's active challenges
     */
    public static List<ActiveChallenge> getActiveChallenges(UUID uuid) {
        if (enabled) {
            if (globalChallenges) {
                if (activeChallenges.containsKey(new UUID(0, 0)))
                    return activeChallenges.get(new UUID(0, 0));
                generateChallenges(new UUID(0, 0)); // generate challenges if no active global challenge exists
            } else {
                if (activeChallenges.containsKey(uuid))
                    return activeChallenges.get(uuid);
                generateChallenges(uuid); // generate challenges if no active challenge exists for this player
            }
            return getActiveChallenges(uuid);
        }
        return new ArrayList<>();
    }

    /**
     * Generates concurrentChallenges number of challenges for the uuid, and stores them in the activeChallenges and variation array
     * @param uuid UUID of the player to generate the challenges for
     */
    private static void generateChallenges(UUID uuid) {
        LinkedList<ActiveChallenge> activeChallengeList = new LinkedList<>();
        List<Challenge> createdChallenges = new ArrayList<>();
        int uniqueChallengesEnabled = getUniqueChallengesEnabled();
        for (int i = 0; i < concurrentChallenges; i++) {
            Challenge challenge; // get random challenge
            // make sure the challenge is different from the others
            do {
                challenge = getRandomChallenge();
            } while (createdChallenges.size() < uniqueChallengesEnabled && createdChallenges.contains(challenge) || !isChallengeEnabled(challenge));
            createdChallenges.add(challenge);
            int randomVariationIndex = random.nextInt(challenge.getVariations().size()); // get random variation
            // add to list
            activeChallengeList.add(new ActiveChallenge(challenge, randomVariationIndex));
        }
        activeChallenges.put(uuid, activeChallengeList);
    }

    private static boolean isChallengeEnabled(Challenge challenge) {
        switch (challenge.getChallengeType()) {
            case PURCHASE_IMMUNITY -> {
                return Immunity.immunityType == Immunity.ImmunityType.TIME || Immunity.immunityType == Immunity.ImmunityType.SCALING;
            }
            case HOLD_POSTER -> {
                return ConfigOptions.craftPoster;
            }
            case WHITELISTED_BOUNTY_SET -> {
                return ConfigOptions.bountyWhitelistEnabled;
            }
            case AUTO_BOUNTY -> {
                return MurderBounties.isEnabled() || RandomBounties.isEnabled() || TimedBounties.isEnabled();
            }
            case BUY_OWN -> {
                return ConfigOptions.buyBack;
            }
            default -> {
                return true;
            }
        }
    }

    private static int getUniqueChallengesEnabled() {
        int enabled = 0;
        for (Challenge challenge : allChallenges) {
            if (isChallengeEnabled(challenge))
                enabled++;
        }
        return enabled;
    }

    private static Challenge getRandomChallenge() {
        int randIndex = random.nextInt(allChallenges.size());
        return allChallenges.get(randIndex);
    }

    /**
     * Get a string that displays how much time is left before challenges reset
     * @return A formatted string in relative time.
     */
    public static String getTimeLeft() {
        if (!enabled)
            return "";
        long timeLeft = nextChallengeChange - System.currentTimeMillis();
        if (timeLeft < 0)
            return "0s";
        return LocalTime.formatTime(timeLeft, LocalTime.TimeFormat.RELATIVE);
    }

    /**
     * Get the current active variation for a player and a challenge
     * @param uuid UUID of the player
     * @param challengeIndex Index of the challenge
     * @return The current active variation chosen from the list of variations in the challenges.yml file
     */
    private static double getVariation(UUID uuid, int challengeIndex) {
        if (globalChallenges) {
            // return the active challenge variation for console uuid
            return activeChallenges.get(DataManager.GLOBAL_SERVER_ID).get(challengeIndex).getChallenge().getVariations().get(activeChallenges.get(DataManager.GLOBAL_SERVER_ID).get(challengeIndex).getVariationIndex());
        } else {
            if (!activeChallenges.containsKey(uuid))
                return 0;
            // return the active challenge variation for uuid argument
            return activeChallenges.get(uuid).get(challengeIndex).getChallenge().getVariations().get(activeChallenges.get(uuid).get(challengeIndex).getVariationIndex());
        }
    }

    /**
     * Get the progress of the player as if they started with a value of 0
     * @param uuid UUID of the player
     * @param challengeIndex Index of the challenge
     * @return The progress - the start amount
     */
    private static double getAdjustedProgress(UUID uuid, int challengeIndex) {
        if (!challengeDataMap.containsKey(uuid))
            return 0;
        ChallengeData challengeData = challengeDataMap.get(uuid).get(challengeIndex);
        return challengeData.getProgress() - getStartAmount (uuid, challengeData.getGoal(), challengeIndex);
    }

    /**
     * Get the title for a certain challenge parsed for a player
     * @param player Player to parse the challenge for
     * @param challengeIndex Index of the challenge
     * @return A formatted string.
     */
    public static String getChallengeTitle(OfflinePlayer player, int challengeIndex) {
        if (!enabled)
            return "";
        if (challengeIndex >= concurrentChallenges)
            return ""; // out of bounds

        ActiveChallenge activeChallenge = getActiveChallenges(player.getUniqueId()).get(challengeIndex);
        Challenge challenge = activeChallenge.getChallenge();
        int variationIndex = activeChallenge.getVariationIndex();

        ChallengeData challengeData = getChallengeData(player.getUniqueId(), challengeIndex);
        return challenge.getChallengeTitle(player, getStartAmount(player.getUniqueId(), challengeData.getGoal(), challengeIndex), challengeData.getProgress(), challengeData.getGoal(), variationIndex);
    }

    /**
     * Get the value that the player started with when the challenge began.
     * @param uuid UUID of the player.
     * @param goal The player's end challenge goal.
     * @return The player's current variation subtracted from their goal
     */
    private static double getStartAmount(UUID uuid, double goal, int challengeIndex) {
        ActiveChallenge activeChallenge = getActiveChallenges(uuid).get(challengeIndex);
        double variation = activeChallenge.getChallenge().getVariations().get(activeChallenge.getVariationIndex());
        if (activeChallenge.getChallenge().getChallengeType() == ChallengeType.BOUNTY_MULTIPLE) {
            return goal / variation;
        }
        return goal - variation;
    }

    /**
     * Get the challenge data for a player and a challenge.
     * Returns challenge data with 0 progress and a 0 goal if the uuid is not recorded.
     * @param uuid UUID of the player
     * @param challengeIndex Index of the challenge
     * @return The corresponding challenge data
     */
    private static ChallengeData getChallengeData(UUID uuid, int challengeIndex) {
        if (!challengeDataMap.containsKey(uuid))
            return new ChallengeData(0, 0, true);
        return challengeDataMap.get(uuid).get(challengeIndex);
    }

    /**
     *
     * @return If challenges are enabled
     */
    public static boolean isEnabled() {
        return enabled;
    }

    /**
     *
     * @return The maximum number of active challenges that can be open at the same time.
     */
    public static int getConcurrentChallenges() {
        return concurrentChallenges;
    }

    /**
     * Gets an array of display items for all the player's active challenges
     * @param player Player to get active challenges for
     * @return A list of ItemStacks representing each challenge with length of concurrentChallenges
     */
    public static ItemStack[] getDisplayItems(Player player) {
        ItemStack[] items = new ItemStack[concurrentChallenges];
        List<ActiveChallenge> activeChallengeList = ChallengeManager.getActiveChallenges(player.getUniqueId());
        if (!challengeDataMap.containsKey(player.getUniqueId()))
            generateProgress(player);
        List<ChallengeData> challengeDataList = challengeDataMap.get(player.getUniqueId());
        for (int i = 0; i < concurrentChallenges; i++) {
            ActiveChallenge activeChallenge = activeChallengeList.get(i);
            ChallengeData challengeData = challengeDataList.get(i);
            items[i] = activeChallenge.getChallenge().getItem(player, getStartAmount(player.getUniqueId(), challengeData.getGoal(), i), challengeData.getProgress(), challengeData.getGoal(), activeChallenge.getVariationIndex(), !challengeData.isRewarded() && challengeData.isNotified());
        }
        return items;
    }

    /**
     * Get the display text for a challenge
     * @param player Player to parse the challenge for
     * @param challengeIndex Index of the challenge in activeChallenges
     * @return A list of text representing a challenge for a player
     */
    public static List<String> getDisplayText(Player player, int challengeIndex) {
        ChallengeData challengeData = getChallengeData(player.getUniqueId(), challengeIndex);
        ActiveChallenge activeChallenge = getActiveChallenges(player.getUniqueId()).get(challengeIndex);
        return activeChallenge.getChallenge().getDisplayText(player, getStartAmount(player.getUniqueId(), challengeData.getGoal(), challengeIndex), challengeData.getProgress(), challengeData.getGoal(), activeChallenge.getVariationIndex(), !challengeData.isRewarded() && challengeData.isNotified());
    }

    /**
     * Gets an array of built display components for all the player's active challenges
     * @param player Player to get active challenges for
     * @return A list of GUIComponents representing each challenge with length of concurrentChallenges
     */
    public static GUIComponent[] getDisplayComponents(Player player) {
        GUIComponent[] items = new GUIComponent[concurrentChallenges];
        List<ActiveChallenge> activeChallengeList = ChallengeManager.getActiveChallenges(player.getUniqueId());
        if (!challengeDataMap.containsKey(player.getUniqueId()))
            generateProgress(player);
        List<ChallengeData> challengeDataList = challengeDataMap.get(player.getUniqueId());
        for (int i = 0; i < concurrentChallenges; i++) {
            ActiveChallenge activeChallenge = activeChallengeList.get(i);
            ChallengeData challengeData = challengeDataList.get(i);
            items[i] = activeChallenge.getChallenge().getComponent(player, getStartAmount(player.getUniqueId(), challengeData.getGoal(), i), challengeData.getProgress(), challengeData.getGoal(), activeChallenge.getVariationIndex(), !challengeData.isRewarded() && challengeData.isNotified());
            items[i].addCommands(List.of("[p] " + ConfigOptions.pluginBountyCommands.get(0) + " challenges claim " + (i+1)));
        }
        return items;
    }

    /**
     * Try to claim a challenge reward
     * @param player Player to try to claim the challenge reward for
     * @param challengeIndex Index of the challenge to claim
     * @return True if the challenge reward was claimed successfully, or false if the player can't claim this challenge reward
     */
    public static boolean tryClaim(Player player, int challengeIndex) {
        ChallengeData challengeData = getChallengeData(player.getUniqueId(), challengeIndex);
        if (challengeData.getProgress() >= challengeData.getGoal() && !challengeData.isRewarded()) {
            ActiveChallenge activeChallenge = getActiveChallenges(player.getUniqueId()).get(challengeIndex);
            activeChallenge.getChallenge().executeCommands(player, getStartAmount(player.getUniqueId(), challengeData.getGoal(), challengeIndex), challengeData.getProgress(), challengeData.getGoal(), activeChallenge.getVariationIndex());
            challengeData.setRewarded(true);
            return true;
        } else {
            player.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("challenge-claim-deny"), player));
            player.playSound(player.getEyeLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
            return false;
        }
    }

    public static boolean canClaim(Player player, int challengeIndex) {
        ChallengeData challengeData = getChallengeData(player.getUniqueId(), challengeIndex);
        return challengeData.getProgress() >= challengeData.getGoal() && !challengeData.isRewarded();
    }
}
