package com.nisovin.shopkeepers;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

public class ChestBreakListener implements Listener {

	private ShopkeepersPlugin plugin;
	
	public ChestBreakListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == Material.CHEST) {
			List<PlayerShopkeeper> shopkeepers = plugin.getShopkeeperOwnersOfChest(event.getBlock());
			if (shopkeepers.size() > 0) {
				for (PlayerShopkeeper shopkeeper : shopkeepers) {
					plugin.closeTradingForShopkeeper(shopkeeper.getId());
					plugin.activeShopkeepers.remove(shopkeeper.getId());
					plugin.allShopkeepersByChunk.get(shopkeeper.getChunk()).remove(shopkeeper);
					shopkeeper.remove();
				}
				plugin.save();
			}
		}
	}
	
}
