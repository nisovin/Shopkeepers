package com.nisovin.shopkeepers;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event.Result;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

class BlockShopListener implements Listener {

	private final ShopkeepersPlugin plugin;

	BlockShopListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();

		// check for sign shop
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && (block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN)) {
			Shopkeeper shopkeeper = plugin.getShopkeeperByBlock(block);
			if (shopkeeper != null) {
				Log.debug("Player " + player.getName() + " is interacting with sign shopkeeper at " + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ());
				if (event.useInteractedBlock() == Result.DENY) {
					Log.debug("  Cancelled by another plugin");
				} else {
					shopkeeper.onPlayerInteraction(player);
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
			if (plugin.getShopkeeperByBlock(block) != null) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	void onSignPlace(SignChangeEvent event) {
		Block block = event.getBlock();
		Shopkeeper shopkeeper = plugin.getShopkeeperByBlock(block);
		if (shopkeeper != null && shopkeeper instanceof PlayerShopkeeper) {
			event.setLine(0, Settings.signShopFirstLine);
			String name = shopkeeper.getName();
			if (name == null) name = "";
			else if (name.length() > 15) name = name.substring(0, 15);
			event.setLine(1, name);
			event.setLine(2, ((PlayerShopkeeper) shopkeeper).getOwnerName());
			event.setLine(3, "");
		}
	}
}