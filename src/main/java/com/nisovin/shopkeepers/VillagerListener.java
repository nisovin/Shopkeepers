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
			// TODO: make this configureable to give server owners the possibility to bypass other plugins which cancle entity interaction?
			// example: currently many "land protection" plugins will cancle interactions, causing shops placed on land of other players to 'not work'
			// or leave this to those land protection plugins to "fix"?
			if (event.isCancelled()) {
				ShopkeepersPlugin.debug("  Cancelled by another plugin");
			} else if (shopkeeper != null) {
				plugin.handleShopkeeperInteraction(event.getPlayer(), shopkeeper);
				event.setCancelled(true);
			} else if (villager.hasMetadata("NPC")) {
				// ignore any interaction with citizens2 NPCs
				ShopkeepersPlugin.debug("  Non-shopkeeper, ignoring (probably citizens2) NPC");
				return;
			} else if (Settings.disableOtherVillagers) {
				// don't allow trading with other villagers
				event.setCancelled(true);
				if (Settings.hireOtherVillagers) {
					// allow hiring of other villagers
					ShopkeepersPlugin.debug("  Non-shopkeeper, trade prevented, but possible hire");
					plugin.handleHireOtherVillager(event.getPlayer(), villager);
				} else {
					ShopkeepersPlugin.debug("  Non-shopkeeper, trade prevented");
				}
			} else if (Settings.hireOtherVillagers) {
				// allow hiring of other villagers
				ShopkeepersPlugin.debug("  Non-shopkeeper, possible hire");
				if (plugin.handleHireOtherVillager(event.getPlayer(), villager)) {
					// hiring was successful -> prevent trading
					ShopkeepersPlugin.debug("  Non-shopkeeper, possible hire.. success -> possible trade prevented");
					event.setCancelled(true); 
				} else {
					// hiring was not successful -> no preventing of normal villager trading
					ShopkeepersPlugin.debug("  Non-shopkeeper, possible hire.. failed");
				}
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
