package me.jadenp.notbounties.redis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Setter;

import java.util.*;

public class JedisGetter {
    private final JedisConnection jedisConnection;

    private static final String BOUNTY_PREFIX = "bounty.";
    private static final String STAT_PREFIX = "stat.";
    private static final String BOUNTY_UUIDS_KEY = "bounties";
    private static final String STAT_UUIDS_KEY = "stats";

    public JedisGetter(JedisConnection jedisConnection) {
        this.jedisConnection = jedisConnection;
    }

    /**
     * Add stats of a player to the redis database
     * @param uuid UUID of the player
     * @param stats The stats to add
     */
    public void addStats(UUID uuid, Double[] stats) {
        String key = STAT_PREFIX + uuid.toString();
        if (!jedisConnection.getJedis().exists(key)) {
            // add key to recorded uuids
            String statUUIDs = getStatUUIDString();
            if (!statUUIDs.isEmpty())
                statUUIDs += ",";
            statUUIDs += uuid.toString();
            jedisConnection.getJedis().set(STAT_UUIDS_KEY, statUUIDs);
        }
        Double[] previousStats =  getStats(uuid);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stats.length; i++) {
            if (i != 0)
                builder.append(",");
            builder.append(previousStats[i] + stats[i]);
        }
        jedisConnection.getJedis().set(key, builder.toString());
    }

    /**
     * Get stats of a player
     * @param uuid UUID of the player
     * @return A 6 element array of the player's recorded stats
     */
    public Double[] getStats(UUID uuid) {
        String key = STAT_PREFIX + uuid.toString();
        Double[] stats = new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
        if (jedisConnection.getJedis().exists(key)) {
            String[] split = jedisConnection.getJedis().get(key).split(",");
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
        for (UUID uuid : getStatUUIDs()) {
            stats.put(uuid, getStats(uuid));
        }
        return stats;
    }

    private String getStatUUIDString() {
        if (jedisConnection.getJedis().exists(STAT_UUIDS_KEY)) {
            return jedisConnection.getJedis().get(STAT_UUIDS_KEY);
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

    /**
     * Add a bounty to the redis database
     * @param bounty Bounty to be added
     */
    public void addBounty(Bounty bounty) {
        // key will be uuid of bounty
        String key = BOUNTY_PREFIX + bounty.getUUID().toString();
        if (jedisConnection.getJedis().exists(key)) {
            // bounty already exists - add to existing json
            JsonObject jsonObject = (JsonObject) jedisConnection.getJedis().jsonGet(key);
            JsonArray settersArray = jsonObject.getAsJsonArray("setters");
            for (Setter setter : bounty.getSetters()) {
                settersArray.add(setter.toJson());
            }
            jedisConnection.getJedis().jsonSet(key, jsonObject);
        } else {
            // new bounty
            jedisConnection.getJedis().jsonSet(key, bounty.toJson());
            String bountyUUIDs = getBountyUUIDString();
            if (!bountyUUIDs.isEmpty())
                bountyUUIDs += ",";
            bountyUUIDs += bounty.getUUID().toString();
            jedisConnection.getJedis().set(BOUNTY_UUIDS_KEY, bountyUUIDs);
        }
        sortBounties();
    }

    /**
     * Remove bounty from the redis database
     * @param bounty Bounty to be removed
     * @return True if the bounty was removed, false if no bounty was found
     */
    public boolean removeBounty(Bounty bounty) {
        String key = BOUNTY_PREFIX + bounty.getUUID().toString();
        if (jedisConnection.getJedis().exists(key)) {
            Bounty currentBounty = new Bounty((JsonObject) jedisConnection.getJedis().jsonGet(key));
            // delete if uuid and time created match in any setter from the two bounties
            currentBounty.getSetters().removeIf(setter -> bounty.getSetters().stream().anyMatch(toDeleteSetter -> setter.getUuid().equals(toDeleteSetter.getUuid()) && setter.getTimeCreated() == toDeleteSetter.getTimeCreated()));
            if (currentBounty.getSetters().isEmpty()) {
                // bounty should be deleted
                return removeBounty(bounty.getUUID());
            } else {
                // replace bounty
                jedisConnection.getJedis().jsonSet(key, currentBounty.toJson());
                sortBounties();
            }
            return true;
        }
        return false;
    }

    /**
     * Remove a player's bounty from the database
     * @param uuid UUID of the player
     * @return True if a bounty was removed, false if no bounty was found
     */
    public boolean removeBounty(UUID uuid) {
        jedisConnection.getJedis().del(BOUNTY_PREFIX + uuid.toString());
        String bountyUUIDs = getBountyUUIDString();
        if (bountyUUIDs.contains(uuid.toString())) {
            if (bountyUUIDs.indexOf(uuid.toString()) == 0) {
                bountyUUIDs = bountyUUIDs.substring(uuid.toString().length());
            } else {
                bountyUUIDs = bountyUUIDs.substring(0, bountyUUIDs.indexOf(uuid.toString()) - 1) + bountyUUIDs.substring(bountyUUIDs.indexOf(uuid.toString()) + uuid.toString().length());
            }
            jedisConnection.getJedis().set(BOUNTY_UUIDS_KEY, bountyUUIDs);
            return true;
        }
        return false;
    }

    private String getBountyUUIDString() {
        if (jedisConnection.getJedis().exists(BOUNTY_UUIDS_KEY)) {
            return jedisConnection.getJedis().get(BOUNTY_UUIDS_KEY);
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
            JsonObject bountyJson = (JsonObject) jedisConnection.getJedis().jsonGet(uuid);
            bounties.add(new Bounty(bountyJson));
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
        jedisConnection.getJedis().set(BOUNTY_UUIDS_KEY, builder.toString());
    }
}
