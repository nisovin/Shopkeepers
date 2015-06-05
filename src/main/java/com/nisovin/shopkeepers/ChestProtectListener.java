package com.nisovin.shopkeepers;

import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

class ChestProtectListener implements Listener {

	private final ShopkeepersPlugin plugin;

	ChestProtectListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (Utils.isChest(block.getType())) {
			Player player = event.getPlayer();
			if (!Utils.hasPermission(player, ShopkeepersAPI.BYPASS_PERMISSION)) {
				if (plugin.isChestProtected(player, block)) {
					event.setCancelled(true);
					return;
				}
				if (Utils.isProtectedChestAroundChest(player, block)) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		Material type = block.getType();
		if (Utils.isChest(type)) {
			Player player = event.getPlayer();
			if (Utils.isProtectedChestAroundChest(player, block)) {
				event.setCancelled(true);
			}
		} else if (type == Material.HOPPER) {
			Player player = event.getPlayer();
			if (Utils.isProtectedChestAroundHopper(player, block)) {
				event.setCancelled(true);
			}
		} else if (type == Material.RAILS || type == Material.POWERED_RAIL || type == Material.DETECTOR_RAIL || type == Material.ACTIVATOR_RAIL) {
			Player player = event.getPlayer();
			Block b = block.getRelative(BlockFace.UP);
			if (Utils.isChest(b.getType()) && plugin.isChestProtected(player, b)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onInventoryMoveItem(InventoryMoveItemEvent event) {
		if (event.getSource() != null) {
			InventoryHolder holder = event.getSource().getHolder();
			if (holder != null && holder instanceof Chest) {
				Block block = ((Chest) holder).getBlock();
				if (plugin.isChestProtected(null, block)) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onExplosion(EntityExplodeEvent event) {
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();
			if (Utils.isChest(block.getType())) {
				if (plugin.isChestProtected(null, block) || Utils.isProtectedChestAroundChest(null, block)) {
					iter.remove();
				}
			}
		}
	}
}