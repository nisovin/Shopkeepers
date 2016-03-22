package com.nisovin.shopkeepers;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

class WorldListener implements Listener {

	private final ShopkeepersPlugin plugin;

	WorldListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	void onChunkLoad(ChunkLoadEvent event) {
		final Chunk chunk = event.getChunk();
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				if (chunk.isLoaded()) {
					plugin.loadShopkeepersInChunk(chunk);
				}
			}
		}, 2);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onChunkUnload(ChunkUnloadEvent event) {
		plugin.unloadShopkeepersInChunk(event.getChunk());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	void onWorldSave(WorldSaveEvent event) {
		World world = event.getWorld();
		final UUID worldUID = world.getUID();
		Log.debug("World '" + world.getName() + "' is about to get saved: Unloading all shopkeepers in that world.");
		plugin.unloadShopkeepersInWorld(world);
		Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			public void run() {
				// check if the world is still loaded:
				World world = Bukkit.getWorld(worldUID);
				if (world != null) {
					Log.debug("World '" + world.getName() + "' was saved. Reloading all shopkeepers in that world.");
					plugin.loadShopkeepersInWorld(world);
				}
			}
		}, 2);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void onWorldLoad(WorldLoadEvent event) {
		plugin.loadShopkeepersInWorld(event.getWorld());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	void onWorldUnload(WorldUnloadEvent event) {
		plugin.unloadShopkeepersInWorld(event.getWorld());
	}
}