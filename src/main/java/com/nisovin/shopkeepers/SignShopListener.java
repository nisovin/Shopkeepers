package com.nisovin.shopkeepers;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.shopkeepers.compat.NMSManager;

class SignShopListener implements Listener {

	private final ShopkeepersPlugin plugin;

	private Location cancelNextBlockPhysicsLoc = null;

	SignShopListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	void cancelNextBlockPhysics(Location location) {
		cancelNextBlockPhysicsLoc = location;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();

		// check for sign shop
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && Utils.isSign(block.getType())) {
			Shopkeeper shopkeeper = plugin.getShopkeeperByBlock(block);
			if (shopkeeper != null) {
				// only trigger shopkeeper interaction for main-hand events:
				if (NMSManager.getProvider().isMainHandInteraction(event)) {
					Log.debug("Player " + player.getName() + " is interacting with sign shopkeeper at " + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ());
					if (event.useInteractedBlock() == Result.DENY) {
						Log.debug("  Cancelled by another plugin");
					} else {
						shopkeeper.onPlayerInteraction(player);
					}
				}

				// always cancel interactions with shopkeepers, to prevent any default behavior:
				event.setCancelled(true);
			}
		}
	}

	// protect sign block:

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (Utils.isSign(block.getType())) {
			if (plugin.getShopkeeperByBlock(block) != null) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onBlockPhysics(BlockPhysicsEvent event) {
		Block block = event.getBlock();
		if (cancelNextBlockPhysicsLoc != null && cancelNextBlockPhysicsLoc.equals(block.getLocation())) {
			event.setCancelled(true);
		} else {
			if (Utils.isSign(block.getType()) && plugin.getShopkeeperByBlock(block) != null) {
				event.setCancelled(true);
			}
		}
	}

	// TODO also listen to spigot's BlockExplodeEvent in 1.8?
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onExplosion(EntityExplodeEvent event) {
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();
			if (Utils.isSign(block.getType()) && plugin.getShopkeeperByBlock(block) != null) {
				iter.remove();
			}
		}
	}
}