package com.nisovin.shopkeepers;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Witch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class WitchListener implements Listener {

	final ShopkeepersPlugin plugin;
	
	public WitchListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	void onEntityInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Witch) {
			LivingEntity entity = (LivingEntity)event.getRightClicked();
			ShopkeepersPlugin.debug("Player " + event.getPlayer().getName() + " is interacting with witch at " + entity.getLocation());
			Shopkeeper shopkeeper = plugin.activeShopkeepers.get("entity" + entity.getEntityId());
			if (event.isCancelled()) {
				ShopkeepersPlugin.debug("  Cancelled by another plugin");
			} else if (shopkeeper != null) {
				plugin.handleShopkeeperInteraction(event.getPlayer(), shopkeeper);
				event.setCancelled(true);
			} else {
				ShopkeepersPlugin.debug("  Non-shopkeeper");
			}
		}
	}
	
	@EventHandler
	void onTarget(EntityTargetEvent event) {
		if (event.getEntityType() == EntityType.WITCH && plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
	
}
