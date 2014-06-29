package com.nisovin.shopkeepers.shopobjects.living;

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

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.compat.NMSManager;

import org.bukkit.metadata.FixedMetadataValue;

public class LivingEntityShop extends ShopObject {

	protected final LivingEntityType livingType;
	protected LivingEntity entity;
	private String uuid;
	private int respawnAttempts = 0;

	protected LivingEntityShop(Shopkeeper shopkeeper, ShopCreationData creationData, LivingEntityType livingType) {
		super(shopkeeper, creationData);
		this.livingType = livingType;
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		if (config.contains("uuid")) {
			this.uuid = config.getString("uuid");
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		// let's save last known uuid nevertheless, for the case that the entity somehow wasn't properly removed before (which seems to still happen sometimes during server shutdowns)
		if (this.uuid != null && !this.uuid.isEmpty()) {
			config.set("uuid", this.uuid);
		}
	}

	public EntityType getEntityType() {
		return this.livingType.getEntityType();
	}

	public LivingEntity getEntity() {
		return this.entity;
	}

	@Override
	public boolean needsSpawning() {
		return true;
	}

	protected void assignShopkeeperMetadata(LivingEntity entity) {
		entity.setMetadata("shopkeeper", new FixedMetadataValue(ShopkeepersPlugin.getInstance(), true));
	}

	protected void removeShopkeeperMetadata(LivingEntity entity) {
		entity.removeMetadata("shopkeeper", ShopkeepersPlugin.getInstance());
	}

	// returns true if we find a valid entity:
	protected boolean searchOldEntity(Location location) {
		assert location != null && !this.isActive();
		if (this.uuid != null && !this.uuid.isEmpty()) {
			Entity[] entities = location.getChunk().getEntities();
			for (Entity e : entities) {
				if (e.isValid() && !e.isDead() && e.getType() == getEntityType() && e.getUniqueId().toString().equalsIgnoreCase(this.uuid)) {
					Log.debug("  Found old shopkeeper entity, using it now");
					this.entity = (LivingEntity) e;
					// entity.setHealth(entity.getMaxHealth());
					this.entity.teleport(location);
					assert this.isActive(); // let's assume that the found entity is still valid since we found it
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean spawn() {
		// check if our current old entity is still valid:
		if (this.isActive()) return true;
		if (this.entity != null) {
			// clean up metadata before replacing the currently stored entity with a new one:
			this.removeShopkeeperMetadata(this.entity);
		}
		// prepare location:
		World world = Bukkit.getWorld(this.shopkeeper.getWorldName());
		Location location = new Location(world, this.shopkeeper.getX() + .5, this.shopkeeper.getY() + .5, this.shopkeeper.getZ() + .5);
		// find old shopkeeper entity, else spawn a new one:
		if (!searchOldEntity(location)) {
			// try to bypass entity-spawn blocking plugins:
			EntityType entityType = this.getEntityType();
			ShopkeepersPlugin.getInstance().forceCreatureSpawn(location, entityType);
			this.entity = (LivingEntity) world.spawnEntity(location, entityType);
			this.uuid = this.entity.getUniqueId().toString();
		}
		if (this.isActive()) {
			// assign metadata for easy identification by other plugins:
			this.assignShopkeeperMetadata(this.entity);
			this.setName(this.shopkeeper.getName());
			this.entity.setRemoveWhenFarAway(false);
			overwriteAI();
			return true;
		} else {
			this.entity = null;
			return false;
		}
	}

	@Override
	public boolean isActive() {
		// validate the entity:
		return this.entity != null && !this.entity.isDead() && this.entity.isValid();
	}

	@Override
	public String getId() {
		if (this.entity != null) {
			return "entity" + this.entity.getEntityId();
		}
		return null;
	}

	@Override
	public Location getActualLocation() {
		if (this.isActive()) {
			return this.entity.getLocation();
		} else {
			return null;
		}
	}

	@Override
	public void setName(String name) {
		if (!this.isActive()) return;
		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
				name = Settings.nameplatePrefix + name;
			}
			name = ChatColor.translateAlternateColorCodes('&', name);
			name = this.trimToNameLength(name);
			// set entity name plate:
			this.entity.setCustomName(name);
			this.entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// remove name plate:
			this.entity.setCustomName(null);
			this.entity.setCustomNameVisible(false);
		}
	}

	@Override
	public int getNameLengthLimit() {
		return 32;
	}

	@Override
	public void setItem(ItemStack item) {
		if (this.isActive()) {
			this.entity.getEquipment().setItemInHand(item);
			this.entity.getEquipment().setItemInHandDropChance(0);
		}
	}

	@Override
	public boolean check() {
		String worldName = this.shopkeeper.getWorldName();
		int x = this.shopkeeper.getX();
		int y = this.shopkeeper.getY();
		int z = this.shopkeeper.getZ();

		if (!this.isActive()) {
			Log.debug("Shopkeeper (" + worldName + "," + x + "," + y + "," + z + ") missing, triggering respawn now");
			boolean silentlyUnloaded = (this.entity != null && !this.entity.isValid());
			if (silentlyUnloaded) {
				// the chunk was silently unloaded before:
				Log.debug("  Chunk was silently unloaded before: Loading it now and requesting controlled unload");
			}
			boolean spawned = this.spawn(); // this will load the chunk if necessary
			if (spawned) {
				this.respawnAttempts = 0;
				if (silentlyUnloaded) {
					World world = Bukkit.getWorld(worldName);
					Location location = new Location(world, x + .5, y + .5, z + .5);
					Chunk chunk = location.getChunk();
					// request a safe chunk unload which will call an ChunkUnloadEvent then: (in order to not keep the chunks loaded by constantly calling of this method)
					world.unloadChunkRequest(chunk.getX(), chunk.getZ(), true); // TODO: this doesn't seem to actually call the ChunkUnloadEvent if world saving is disabled..
				}
				return true;
			} else {
				Log.debug("  Respawn failed");
				return (++this.respawnAttempts > 5);
			}
		} else {
			World world = Bukkit.getWorld(worldName);
			Location loc = new Location(world, x + .5, y, z + .5, this.entity.getLocation().getYaw(), this.entity.getLocation().getPitch());
			if (this.entity.getLocation().distanceSquared(loc) > .4) {
				this.entity.teleport(loc);
				overwriteAI();
				Log.debug("Shopkeeper (" + worldName + "," + x + "," + y + "," + z + ") out of place, teleported back");
			}
			return false;
		}
	}

	@Override
	public void despawn() {
		if (this.entity != null) {
			if (!this.entity.isValid()) {
				String worldName = shopkeeper.getWorldName();
				int x = this.shopkeeper.getX();
				int y = this.shopkeeper.getY();
				int z = this.shopkeeper.getZ();
				Log.debug("Chunk was silently unloaded at (" + worldName + "," + x + "," + y + "," + z + "): Loading it now to remove old entity");
				World world = Bukkit.getWorld(worldName);
				if (world != null) {
					Location location = new Location(world, x + .5, y + .5, z + .5);
					this.searchOldEntity(location); // this will load the chunk
					// request a safe chunk unload which will call an ChunUnloadEvent then: (for now let's assume that the server can handle this automatically)
					// Chunk chunk = location.getChunk();
					// world.unloadChunkRequest(chunk.getX(), chunk.getZ(), true);
				}
			}
			this.removeShopkeeperMetadata(this.entity);
			this.entity.remove();
			this.entity.setHealth(0D);
			this.entity = null;
			// TODO chunk loading and removal doesn't seem to work during server shutdown.. :( so we are now storing the last known entity uuid
		}
	}

	@Override
	public void delete() {
		this.despawn();
	}

	protected void overwriteAI() {
		NMSManager.getProvider().overwriteLivingEntityAI(this.entity);
	}

	@Override
	public ShopObjectType getObjectType() {
		return this.livingType.getObjectType();
	}

	@Override
	public ItemStack getSubTypeItem() {
		// no sub types by default
		return null;
	}

	@Override
	public void cycleSubType() {
		// no sub types to cacle through by default
	}
}