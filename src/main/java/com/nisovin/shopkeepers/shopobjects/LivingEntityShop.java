package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;

import com.nisovin.shopkeepers.compat.NMSManager;

public abstract class LivingEntityShop extends ShopObject {

	protected LivingEntity entity;
	private String uuid;
	private int respawnAttempts = 0;

	protected LivingEntityShop(Shopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public void load(ConfigurationSection config) {
		if (config.contains("uuid")) {
			uuid = config.getString("uuid");
		}
	}

	@Override
	public void save(ConfigurationSection config) {
		// let's save last known uuid nevertheless, for the case that the entity somehow wasn't properly removed before (which seems to still happen sometimes during server shutdowns)
		if (uuid != null && !uuid.isEmpty()) {
			config.set("uuid", this.uuid);
		}
	}

	protected abstract EntityType getEntityType();

	public LivingEntity getEntity() {
		return this.entity;
	}

	@Override
	public boolean needsSpawned() {
		return true;
	}

	// returns true if we find a valid entity:
	protected boolean searchOldEntity(Location location) {
		assert location != null && !this.isActive();
		if (uuid != null && !uuid.isEmpty()) {
			Entity[] entities = location.getChunk().getEntities();
			for (Entity e : entities) {
				if (e.isValid() && !e.isDead() && e.getType() == getEntityType() && e.getUniqueId().toString().equalsIgnoreCase(uuid)) {
					ShopkeepersPlugin.debug("  Found old shopkeeper entity, using it now");
					entity = (LivingEntity) e;
					// entity.setHealth(entity.getMaxHealth());
					entity.teleport(location);
					assert this.isActive(); // let's assume that the found entity is still valid since we found it
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean spawn() {
		// check if out current old entity is still valid:
		if (this.isActive()) return true;
		// prepare location:
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		Location location = new Location(world, shopkeeper.getX() + .5, shopkeeper.getY() + .5, shopkeeper.getZ() + .5);
		// find old shopkeeper entity, else spawn a new one:
		if (!searchOldEntity(location)) {
			// try to bypass entity-spawn blocking plugins:
			EntityType entityType = getEntityType();
			ShopkeepersPlugin.getInstance().forceCreatureSpawn(location, entityType);
			entity = (LivingEntity) world.spawnEntity(location, entityType);
			uuid = entity.getUniqueId().toString();
		}
		if (this.isActive()) {
			this.setName(shopkeeper.getName());
			entity.setRemoveWhenFarAway(false);
			overwriteAI();
			return true;
		} else {
			entity = null;
			return false;
		}
	}

	@Override
	public boolean isActive() {
		// validate the entity:
		return entity != null && !entity.isDead() && entity.isValid();
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
		if (this.isActive()) {
			return entity.getLocation();
		} else {
			return null;
		}
	}

	@Override
	public void setName(String name) {
		if (this.isActive()) {
			if (Settings.showNameplates && name != null && !name.isEmpty()) {
				if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
					name = Settings.nameplatePrefix + name;
				}
				name = ChatColor.translateAlternateColorCodes('&', name);
				if (name.length() > 32) {
					name = name.substring(0, 32);
				}
				// set entity name plate:
				entity.setCustomName(name);
				entity.setCustomNameVisible(Settings.alwaysShowNameplates);
			} else {
				// remove name plate:
				entity.setCustomName(null);
				entity.setCustomNameVisible(false);
			}
		}
	}

	@Override
	public void setItem(ItemStack item) {
		if (this.isActive()) {
			entity.getEquipment().setItemInHand(item);
			entity.getEquipment().setItemInHandDropChance(0);
		}
	}

	@Override
	public boolean check() {
		String worldName = shopkeeper.getWorldName();
		int x = shopkeeper.getX();
		int y = shopkeeper.getY();
		int z = shopkeeper.getZ();

		if (!this.isActive()) {
			ShopkeepersPlugin.debug("Shopkeeper (" + worldName + "," + x + "," + y + "," + z + ") missing, triggering respawn now");
			boolean silentlyUnloaded = (entity != null && !entity.isValid());
			if (silentlyUnloaded) {
				// the chunk was silently unloaded before:
				ShopkeepersPlugin.debug("  Chunk was silently unloaded before: Loading it now and requesting controlled unload");
			}
			boolean spawned = spawn(); // this will load the chunk if necessary
			if (spawned) {
				respawnAttempts = 0;
				if (silentlyUnloaded) {
					World world = Bukkit.getWorld(worldName);
					Location location = new Location(world, x + .5, y + .5, z + .5);
					Chunk chunk = location.getChunk();
					// request a safe chunk unload which will call an ChunkUnloadEvent then: (in order to not keep the chunks loaded by constantly calling of this method)
					world.unloadChunkRequest(chunk.getX(), chunk.getZ(), true); // TODO: this doesn't seem to actually call the ChunkUnloadEvent if world saving is disabled..
				}
				return true;
			} else {
				ShopkeepersPlugin.debug("  Respawn failed");
				return (++respawnAttempts > 5);
			}
		} else {
			World world = Bukkit.getWorld(worldName);
			Location loc = new Location(world, x + .5, y, z + .5, entity.getLocation().getYaw(), entity.getLocation().getPitch());
			if (entity.getLocation().distanceSquared(loc) > .4) {
				entity.teleport(loc);
				overwriteAI();
				ShopkeepersPlugin.debug("Shopkeeper (" + worldName + "," + x + "," + y + "," + z + ") out of place, teleported back");
			}
			return false;
		}
	}

	@Override
	public void despawn() {
		if (entity != null) {
			if (!entity.isValid()) {
				String worldName = shopkeeper.getWorldName();
				int x = shopkeeper.getX();
				int y = shopkeeper.getY();
				int z = shopkeeper.getZ();
				ShopkeepersPlugin.debug("Chunk was silently unloaded at (" + worldName + "," + x + "," + y + "," + z + "): Loading it now to remove old entity");
				World world = Bukkit.getWorld(worldName);
				if (world != null) {
					Location location = new Location(world, x + .5, y + .5, z + .5);
					this.searchOldEntity(location); // this will load the chunk
					// request a safe chunk unload which will call an ChunUnloadEvent then: (for now let's assume that the server can handle this automatically)
					//Chunk chunk = location.getChunk();
					//world.unloadChunkRequest(chunk.getX(), chunk.getZ(), true);
				}
			}
			entity.remove();
			entity.setHealth(0D);
			entity = null;
			//TODO chunk loading and removal doesn't seem to work during server shutdown.. :( so we are now storing the last known entity uuid
		}
	}

	@Override
	public void delete() {
		despawn();
	}

	protected void overwriteAI() {
		NMSManager.getProvider().overwriteLivingEntityAI(entity);
	}

}