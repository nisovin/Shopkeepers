package com.nisovin.shopkeepers;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class VillagerListener implements Listener {

	final ShopkeepersPlugin plugin;
	
	public VillagerListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	void onEntityInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Villager) {
			Villager villager = (Villager)event.getRightClicked();
			ShopkeepersPlugin.debug("Player " + event.getPlayer().getName() + " is interacting with villager at " + villager.getLocation());
			Shopkeeper shopkeeper = plugin.activeShopkeepers.get("entity" + villager.getEntityId());
			if (event.isCancelled()) {
				ShopkeepersPlugin.debug("  Cancelled by another plugin");
			} else if (shopkeeper != null) {
				plugin.handleShopkeeperInteraction(event.getPlayer(), shopkeeper);
				event.setCancelled(true);
			} else if (Settings.disableOtherVillagers) {
				// don't allow trading with other villagers
				ShopkeepersPlugin.debug("  Non-shopkeeper, trade prevented");
				event.setCancelled(true);
			} else {
				ShopkeepersPlugin.debug("  Non-shopkeeper");
			}
		}
	}
	
	@EventHandler
	void onTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();
		if (target != null && target.getType() == EntityType.VILLAGER && plugin.activeShopkeepers.containsKey("entity" + target.getEntityId())) {
			event.setCancelled(true);
		}
	}
	
}
