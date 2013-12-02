package com.nisovin.shopkeepers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

class ChestProtectListener implements Listener {

	ShopkeepersPlugin plugin;
	
	ChestProtectListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(ignoreCancelled=true)
	void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.CHEST) {
			Player player = event.getPlayer();
			Block block = event.getBlock();
			if (!event.getPlayer().hasPermission("shopkeeper.bypass")) {
				if (plugin.isChestProtected(player, block)) {
					event.setCancelled(true);
					return;
				}
				for (BlockFace face : plugin.chestProtectFaces) {
					if (block.getRelative(face).getType() == Material.CHEST) {
						if (plugin.isChestProtected(player, block.getRelative(face))) {
							event.setCancelled(true);
							return;
						}
					}
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		int id = block.getTypeId();
		if (id == Material.CHEST.getId()) {
			Player player = event.getPlayer();
			Block b;
			for (BlockFace face : plugin.chestProtectFaces) {
				b = block.getRelative(face);
				if (b.getType() == Material.CHEST) {
					if (plugin.isChestProtected(player, b)) {
						event.setCancelled(true);
						return;
					}				
				}
			}
		} else if (id == 154 /* hopper */) {
			Player player = event.getPlayer();
			Block b;
			for (BlockFace face : plugin.hopperProtectFaces) {
				b = block.getRelative(face);
				if (b.getType() == Material.CHEST) {
					if (plugin.isChestProtected(player, b)) {
						event.setCancelled(true);
						return;
					}				
				}
			}
		} else if (id == Material.RAILS.getId() || id == Material.POWERED_RAIL.getId() || id == Material.DETECTOR_RAIL.getId() || id == 157 /*activator rail*/) {
			Player player = event.getPlayer();
			Block b = block.getRelative(BlockFace.UP);
			if (b.getType() == Material.CHEST && plugin.isChestProtected(player, b)) {
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	void onInventoryMoveItem(InventoryMoveItemEvent event) {
		if (event.getSource() != null) {
			InventoryHolder holder = event.getSource().getHolder();
			if (holder != null && holder instanceof Chest) {
				Block block = ((Chest)holder).getBlock();
				if (plugin.isChestProtected(null, block)) {
					event.setCancelled(true);
				}
			}
		}
	}
	
}
