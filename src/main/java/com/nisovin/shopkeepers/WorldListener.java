package com.nisovin.shopkeepers;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

class WorldListener implements Listener {

	private final ShopkeepersPlugin plugin;

	WorldListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	void onChunkLoad(ChunkLoadEvent event) {
		final Chunk chunk = event.getChunk();
		Bukkit.getScheduler().runTaskLater(this.plugin, new Runnable() {
			public void run() {
				if (chunk.isLoaded()) {
					plugin.loadShopkeepersInChunk(chunk);
				}
			}
		}, 2);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onChunkUnload(ChunkUnloadEvent event) {
		this.plugin.unloadShopkeepersInChunk(event.getChunk());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void onWorldLoad(WorldLoadEvent event) {
		this.plugin.loadShopkeepersInWorld(event.getWorld());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void onWorldUnload(WorldUnloadEvent event) {
		this.plugin.unloadShopkeepersInWorld(event.getWorld());
	}
}