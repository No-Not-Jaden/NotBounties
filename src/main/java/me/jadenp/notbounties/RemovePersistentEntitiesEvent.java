package me.jadenp.notbounties;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static me.jadenp.notbounties.NotBounties.namespacedKey;
import static me.jadenp.notbounties.NotBounties.sessionKey;
import static me.jadenp.notbounties.utils.ConfigOptions.wanted;

public class RemovePersistentEntitiesEvent implements Listener {
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (wanted || !NotBounties.getInstance().getBountyBoards().isEmpty())
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
}
