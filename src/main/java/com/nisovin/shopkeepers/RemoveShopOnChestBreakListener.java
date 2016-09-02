package com.nisovin.shopkeepers;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

class RemoveShopOnChestBreakListener implements Listener {

	private final ShopkeepersPlugin plugin;

	RemoveShopOnChestBreakListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (Utils.isChest(block.getType())) {
			List<PlayerShopkeeper> shopkeepers = plugin.getProtectedChests().getShopkeeperOwnersOfChest(block);
			if (shopkeepers.size() > 0) {
				for (PlayerShopkeeper shopkeeper : shopkeepers) {
					// return creation item for player shopkeepers:
					if (Settings.deletingPlayerShopReturnsCreationItem) {
						ItemStack creationItem = Settings.createCreationItem();
						block.getWorld().dropItemNaturally(block.getLocation(), creationItem);
					}
					plugin.deleteShopkeeper(shopkeeper);
				}
				plugin.save();
			}
		}
	}
}
