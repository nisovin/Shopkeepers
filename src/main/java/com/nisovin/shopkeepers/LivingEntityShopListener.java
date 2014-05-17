package com.nisovin.shopkeepers;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

class LivingEntityShopListener implements Listener {

	protected final ShopkeepersPlugin plugin;

	LivingEntityShopListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	void onEntityInteract(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof LivingEntity)) return;
		LivingEntity shopEntity = (LivingEntity) event.getRightClicked();
		Player player = event.getPlayer();
		String playerName = player.getName();
		Log.debug("Player " + playerName + " is interacting with entity at " + shopEntity.getLocation());
		Shopkeeper shopkeeper = this.plugin.getShopkeeperByEntityId(shopEntity.getEntityId());

		if (event.isCancelled() && !Settings.bypassShopInteractionBlocking) {
			Log.debug("  Cancelled by another plugin");
		} else if (shopkeeper != null) {
			shopkeeper.onPlayerInteraction(player);
			event.setCancelled(true);
		} else {
			Log.debug("  Non-shopkeeper");
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	void onShopkeeperBeingTargeted(EntityTargetEvent event) {
		if (plugin.isShopkeeper(event.getTarget())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onEntityDamage(EntityDamageEvent event) {
		// don't allow damaging shopkeepers!
		if (this.plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
			if (event instanceof EntityDamageByEntityEvent) {
				EntityDamageByEntityEvent evt = (EntityDamageByEntityEvent) event;
				if (evt.getDamager() instanceof Monster) {
					evt.getDamager().remove();
				}
			}
		}
	}

	// ex: creepers

	@EventHandler
	void onExplodePrime(ExplosionPrimeEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	void onExplode(EntityExplodeEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
}