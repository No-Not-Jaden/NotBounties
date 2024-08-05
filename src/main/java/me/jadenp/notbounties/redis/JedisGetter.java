package me.jadenp.notbounties.redis;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.jadenp.notbounties.Bounty;
import me.jadenp.notbounties.Setter;

import java.util.*;
import java.util.stream.Collectors;

public class JedisGetter {
    private JedisConnection jedisConnection;
    public JedisGetter(JedisConnection jedisConnection) {
        this.jedisConnection = jedisConnection;
    }

    public void addStats(UUID uuid, Double[] stats) {
        String key = "stat." + uuid.toString();
        Double[] previousStats =  getStats(uuid);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stats.length; i++) {
            if (i != 0)
                builder.append(",");
            builder.append(previousStats[i] + stats[i]);
        }
        jedisConnection.getJedis().set(key, builder.toString());
    }

    public Double[] getStats(UUID uuid) {
        String key = "stat." + uuid.toString();
        Double[] stats = new Double[]{0.0,0.0,0.0,0.0,0.0,0.0};
        if (jedisConnection.getJedis().exists(key)) {
            String[] split = jedisConnection.getJedis().get(key).split(",");
            for (int i = 0; i < split.length; i++) {
                stats[i] = Double.parseDouble(split[i]);
            }
        }
        return stats;
    }

    public Map<UUID, Double[]> getAllStats() {
        Map<UUID, Double[]> stats = new HashMap<>();
        for (UUID uuid : getStatUUIDs()) {
            stats.put(uuid, getStats(uuid));
        }
        return stats;
    }

    private String getStatUUIDString() {
        if (jedisConnection.getJedis().exists("stats")) {
            return jedisConnection.getJedis().get("stats");
        }
        return "";
    }

    public List<UUID> getStatUUIDs() {
        return convertUUIDStringList(getStatUUIDString());
    }

    public void addBounty(Bounty bounty) {
        // key will be uuid of bounty
        String key = "bounty." + bounty.getUUID().toString();
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
            jedisConnection.getJedis().set("bounties", bountyUUIDs);
        }
        sortBounties();
    }
    public void removeBounty(Bounty bounty) {

    }

    public void removeBounty(UUID uuid) {
        jedisConnection.getJedis().del("bounty." + uuid.toString());
        String bountyUUIDs = getBountyUUIDString();
        if (bountyUUIDs.contains(uuid.toString())) {
            if (bountyUUIDs.indexOf(uuid.toString()) == 0) {
                bountyUUIDs = bountyUUIDs.substring(uuid.toString().length());
            } else {
                bountyUUIDs = bountyUUIDs.substring(0, bountyUUIDs.indexOf(uuid.toString()) - 1) + bountyUUIDs.substring(bountyUUIDs.indexOf(uuid.toString()) + uuid.toString().length());
            }
            jedisConnection.getJedis().set("bounties", bountyUUIDs);
        }
    }

    private String getBountyUUIDString() {
        if (jedisConnection.getJedis().exists("bounties")) {
            return jedisConnection.getJedis().get("bounties");
        }
        return "";
    }

    public List<UUID> getBountyUUIDs() {
        return convertUUIDStringList(getBountyUUIDString());
    }

    private List<UUID> convertUUIDStringList(String list) {
        List<UUID> uuidList = new ArrayList<>();
        if (list.isEmpty())
            return uuidList;
        uuidList = Arrays.stream(list.split(",")).map(UUID::fromString).collect(Collectors.toList());
        return uuidList;
    }

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
        jedisConnection.getJedis().set("bounties", builder.toString());
    }
}
