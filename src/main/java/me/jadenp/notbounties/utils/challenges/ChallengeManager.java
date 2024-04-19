package me.jadenp.notbounties.utils.challenges;

import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.utils.externalAPIs.LocalTime;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

public class ChallengeManager implements Listener {
    private static Map<UUID, LinkedList<Double>> progress = new HashMap<>(); // progress towards the challenge
    private static Map<UUID, LinkedList<Double>> goal = new HashMap<>(); // number that the player's progress needs to reach to complete the challenge
    private static Map<UUID, LinkedList<Integer>> variation = new HashMap<>(); // what variation the players are on
    private static Map<UUID, LinkedList<Challenge>> activeChallenges = new HashMap<>(); // active challenges
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
        enabled = configuration.isSet("enabled") && configuration.getBoolean("enabled"); // default false
        timeLimitDays = configuration.isSet("time-limit-days") ? configuration.getDouble("time-limit-days") : 7;
        concurrentChallenges = configuration.isSet("concurrent-challenges") ? configuration.getInt("concurrent-challenges") : 3;
        globalChallenges = !configuration.isSet("global-challenges") || configuration.getBoolean("global-challenges"); // default true
        guiClaim = configuration.isSet("gui-cliam") ? configuration.getString("gui-claim") : "&5&lClick to claim your reward";
        chatClaim = configuration.isSet("chat-claim") ? configuration.getString("chat-claim") : "&5&lDo &d/bounty challenges claim &5&lto claim your reward.";

        if (!configuration.isConfigurationSection("challenge-types"))
            return;
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
     * Get the percent completion the player has for the specific challenge
     * @param challengeIndex Index of the challenge
     * @param uuid UUID of the player
     * @return a double from 0-1
     */
    public static double getProgress(int challengeIndex, UUID uuid) {
        if (progress.containsKey(uuid) && goal.containsKey(uuid))
            return goal.get(uuid).get(challengeIndex) / progress.get(uuid).get(challengeIndex);
        return 0;
    }


    private static void login(Player player) {
        if (!enabled)
            return;
        // check if progress needs to be generated for this player
        if (!progress.containsKey(player.getUniqueId()))
            generateProgress(player);
    }

    private static void generateProgress(Player player) {
        if (!globalChallenges)
            generateChallenges(player.getUniqueId()); // generate new challenges
        // create progress and goal list
        LinkedList<Double> progressList = new LinkedList<>();
        LinkedList<Double> goalList = new LinkedList<>();

        // get created challenges
        LinkedList<Challenge> createdChallenges = globalChallenges ? activeChallenges.get(new UUID(0,0)) : activeChallenges.get(player.getUniqueId());
        // get the progress and goal for each challenge
        for (int i = 0; i < createdChallenges.size(); i++) {
            Challenge challenge = createdChallenges.get(i);
            double progressValue = challenge.getDefaultProgress(player); // get default progress value
            progressList.add(progressValue); // add to progress list
            goalList.add(progressValue + challenge.getVariations().get(variation.get(player.getUniqueId()).get(i))); // set the goal to the default progress + the variation set for this player and challenge
        }
        progress.put(player.getUniqueId(), progressList);
        goal.put(player.getUniqueId(), goalList);
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
        progress.clear();
        variation.clear();
        goal.clear();
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
        LinkedList<Challenge> challenges = new LinkedList<>();
        LinkedList<Integer> variations = new LinkedList<>();
        for (int i = 0; i < concurrentChallenges; i++) {
            Challenge challenge = getRandomChallenge(); // get random challenge
            challenges.add(challenge); // add random challenge to list
            variations.add((int) (Math.random() * challenge.getVariations().size())); // add random index of variation
        }
        activeChallenges.put(uuid, challenges);
        variation.put(uuid, variations);
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
            // return the active challenge variation
            return activeChallenges.get(new UUID(0,0)).get(challengeIndex).getVariations().get(variation.get(new UUID(0,0)).get(challengeIndex));
        } else {
            if (!activeChallenges.containsKey(uuid))
                return 0;
            return activeChallenges.get(uuid).get(challengeIndex).getVariations().get(variation.get(uuid).get(challengeIndex));
        }
    }

    private static double getAdjustedProgress(UUID uuid, int challengeIndex) {
        if (!progress.containsKey(uuid) || !goal.containsKey(uuid))
            return 0;
        return progress.get(uuid).get(challengeIndex) - (goal.get(uuid).get(challengeIndex) - getVariation(uuid, challengeIndex));
    }

    public static String getChallengeTitle(OfflinePlayer player, int challengeIndex) {
        if (!enabled)
            return "";
        if (challengeIndex >= concurrentChallenges)
            return ""; // out of bounds
        Challenge challenge = globalChallenges ? activeChallenges.get(new UUID(0,0)).get(challengeIndex) : activeChallenges.get(player.getUniqueId()).get(challengeIndex);
        return challenge.getChallengeTitle(player, getAdjustedProgress(player.getUniqueId(), challengeIndex), getVariation(player.getUniqueId(), challengeIndex));
    }

    public static String getGuiClaim() {
        return guiClaim;
    }

    public static String getChatClaim() {
        return chatClaim;
    }
}
