package com.nisovin.shopkeepers;

import org.bukkit.Bukkit;
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

import com.nisovin.shopkeepers.events.OpenTradeEvent;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

public class BlockListener implements Listener {

	final ShopkeepersPlugin plugin;
	
	public BlockListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		
		// check for sign shop
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && (block.getType() == Material.SIGN_POST || block.getType() == Material.WALL_SIGN)) {
			Shopkeeper shopkeeper = plugin.activeShopkeepers.get("block" + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ());
			if (shopkeeper != null) {
				ShopkeepersPlugin.debug("Player " + player.getName() + " is interacting with sign shopkeeper at " + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ());
				if (event.useInteractedBlock() == Result.DENY) {
					ShopkeepersPlugin.debug("  Cancelled by another plugin");
				} else if (event.getPlayer().isSneaking()) {
					// modifying a shopkeeper
					ShopkeepersPlugin.debug("  Opening editor window...");
					event.setCancelled(true);
					boolean isEditing = shopkeeper.onEdit(event.getPlayer());
					if (isEditing) {
						ShopkeepersPlugin.debug("  Editor window opened");
						plugin.editing.put(event.getPlayer().getName(), shopkeeper.getId());
					} else {
						ShopkeepersPlugin.debug("  Editor window NOT opened");
					}
				} else {
					ShopkeepersPlugin.debug("  Opening trade window...");
					OpenTradeEvent evt = new OpenTradeEvent(event.getPlayer(), shopkeeper);
					Bukkit.getPluginManager().callEvent(evt);
					if (evt.isCancelled()) {
						ShopkeepersPlugin.debug("  Trade cancelled by another plugin");
						event.setCancelled(true);
						return;
					}
					plugin.openTradeWindow(shopkeeper, event.getPlayer());
					plugin.purchasing.put(event.getPlayer().getName(), shopkeeper.getId());
					ShopkeepersPlugin.debug("  Trade window opened");
					return;
				}
			}
		}
	}
	
	@EventHandler
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST) {
			if (plugin.activeShopkeepers.containsKey("block" + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ())) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	void onSignPlace(SignChangeEvent event) {
		Block block = event.getBlock();
		Shopkeeper shopkeeper = plugin.activeShopkeepers.get("block" + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ());
		if (shopkeeper != null && shopkeeper instanceof PlayerShopkeeper) {
			event.setLine(0, Settings.signShopFirstLine);
			String name = shopkeeper.getName();
			if (name.length() > 15) name = name.substring(0, 15);
			event.setLine(1, name);
			event.setLine(2, ((PlayerShopkeeper)shopkeeper).getOwner());
			event.setLine(3, "");
		}
	}
}
