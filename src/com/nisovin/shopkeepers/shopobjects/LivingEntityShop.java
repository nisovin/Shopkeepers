package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeepersPlugin;

public abstract class LivingEntityShop extends ShopObject {
	
	protected LivingEntity entity;
	private String uuid;
	private int respawnAttempts = 0;
	
	@Override
	public void load(ConfigurationSection config) {
		if (config.contains("uuid")) {
			uuid = config.getString("uuid");
		}
	}

	@Override
	public void save(ConfigurationSection config) {
		if (entity != null) {
			config.set("uuid", entity.getUniqueId().toString());
		}
	}
	
	protected abstract EntityType getEntityType();
	
	@Override
	public boolean needsSpawned() {
		return true;
	}
	
	@Override
	public boolean spawn(String world, int x, int y, int z) {
		// prepare location
		World w = Bukkit.getWorld(world);
		Location loc = new Location(w, x + .5, y + .5, z + .5);
		// find old villager
		if (uuid != null && !uuid.isEmpty()) {
			Entity[] entities = loc.getChunk().getEntities();
			for (Entity e : entities) {
				if (e.getType() == getEntityType() && e.getUniqueId().toString().equalsIgnoreCase(uuid) && e.isValid()) {
					entity = (LivingEntity)e;					
					//entity.setHealth(entity.getMaxHealth());
					String name = shopkeeper.getName();
					if (name != null && !name.isEmpty()) {
						setEntityName(name);
					}
					entity.teleport(loc);
					break;
				}
			}
		}
		// spawn villager
		if (entity == null || !entity.isValid()) {
			// try to bypass entity-spawn blocking plugins:
			EntityType entityType = getEntityType();
			ShopkeepersPlugin.getInstance().forceCreatureSpawn(loc, entityType);
			entity = (LivingEntity)w.spawnEntity(loc, entityType);
			uuid = entity.getUniqueId().toString();
			this.setName(shopkeeper.getName());
		}
		if (entity != null && entity.isValid()) {
			entity.setRemoveWhenFarAway(false);
			overwriteAI();
			return true;
		} else {
			if (entity != null) {
				entity.remove();
				entity = null;
			}
			return false;
		}
	}

	@Override
	public boolean isActive() {
		return entity != null && !entity.isDead();
	}

	@Override
	public String getId() {
		if (entity != null) {
			return "entity" + entity.getEntityId();
		}
		return null;
	}
	
	@Override
	public Location getActualLocation() {
		if (entity == null || !entity.isValid()) {
			return null;
		} else {
			return entity.getLocation();
		}
	}
	
	@Override
	public void setName(String name) {
		if (entity != null && entity.isValid() && Settings.showNameplates) {
			if (name != null && !name.isEmpty()) {
				if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
					name = Settings.nameplatePrefix + name;
				}
				name = ChatColor.translateAlternateColorCodes('&', name);
				if (name.length() > 32) {
					name = name.substring(0, 32);
				}
			}
			setEntityName(name);
		}
	}
	
	private void setEntityName(String name) {
		if (name != null && !name.isEmpty()) {
			entity.setCustomName(name);
			entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
		}
	}
	
	@Override
	public void setItem(ItemStack item) {
		if (entity != null && entity.isValid()) {
			entity.getEquipment().setItemInHand(item);
			entity.getEquipment().setItemInHandDropChance(0);
		}
	}
	
	@Override
	public boolean check(String world, int x, int y, int z) {
		if (entity == null || !entity.isValid()) {
			boolean spawned = spawn(world, x, y, z);
			ShopkeepersPlugin.debug("Shopkeeper (" + world + "," + x + "," + y + "," + z + ") missing, respawn " + (spawned?"successful":"failed"));
			if (spawned) {
				respawnAttempts = 0;
				return true;
			} else {
				return (++respawnAttempts > 5);
			}
		} else {
			World w = Bukkit.getWorld(world);
			Location loc = new Location(w, x + .5, y, z + .5, entity.getLocation().getYaw(), entity.getLocation().getPitch());
			if (entity.getLocation().distanceSquared(loc) > .4) {
				entity.teleport(loc);
				overwriteAI();
				ShopkeepersPlugin.debug("Shopkeeper (" + world + "," + x + "," + y + "," + z + ") out of place, teleported back");
			}
			return false;
		}
	}

	@Override
	public void despawn() {
		if (entity != null) {
			entity.remove();
			entity.setHealth(0D);
			entity = null;
		}
	}
	
	@Override
	public void delete() {
		despawn();
	}
	
	protected void overwriteAI() {
		ShopkeepersPlugin.getVolatileCode().overwriteLivingEntityAI(entity);
	}
	
}
