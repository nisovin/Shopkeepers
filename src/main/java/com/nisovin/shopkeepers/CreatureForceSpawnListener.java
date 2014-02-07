package com.nisovin.shopkeepers;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class CreatureForceSpawnListener implements Listener {

	private Location nextSpawnLocation = null;
	private EntityType nextEntityType = null;

	// This listener tries to bypass other plugins which block the spawning of living shopkeeper entities.
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (nextSpawnLocation == null) return;
		if (event.getEntityType() == nextEntityType && event.getLocation().equals(nextSpawnLocation)) {
			event.setCancelled(false);
		} else {
			// this shouldn't normally be reached..
			ShopkeepersPlugin.debug("Shopkeeper entity-spawning seems to be out of sync: spawn-force was activated for an entity of type " 
					+ nextEntityType.name() + " at location " + nextSpawnLocation.toString() + ", but a (different) entity of type " 
					+ event.getEntityType().name() + " was spawned at location " + event.getLocation().toString() + ".");
		}
		nextSpawnLocation = null;
		nextEntityType = null;
	}

	public void forceCreatureSpawn(Location location, EntityType entityType) {
		this.nextSpawnLocation = location;
		this.nextEntityType = entityType;
	}
}