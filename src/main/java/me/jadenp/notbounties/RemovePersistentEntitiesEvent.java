package me.jadenp.notbounties;

import me.jadenp.notbounties.ui.map.BountyBoard;
import me.jadenp.notbounties.utils.configuration.LanguageOptions;
import me.jadenp.notbounties.utils.configuration.NumberFormatting;
import me.jadenp.notbounties.utils.configuration.WantedTags;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

import static me.jadenp.notbounties.NotBounties.*;

public class RemovePersistentEntitiesEvent implements Listener {
    private static final List<Chunk> completedChunks = new ArrayList<>();
    private static final List<Entity> removedEntities = new ArrayList<>();

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (NotBounties.isPaused())
            return;
        // remove persistent entities (wanted tags & bounty boards)
        if (WantedTags.isEnabled() || !BountyBoard.getBountyBoards().isEmpty())
            RemovePersistentEntitiesEvent.cleanAsync(Arrays.asList(event.getChunk().getEntities()), null);
    }

    public static void checkRemovedEntities() {
        ListIterator<Entity> entityListIterator = removedEntities.listIterator();
        while (entityListIterator.hasNext()) {
            Entity entity = entityListIterator.next();
            if (!entity.isValid()) {
                entityListIterator.remove();
            } else {
                NotBounties.debugMessage("Entity was not removed", false);
                entity.remove();
            }
        }
    }

    /**
     * Checks the chunk for bounty entities if the chunk is loaded.
     * @param location chunk location
     */
    public static void cleanChunk(Location location) {
        cleanChunk(location.getChunk());
    }

    public static void cleanChunks(List<Location> locations) {
        locations.forEach(RemovePersistentEntitiesEvent::cleanChunk);
    }

    public static void cleanChunk(Chunk chunk) {
        if (completedChunks.contains(chunk))
            return;
        completedChunks.add(chunk);
        List<Entity> entities = new ArrayList<>(List.of(chunk.getEntities()));
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
                    if (!isBountyEntity(entity))
                        continue;
                    PersistentDataContainer container = entity.getPersistentDataContainer();
                    if (container.has(getNamespacedKey(), PersistentDataType.STRING)) {
                        String value = container.get(getNamespacedKey(), PersistentDataType.STRING);
                        if (value != null && !value.equals(SESSION_KEY))
                            toRemove.add(entity);
                    }
                }

                removeSync(toRemove, sender);
            }
        }.runTaskAsynchronously(NotBounties.getInstance());
    }

    private static boolean isBountyEntity(Entity entity) {
        return entity != null
                && (entity.getType() == EntityType.ARMOR_STAND || entity.getType() == EntityType.ITEM_FRAME
                || (NotBounties.getServerVersion() >= 17 && entity.getType() == EntityType.GLOW_ITEM_FRAME));
    }

    /**
     * Synchronously remove entities from the server and send a response to the CommandSender.
     * @param toRemove Entities to remove.
     * @param sender CommandSender to send a response to.
     */
    private static void removeSync(List<Entity> toRemove, @Nullable CommandSender sender) {
        if (toRemove.isEmpty() && sender == null)
            // nothing to remove and no message to be sent.
            return;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity entity : toRemove) {
                    entity.remove();
                    removedEntities.add(entity);
                }
                if (sender != null) {
                    Player parser = sender instanceof Player player ? player : null;
                    sender.sendMessage(LanguageOptions.parse(LanguageOptions.getPrefix() + LanguageOptions.getMessage("entity-remove").replace("{amount}", NumberFormatting.formatNumber(toRemove.size())), parser));
                }
            }
        }.runTask(NotBounties.getInstance());
    }
}
