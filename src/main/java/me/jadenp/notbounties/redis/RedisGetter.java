package me.jadenp.notbounties.redis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.NotBounties;
import me.jadenp.notbounties.Setter;

import javax.annotation.Nullable;
import java.util.*;

public class RedisGetter {
    private final RedisConnection redisConnection;
    

    private static final String BOUNTY_PREFIX = "bounty.";
    private static final String STAT_PREFIX = "stat.";
    private static final String BOUNTY_UUIDS_KEY = "bounties";
    private static final String STAT_UUIDS_KEY = "stats";

    public RedisGetter(RedisConnection redisConnection) {
        this.redisConnection = redisConnection;
    }

    private boolean isConnectionValid() {
        return redisConnection.getData() != null;
    }

    /**
     * Add stats of a player to the redis database
     * @param uuid UUID of the player
     * @param stats The stats to add
     */
    public void addStats(UUID uuid, Double[] stats) {
        if (!isConnectionValid()) {
            NotBounties.debugMessage("Redis has never connected! Cannot add stats to database.", true);
            return;
        }
        String key = STAT_PREFIX + uuid.toString();
        if (redisConnection.getData().exists(key) == 0) {
            // add key to recorded uuids
            String statUUIDs = getStatUUIDString();
            if (!statUUIDs.isEmpty())
                statUUIDs += ",";
            statUUIDs += uuid.toString();
            redisConnection.getData().set(STAT_UUIDS_KEY, statUUIDs);
        }
        Double[] previousStats =  getStats(uuid);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stats.length; i++) {
            if (i != 0)
                builder.append(",");
            builder.append(previousStats[i] + stats[i]);
        }
        redisConnection.getData().set(key, builder.toString());
    }

    /**
     * Get stats of a player
     * @param uuid UUID of the player
     * @return A 6 element array of the player's recorded stats
     */
    public Double[] getStats(UUID uuid) {
        String key = STAT_PREFIX + uuid.toString();
        Double[] stats = new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
        if (isConnectionValid() && redisConnection.getData().exists(key) > 0) {
            String[] split = redisConnection.getData().get(key).split(",");
            for (int i = 0; i < split.length; i++) {
                stats[i] = Double.parseDouble(split[i]);
            }
        }
        return stats;
    }

    /**
     * Get all the stats in the redis database
     * @return All recorded player stats
     */
    public Map<UUID, Double[]> getAllStats() {
        Map<UUID, Double[]> stats = new HashMap<>();
        if (!isConnectionValid())
            return stats;
        for (UUID uuid : getStatUUIDs()) {
            stats.put(uuid, getStats(uuid));
        }
        return stats;
    }

    private String getStatUUIDString() {
        if (isConnectionValid() && redisConnection.getData().exists(STAT_UUIDS_KEY) > 0) {
            return redisConnection.getData().get(STAT_UUIDS_KEY);
        }
        return "";
    }

    /**
     *
     * @return all the uuids that have stats recorded in the redis database
     */
    public List<UUID> getStatUUIDs() {
        return convertUUIDStringList(getStatUUIDString());
    }

    public void setAllData(List<Bounty> bounties, Map<UUID, Double[]> stats) {
        if (!isConnectionValid()) {
            NotBounties.debugMessage("Cannot set all data to Redis!", false);
            return;
        }
        // delete all data
        for (String uuid : getBountyUUIDString().split(",")) {
            redisConnection.getData().del(BOUNTY_PREFIX + uuid);
        }

        for (String uuid : getStatUUIDString().split(",")) {
            redisConnection.getData().del(STAT_PREFIX + uuid);
        }

        // add new data
        StringBuilder builder = new StringBuilder();
        bounties.sort(Collections.reverseOrder());
        for (Bounty bounty : bounties) {
            if (!builder.isEmpty())
                builder.append(",");
            builder.append(bounty.getUUID());
            redisConnection.getData().set(BOUNTY_PREFIX + bounty.getUUID(), bounty.toJson().toString());
        }
        redisConnection.getData().set(BOUNTY_UUIDS_KEY, builder.toString());

        builder = new StringBuilder();
        for (Map.Entry<UUID, Double[]> entry : stats.entrySet()) {
            if (!builder.isEmpty())
                builder.append(",");
            builder.append(entry.getKey());
            StringBuilder statBuilder = new StringBuilder();
            for (int i = 0; i < entry.getValue().length; i++) {
                if (i != 0)
                    statBuilder.append(",");
                statBuilder.append(entry.getValue()[i]);
            }
            redisConnection.getData().set(STAT_PREFIX + entry.getKey(), statBuilder.toString());
        }
        redisConnection.getData().set(STAT_UUIDS_KEY, builder.toString());
    }

    /**
     * Add a bounty to the redis database
     * @param bounty Bounty to be added
     */
    public void addBounty(Bounty bounty) {
        if (!isConnectionValid()) {
            NotBounties.debugMessage("Redis has never been connected! Cannot add bounty to database.", true);
            return;
        }
        // key will be uuid of bounty
        String key = BOUNTY_PREFIX + bounty.getUUID().toString();
        if (redisConnection.getData().exists(key) > 0) {
            // bounty already exists - add to existing json
            JsonObject jsonObject = new JsonParser().parse(redisConnection.getData().get(key)).getAsJsonObject();
            JsonArray settersArray = jsonObject.getAsJsonArray("setters");
            for (Setter setter : bounty.getSetters()) {
                settersArray.add(setter.toJson());
            }
            redisConnection.getData().set(key, jsonObject.toString());
        } else {
            // new bounty
            redisConnection.getData().set(key, bounty.toJson().toString());
            String bountyUUIDs = getBountyUUIDString();
            if (!bountyUUIDs.isEmpty())
                bountyUUIDs += ",";
            bountyUUIDs += bounty.getUUID().toString();
            redisConnection.getData().set(BOUNTY_UUIDS_KEY, bountyUUIDs);
        }
        sortBounties();
    }

    /**
     * Replaces a bounty in the jedis database
     *
     * @param uuid   UUID of the bounty to be replaced
     * @param bounty Replacement bounty. A null value will remove the bounty.
     */
    public void replaceBounty(UUID uuid, @Nullable Bounty bounty) {
        String key = BOUNTY_PREFIX + uuid.toString();
        if (isConnectionValid() && redisConnection.getData().exists(key) > 0) {
            if (bounty != null)
                redisConnection.getData().set(key, bounty.toJson().toString());
            else
                removeBounty(uuid);
        }
    }

    public @Nullable Bounty getBounty(UUID uuid) {
        String key = BOUNTY_PREFIX + uuid.toString();
        if (isConnectionValid() && redisConnection.getData().exists(key) > 0) {
            return new Bounty(redisConnection.getData().get(key));
        }
        return null;
    }

    /**
     * Remove bounty from the redis database
     * @param bounty Bounty to be removed
     * @return True if the bounty was removed, false if no bounty was found
     */
    public boolean removeBounty(Bounty bounty) {
        String key = BOUNTY_PREFIX + bounty.getUUID().toString();
        if (isConnectionValid() && redisConnection.getData().exists(key) > 0) {
            Bounty currentBounty = new Bounty(redisConnection.getData().get(key));
            // delete if uuid and time created match in any setter from the two bounties
            currentBounty.getSetters().removeIf(setter -> bounty.getSetters().stream().anyMatch(toDeleteSetter -> setter.getUuid().equals(toDeleteSetter.getUuid()) && setter.getTimeCreated() == toDeleteSetter.getTimeCreated()));
            if (currentBounty.getSetters().isEmpty()) {
                // bounty should be deleted
                redisConnection.getData().del(key);
            } else {
                // replace bounty
                redisConnection.getData().set(key, currentBounty.toJson().toString());
                sortBounties();
            }
            return true;
        }
        return false;
    }

    /**
     * Remove a player's bounty from the database
     * @param uuid UUID of the player
     * @return The bounty that was removed, or null if no bounty was removed.
     */
    public @Nullable Bounty removeBounty(UUID uuid) {
        if (!isConnectionValid())
            return null;
        String key = BOUNTY_PREFIX + uuid.toString();
        String bountyJson = redisConnection.getData().get(key);
        if (bountyJson != null) {
            redisConnection.getData().del(key);
            String bountyUUIDs = getBountyUUIDString();
            if (bountyUUIDs.contains(uuid.toString())) {
                if (bountyUUIDs.indexOf(uuid.toString()) == 0) {
                    bountyUUIDs = bountyUUIDs.substring(uuid.toString().length());
                } else {
                    bountyUUIDs = bountyUUIDs.substring(0, bountyUUIDs.indexOf(uuid.toString()) - 1) + bountyUUIDs.substring(bountyUUIDs.indexOf(uuid.toString()) + uuid.toString().length());
                }
                redisConnection.getData().set(BOUNTY_UUIDS_KEY, bountyUUIDs);
            }
            return new Bounty(bountyJson);
        }
        return null;
    }

    private String getBountyUUIDString() {
        if (isConnectionValid() && redisConnection.getData().exists(BOUNTY_UUIDS_KEY) > 0) {
            return redisConnection.getData().get(BOUNTY_UUIDS_KEY);
        }
        return "";
    }

    /**
     *
     * @return all the recorded bounty uuids in the redis database
     */
    public List<UUID> getBountyUUIDs() {
        return convertUUIDStringList(getBountyUUIDString());
    }

    private List<UUID> convertUUIDStringList(String list) {
        List<UUID> uuidList = new ArrayList<>();
        if (list.isEmpty())
            return uuidList;
        uuidList = Arrays.stream(list.split(",")).map(UUID::fromString).toList();
        return uuidList;
    }

    /**
     * Get all the bounties in the redis database
     * @param sortType How the returned list should be sorted
     * @return A list of all the bounties in the redis database
     */
    public List<Bounty> getAllBounties(int sortType) {
        List<Bounty> bounties = new ArrayList<>();
        for (String uuid : getBountyUUIDString().split(",")) {
            String data = redisConnection.getData().get(BOUNTY_PREFIX + uuid);
            if (data != null)
                bounties.add(new Bounty(data));
        }
        // bounties should be in descending order already
        if (sortType == -1 || sortType == 2)
            return bounties;
        if (sortType == 3) {
            Collections.reverse(bounties);
            return bounties;
        }
        // sort in other ways
        Bounty temp;
        for (int i = 0; i < bounties.size(); i++) {
            for (int j = i + 1; j < bounties.size(); j++) {
                if ((bounties.get(i).getSetters().get(0).getTimeCreated() > bounties.get(j).getSetters().get(0).getTimeCreated() && sortType == 0) || // oldest bounties at top
                        (bounties.get(i).getSetters().get(0).getTimeCreated() < bounties.get(j).getSetters().get(0).getTimeCreated() && sortType == 1)) { // newest bounties at top
                    temp = bounties.get(i);
                bounties.set(i, bounties.get(j));
                bounties.set(j, temp);
                }
            }
        }

        return bounties;
    }

    /**
     * Sort all the bounties in the jedis database
     */
    public void sortBounties() {
        // get all bounties, sort them, and re add the bountyUUIDs in a sorted order
        List<Bounty> bounties = getAllBounties(-1);
        bounties.sort(Collections.reverseOrder());
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bounties.size(); i++) {
            if (i != 0)
                builder.append(",");
            builder.append(bounties.get(i).getUUID());
        }
        redisConnection.getData().set(BOUNTY_UUIDS_KEY, builder.toString());
    }
}
