package com.nisovin.shopkeepers;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class CreeperListener implements Listener {

	final ShopkeepersPlugin plugin;
	
	public CreeperListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	void onEntityInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Creeper) {
			LivingEntity entity = (LivingEntity)event.getRightClicked();
			ShopkeepersPlugin.debug("Player " + event.getPlayer().getName() + " is interacting with creeper at " + entity.getLocation());
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
		if (event.getEntityType() == EntityType.CREEPER && plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	void onExplodePrime(ExplosionPrimeEvent event) {
		if (event.getEntityType() == EntityType.CREEPER && plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	void onExplode(EntityExplodeEvent event) {
		if (event.getEntity() != null && event.getEntity().getType() == EntityType.CREEPER && plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
	
}
