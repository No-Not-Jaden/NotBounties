package me.jadenp.notbounties;

import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

import static me.jadenp.notbounties.NotBounties.*;
import static me.jadenp.notbounties.utils.configuration.ConfigOptions.wanted;

public class RemovePersistentEntitiesEvent implements Listener {
    private static final List<Chunk> completedChunks = new ArrayList<>();
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (wanted || !bountyBoards.isEmpty())
            for (Entity entity : event.getEntities()) {
                if (entity == null)
                    return;
                if (entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.GLOW_ITEM_FRAME && entity.getType() != EntityType.ITEM_FRAME)
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

    /**
     * Checks the chunk for bounty entities
     * @param location chunk location
     */
    public static void cleanChunk(Location location) {
        if (!location.getChunk().isLoaded()) {
            return;
        }
        if (completedChunks.contains(location.getChunk()))
            return;
        completedChunks.add(location.getChunk());
        List<Entity> entities = new ArrayList<>(List.of(location.getChunk().getEntities()));
        if (!entities.isEmpty())
            cleanAsync(entities, null);

    }

    /**
     * Iterates through all the entities in async to find any persistent bounty entities.
     * Entities are then removed back on the main thread.
     * @param entities Entities to check
     */
    public static void cleanAsync(List<Entity> entities, CommandSender sender) {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Entity> toRemove = new ArrayList<>();
                for (Entity entity : entities) {
                    if (entity == null)
                        return;
                    if (serverVersion >= 17) {
                        if (entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.GLOW_ITEM_FRAME && entity.getType() != EntityType.ITEM_FRAME)
                            continue;
                    } else {
                        if (entity.getType() != EntityType.ARMOR_STAND && entity.getType() != EntityType.ITEM_FRAME)
                            continue;
                    }
                    PersistentDataContainer container = entity.getPersistentDataContainer();
                    if (container.has(namespacedKey, PersistentDataType.STRING)) {
                        String value = container.get(namespacedKey, PersistentDataType.STRING);
                        if (value == null)
                            continue;
                        if (!value.equals(sessionKey)) {
                            toRemove.add(entity);
                        }
                    }
                }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Entity entity : toRemove) {
                                entity.remove();
                            }
                            if (sender != null) {
                                Player parser = sender instanceof Player ? (Player) sender : null;
                                sender.sendMessage(LanguageOptions.parse(LanguageOptions.prefix + LanguageOptions.entityRemove, toRemove.size(), parser));
                            }
                        }
                    }.runTask(NotBounties.getInstance());
            }
        }.runTaskAsynchronously(NotBounties.getInstance());
    }
}
