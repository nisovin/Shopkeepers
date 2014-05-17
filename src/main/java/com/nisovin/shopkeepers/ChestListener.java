package com.nisovin.shopkeepers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

class ChestListener implements Listener {

	private final ShopkeepersPlugin plugin;

	ChestListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.CHEST) {
			this.plugin.onChestPlacement(event.getPlayer(), block);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	void onPlayerInteract1(PlayerInteractEvent event) {
		// prevent opening shop chests
		if (event.hasBlock() && event.getClickedBlock().getType() == Material.CHEST) {
			Player player = event.getPlayer();
			Block block = event.getClickedBlock();

			// check for protected chest
			if (!player.hasPermission("shopkeeper.bypass")) {
				if (this.plugin.isChestProtected(player, block)) {
					event.setCancelled(true);
					return;
				}
				for (BlockFace face : Utils.chestProtectFaces) {
					if (block.getRelative(face).getType() == Material.CHEST) {
						if (this.plugin.isChestProtected(player, block.getRelative(face))) {
							event.setCancelled(true);
							return;
						}
					}
				}
			}
		}
	}
}