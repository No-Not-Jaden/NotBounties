package me.jadenp.notbounties.utils.challenges;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class ChallengeManager implements Listener {
    private static Map<UUID, LinkedList<ChallengeData>> challengeDataMap = new HashMap<>();
    private static Map<UUID, LinkedList<ActiveChallenge>> activeChallenges = new HashMap<>();
    private static List<Challenge> allChallenges = new ArrayList<>(); // all challenges in the challenges.yml file

    private static long nextChallengeChange = 0; // the time of the next challenge - this should be set from the bounties.yml file when it is read from

    private static boolean enabled = false; // whether the challenges feature is enabled or not
    private static double timeLimitDays; // the amount of time players have to complete these challenges before they expire
    private static int concurrentChallenges; // number of challenges that can be active
    private static boolean globalChallenges; // whether everyone gets the same challenges or not
    private static String guiClaim;
    private static String chatClaim;

    public static void reloadOptions() {
        File challengeFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "challenges.yml");
        if (!challengeFile.exists()) {
            NotBounties.getInstance().saveResource("challenges.yml", false);
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(challengeFile);
        // load configuration options
        enabled = configuration.isSet("enabled") && configuration.getBoolean("enabled"); // default false
        timeLimitDays = configuration.isSet("time-limit-days") ? configuration.getDouble("time-limit-days") : 7;
        concurrentChallenges = configuration.isSet("concurrent-challenges") ? configuration.getInt("concurrent-challenges") : 3;
        globalChallenges = !configuration.isSet("global-challenges") || configuration.getBoolean("global-challenges"); // default true
        guiClaim = configuration.isSet("gui-cliam") ? configuration.getString("gui-claim") : "&5&lClick to claim your reward";
        chatClaim = configuration.isSet("chat-claim") ? configuration.getString("chat-claim") : "&5&lDo &d/bounty challenges claim &5&lto claim your reward.";

        if (!configuration.isConfigurationSection("challenge-types"))
            return;
        // load saved challenges
        allChallenges.clear();
        for (String key : Objects.requireNonNull(configuration.getConfigurationSection("challenge-types")).getKeys(false)) {
            // safety check if the value is a configuration section
            if (!configuration.isConfigurationSection("challenge-types." + key))
                return;
            try {
                // get challenge type
                ChallengeType challengeType = ChallengeType.convertFromConfiguration(key);
                if (challengeType == ChallengeType.CUSTOM) {
                    // custom challenges have a deeper configuration - configuration guaranteed to be not null at this point
                    for (String customKey : Objects.requireNonNull(configuration.getConfigurationSection("challenge-types.custom")).getKeys(false)) {
                        // make sure it is a configuration section
                        if (!configuration.isConfigurationSection("challenge-types.custom." + customKey))
                            return;
                        Challenge challenge = new Challenge(challengeType, Objects.requireNonNull(configuration.getConfigurationSection("challenge-types.custom." + customKey)));
                        allChallenges.add(challenge);
                    }
                } else {
                    // built-in challenges
                    Challenge challenge = new Challenge(challengeType, Objects.requireNonNull(configuration.getConfigurationSection("challenge-types." + key)));
                    allChallenges.add(challenge);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }

    /**
     * Reads the challenge data from the saved file. This should only be used when the plugin starts.
     * @throws IOException If there is a problem reading the challenge-data.yml file
     */
    public static void readChallengeData() throws IOException {
        // load in challenge data
        File challengeDataFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "challenge-data.yml");
        if (challengeDataFile.createNewFile()) {
            Bukkit.getLogger().info("[NotBounties] Created new challenge data file.");
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(challengeDataFile);
        // check if a console uuid exists - all challenges are the same for everyone
        String consoleUUID = new UUID(0,0).toString();

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
                    Bukkit.getLogger().warning("[NotBounties] Saved challenge no longer exists! (" + challengeIndex + ")");
                    continue;
                }
                Challenge challenge =  allChallenges.get(challengeIndex);

                // variation
                if (configuration.isSet(key + "." + challengeIndexString + ".variation")) {
                    int variationIndex = configuration.getInt(key + "." + challengeIndexString + ".variation");
                    // check for a valid variation
                    if (variationIndex >= challenge.getVariations().size()) {
                        Bukkit.getLogger().warning("[NotBounties] Saved challenge variation no longer exists! (" + challengeIndex + ":" + variationIndex + ")");
                        variationIndex = 0;
                    }
                    activeChallengeList.add(new ActiveChallenge(challenge, variationIndex));
                }

                // console won't have anything else
                if (key.equals(consoleUUID))
                    continue;

                // player specific data
                if (configuration.isSet(key + "." + challengeIndexString + ".progress")) {
                    challengeDataList.add(new ChallengeData(configuration.getDouble(key + "." + challengeIndexString + ".progress"), configuration.getDouble(key + "." + challengeIndexString + ".goal"), configuration.getBoolean(key + "." + challengeIndexString + ".rewarded")));
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

    public void saveChallengeData() throws IOException {
        // make sure the file exists
        File challengeDataFile = new File(NotBounties.getInstance().getDataFolder() + File.separator + "challenge-data.yml");
        if (challengeDataFile.createNewFile()) {
            Bukkit.getLogger().info("[NotBounties] Created new challenge data file.");
        }
        YamlConfiguration configuration = new YamlConfiguration();
        UUID consoleUUID = new UUID(0,0);
        if (globalChallenges) {
            // active challenges are only stored for the console uuid
            if (!activeChallenges.containsKey(consoleUUID))
                // no active challenges
                return;
            LinkedList<ActiveChallenge> challenges = activeChallenges.get(consoleUUID);
            for (int i = 0; i < challenges.size(); i++) {
                Challenge challenge = challenges.get(i).getChallenge();
                // get challenge index
                int challengeIndex = allChallenges.indexOf(challenge);
                if (challengeIndex == -1) {
                    // debug only because this may happen when a challenge is removed while the server is running
                    if (NotBounties.debug)
                        Bukkit.getLogger().warning("[NotBounties] Error getting index of challenge: " + challenge.getChallengeType());
                    continue;
                }
                // get variation index - already stored as indices in the list
                int variationIndex = challenges.get(i).getVariationIndex();
                if (challenge.getVariations().size() <= variationIndex) {
                    // debug only because this may happen when a variation is removed while the server is running
                    if (NotBounties.debug)
                        Bukkit.getLogger().warning("[NotBounties] Error getting variation: " + challenge.getChallengeType() + ":" + i);
                    variationIndex = 0;
                }
                // save variation to configuration
                configuration.set(consoleUUID + "." + challengeIndex + ".variation", variationIndex);
                // get player progress and goal for this challenge
                for (Map.Entry<UUID, LinkedList<ChallengeData>> entry : challengeDataMap.entrySet()) {
                    ChallengeData challengeData = entry.getValue().get(challengeIndex);
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
                    int challengeIndex = allChallenges.indexOf(challenge);
                    if (challengeIndex == -1) {
                        // debug only because this may happen when a challenge is removed while the server is running
                        if (NotBounties.debug)
                            Bukkit.getLogger().warning("[NotBounties] Error getting index of challenge: " + challenge.getChallengeType());
                        continue;
                    }

                    // get variation index - already stored as indices in the list
                    int variationIndex = activeChallengeList.get(i).getVariationIndex();
                    if (challenge.getVariations().size() <= variationIndex) {
                        // debug only because this may happen when a variation is removed while the server is running
                        if (NotBounties.debug)
                            Bukkit.getLogger().warning("[NotBounties] Error getting variation: " + challenge.getChallengeType() + ":" + i);
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

    /**
     * Get the percent completion the player has for the specific challenge
     * @param challengeIndex Index of the challenge
     * @param uuid UUID of the player
     * @return a double from 0-1
     */
    public static double getProgress(int challengeIndex, UUID uuid) {
        if (challengeDataMap.containsKey(uuid) && challengeDataMap.get(uuid).size() > challengeIndex) {
            ChallengeData challengeData = challengeDataMap.get(uuid).get(challengeIndex);
            return challengeData.getProgress() / challengeData.getGoal();
        }
        return 0;
    }


    private static void login(Player player) {
        if (!enabled)
            return;
        // check if progress needs to be generated for this player
        if (!challengeDataMap.containsKey(player.getUniqueId()))
            generateProgress(player);
    }

    private static void generateProgress(Player player) {
        if (!globalChallenges)
            generateChallenges(player.getUniqueId()); // generate new challenges
        // create progress and goal list
        LinkedList<ChallengeData> challengeDataList = new LinkedList<>();

        // get created challenges
        LinkedList<ActiveChallenge> createdChallenges = globalChallenges ? activeChallenges.get(new UUID(0,0)) : activeChallenges.get(player.getUniqueId());
        // get the progress and goal for each challenge
        for (ActiveChallenge createdChallenge : createdChallenges) {
            Challenge challenge = createdChallenge.getChallenge();
            double progressValue = challenge.getDefaultProgress(player); // get default progress value
            // add default value and goal (default value + variation amount)
            challengeDataList.add(new ChallengeData(progressValue, progressValue + challenge.getVariations().get(createdChallenge.getVariationIndex()), false));
        }
        challengeDataMap.put(player.getUniqueId(), challengeDataList);
    }



    public static void setNextChallengeChange(long nextChallengeChange) {
        ChallengeManager.nextChallengeChange = nextChallengeChange;
    }

    public static void checkChallengeChange() {
        if (!enabled)
            return;
        if (System.currentTimeMillis() < nextChallengeChange || nextChallengeChange == 0) {
            return;
        }
        nextChallengeChange = System.currentTimeMillis() + (long) (timeLimitDays * 24 * 60 * 60 * 1000);
        changeChallenges();
    }

    private static void changeChallenges() {
        activeChallenges.clear();
        challengeDataMap.clear();
        if (globalChallenges) {
            generateChallenges(new UUID(0,0));
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                generateChallenges(player.getUniqueId());
            }
        }
    }

    /**
     * Generates concurrentChallenges number of challenges for the uuid, and stores them in the activeChallenges and variation array
     * @param uuid UUID of the player to generate the challenges for
     */
    private static void generateChallenges(UUID uuid) {
        LinkedList<ActiveChallenge> activeChallengeList = new LinkedList<>();

        for (int i = 0; i < concurrentChallenges; i++) {
            Challenge challenge = getRandomChallenge(); // get random challenge
            int randomVariationIndex = (int) (Math.random() * challenge.getVariations().size()); // get random variation
            // add to list
            activeChallengeList.add(new ActiveChallenge(challenge, randomVariationIndex));
        }
        activeChallenges.put(uuid, activeChallengeList);
    }

    private static Challenge getRandomChallenge() {
        int randIndex = (int) (Math.random() * allChallenges.size());
        return allChallenges.get(randIndex);
    }

    public static String getTimeLeft() {
        if (!enabled)
            return "";
        long timeLeft = nextChallengeChange - System.currentTimeMillis();
        if (timeLeft < 0)
            return "0s";
        return LocalTime.formatTime(timeLeft, LocalTime.TimeFormat.RELATIVE);
    }

    private static double getVariation(UUID uuid, int challengeIndex) {
        if (globalChallenges) {
            // return the active challenge variation for console uuid
            return activeChallenges.get(new UUID(0,0)).get(challengeIndex).getChallenge().getVariations().get(activeChallenges.get(new UUID(0,0)).get(challengeIndex).getVariationIndex());
        } else {
            if (!activeChallenges.containsKey(uuid))
                return 0;
            // return the active challenge variation for uuid argument
            return activeChallenges.get(uuid).get(challengeIndex).getChallenge().getVariations().get(activeChallenges.get(uuid).get(challengeIndex).getVariationIndex());
        }
    }

    private static double getAdjustedProgress(UUID uuid, int challengeIndex) {
        if (!challengeDataMap.containsKey(uuid))
            return 0;
        ChallengeData challengeData = challengeDataMap.get(uuid).get(challengeIndex);
        return challengeData.getProgress() - (challengeData.getGoal() - getVariation(uuid, challengeIndex));
    }

    public static String getChallengeTitle(OfflinePlayer player, int challengeIndex) {
        if (!enabled)
            return "";
        if (challengeIndex >= concurrentChallenges)
            return ""; // out of bounds
        Challenge challenge = globalChallenges ? activeChallenges.get(new UUID(0,0)).get(challengeIndex).getChallenge() : activeChallenges.get(player.getUniqueId()).get(challengeIndex).getChallenge();
        return challenge.getChallengeTitle(player, getAdjustedProgress(player.getUniqueId(), challengeIndex), getVariation(player.getUniqueId(), challengeIndex));
    }

    public static String getGuiClaim() {
        return guiClaim;
    }

    public static String getChatClaim() {
        return chatClaim;
    }

    public static boolean isEnabled() {
        return enabled;
    }

}
