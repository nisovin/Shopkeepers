package com.nisovin.shopkeepers;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

class BlockVillagerSpawnListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	void onSpawn(CreatureSpawnEvent event) {
		if (event.getEntityType() == EntityType.VILLAGER && event.getSpawnReason() != SpawnReason.CUSTOM) {
			event.setCancelled(true);
		}
	}
}
