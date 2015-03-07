package com.nisovin.shopkeepers;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.PigZapEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.weather.LightningStrikeEvent;

import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;

class LivingEntityShopListener implements Listener {

	// the radius around lightning strikes in which villagers turn into witches
	private static final int VILLAGER_ZAP_RADIUS = 7; // minecraft wiki says 3-4, we use 7 to be safe

	private final ShopkeepersPlugin plugin;

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
		Shopkeeper shopkeeper = plugin.getShopkeeperByEntity(shopEntity); // also check for citizens npc shopkeepers

		if (event.isCancelled() && !Settings.bypassShopInteractionBlocking) {
			Log.debug("  Cancelled by another plugin");
		} else if (shopkeeper != null) {
			shopkeeper.onPlayerInteraction(player);
			// if citizens npc: don't cancel the event, let Citizens perform other actions as appropriate
			if (shopkeeper.getShopObject().getObjectType() != DefaultShopObjectTypes.CITIZEN) {
				event.setCancelled(true);
			}
		} else {
			Log.debug("  Non-shopkeeper");
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onShopkeeperBeingTargeted(EntityTargetEvent event) {
		if (plugin.isShopkeeper(event.getTarget())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onEntityDamage(EntityDamageEvent event) {
		// block damaging of shopkeepers
		if (plugin.isShopkeeper(event.getEntity())) {
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

	@EventHandler(ignoreCancelled = true)
	void onExplodePrime(ExplosionPrimeEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onExplode(EntityExplodeEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
			Log.debug("Cancelled event for living shop: " + event.getEventName());
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onCreeperCharged(CreeperPowerEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	// ex: enderman

	@EventHandler(ignoreCancelled = true)
	void onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onEntityTeleport(EntityTeleportEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onPigZap(PigZapEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onSheepDyed(SheepDyeWoolEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onLightningStrike(LightningStrikeEvent event) {
		// workaround: preventing lightning strikes near villager shopkeepers
		// because they would turn into witches
		Location loc = event.getLightning().getLocation();
		for (Entity entity : Utils.getNearbyEntities(loc, VILLAGER_ZAP_RADIUS, EntityType.VILLAGER)) {
			if (plugin.isShopkeeper(entity)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onPotionSplash(PotionSplashEvent event) {
		for (LivingEntity entity : event.getAffectedEntities()) {
			if (plugin.isShopkeeper(entity)) {
				event.setIntensity(entity, 0.0D);
			}
		}
	}

	// ex: blazes or skeletons

	@EventHandler(ignoreCancelled = true)
	void onEntityLaunchProjectile(ProjectileLaunchEvent event) {
		if (plugin.isShopkeeper(event.getEntity().getShooter())) {
			event.setCancelled(true);
		}

		/*
		 * ProjectileSource source = event.getEntity().getShooter();
		 * if (source instanceof LivingEntity) {
		 * if (plugin.isShopkeeper((LivingEntity) source)) {
		 * event.setCancelled(true);
		 * }
		 * }
		 */
	}

	// ex: snowmans

	@EventHandler(ignoreCancelled = true)
	void onEntityBlockForm(EntityBlockFormEvent event) {
		if (plugin.isShopkeeper(event.getEntity())) {
			event.setCancelled(true);
		}
	}
}