package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.compat.NMSManager;

public class LivingEntityShop extends ShopObject {

	public static String getId(Entity entity) {
		if (entity != null) {
			return "entity" + entity.getEntityId();
		}
		return null;
	}

	protected final LivingEntityType livingType;
	protected LivingEntity entity;
	private String uuid;
	private int respawnAttempts = 0;

	protected LivingEntityShop(Shopkeeper shopkeeper, ShopCreationData creationData, LivingEntityType livingType) {
		super(shopkeeper, creationData);
		this.livingType = livingType;
	}

	@Override
	public ShopObjectType getObjectType() {
		return livingType.getObjectType();
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
		// let's save last known uuid nevertheless, for the case that the entity somehow wasn't properly removed before
		// (which seems to still happen sometimes during server shutdowns)
		if (uuid != null && !uuid.isEmpty()) {
			config.set("uuid", uuid);
		}
	}

	public EntityType getEntityType() {
		return livingType.getEntityType();
	}

	public LivingEntity getEntity() {
		return entity;
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
		if (uuid != null && !uuid.isEmpty()) {
			for (Entity e : location.getChunk().getEntities()) {
				if (e.isValid() && !e.isDead() && e.getType() == this.getEntityType() && e.getUniqueId().toString().equalsIgnoreCase(uuid)) {
					Log.debug("  Found old shopkeeper entity, using it now");
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
		// check if our current old entity is still valid:
		if (this.isActive()) return true;
		if (entity != null) {
			// clean up metadata before replacing the currently stored entity with a new one:
			this.removeShopkeeperMetadata(entity);
		}
		// prepare location:
		World world = Bukkit.getWorld(shopkeeper.getWorldName());
		Location location = new Location(world, shopkeeper.getX() + 0.5D, shopkeeper.getY() + 0.5D, shopkeeper.getZ() + 0.5D);
		// find old shopkeeper entity, else spawn a new one:
		if (!this.searchOldEntity(location)) {
			// TODO check if the block is passable before spawning there?
			// try to bypass entity-spawn blocking plugins:
			EntityType entityType = this.getEntityType();
			ShopkeepersPlugin.getInstance().forceCreatureSpawn(location, entityType);
			entity = (LivingEntity) world.spawnEntity(location, entityType);
			uuid = entity.getUniqueId().toString();
		}
		if (this.isActive()) {
			// assign metadata for easy identification by other plugins:
			this.assignShopkeeperMetadata(entity);
			this.setName(shopkeeper.getName());

			// configure some entity attributes:
			entity.setRemoveWhenFarAway(false);
			entity.setCanPickupItems(false);

			// disable breeding:
			if (entity instanceof Ageable) {
				Ageable ageable = ((Ageable) entity);
				ageable.setBreed(false);
				ageable.setAgeLock(true);
			}

			// remove potion effects:
			for (PotionEffect potionEffect : entity.getActivePotionEffects()) {
				entity.removePotionEffect(potionEffect.getType());
			}

			this.overwriteAI();
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
		return getId(entity);
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
		if (!this.isActive()) return;
		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
				name = Settings.nameplatePrefix + name;
			}
			name = this.trimToNameLength(name);
			// set entity name plate:
			entity.setCustomName(name);
			entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// remove name plate:
			entity.setCustomName(null);
			entity.setCustomNameVisible(false);
		}
	}

	@Override
	public int getNameLengthLimit() {
		return 32;
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
			Log.debug("Shopkeeper (" + worldName + "," + x + "," + y + "," + z + ") missing, triggering respawn now");
			boolean silentlyUnloaded = (entity != null && !entity.isValid());
			if (silentlyUnloaded) {
				// the chunk was silently unloaded before:
				Log.debug("  Chunk was silently unloaded before: Loading it now and requesting controlled unload");
			}
			boolean spawned = this.spawn(); // this will load the chunk if necessary
			if (spawned) {
				respawnAttempts = 0;
				if (silentlyUnloaded) {
					World world = Bukkit.getWorld(worldName);
					Location location = new Location(world, x + 0.5D, y + 0.5D, z + 0.5D);
					Chunk chunk = location.getChunk();
					// request a safe chunk unload which will call an ChunkUnloadEvent then: (in order to not keep the
					// chunks loaded by constantly calling of this method)
					world.unloadChunkRequest(chunk.getX(), chunk.getZ(), true); // TODO: this doesn't seem to actually
																				// call the ChunkUnloadEvent if world
																				// saving is disabled..
				}
				return true;
			} else {
				// TODO maybe add a setting to remove shopkeeper if it can't be spawned a certain amount of times?
				Log.debug("  Respawn failed");
				return (++respawnAttempts > 5);
			}
		} else {
			// teleport back:
			World world = Bukkit.getWorld(worldName);
			Location loc = new Location(world, x + 0.5D, y, z + 0.5D, entity.getLocation().getYaw(), entity.getLocation().getPitch());
			if (entity.getLocation().distanceSquared(loc) > 0.4D) {
				entity.teleport(loc);
				this.overwriteAI();
				Log.debug("Shopkeeper (" + worldName + "," + x + "," + y + "," + z + ") out of place, teleported back");
			}

			// remove potion effects:
			for (PotionEffect potionEffect : entity.getActivePotionEffects()) {
				entity.removePotionEffect(potionEffect.getType());
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
				Log.debug("Chunk was silently unloaded at (" + worldName + "," + x + "," + y + "," + z + "): Loading it now to remove old entity");
				World world = Bukkit.getWorld(worldName);
				if (world != null) {
					Location location = new Location(world, x + 0.5D, y + 0.5D, z + 0.5D);
					this.searchOldEntity(location); // this will load the chunk
					// request a safe chunk unload which will call an ChunkUnloadEvent then: (for now let's assume that
					// the server can handle this automatically)
					// Chunk chunk = location.getChunk();
					// world.unloadChunkRequest(chunk.getX(), chunk.getZ(), true);
				}
			}
			this.removeShopkeeperMetadata(entity);
			entity.remove();
			entity = null;
			// TODO chunk loading and removal doesn't seem to work during server shutdown.. :( so we are now storing the
			// last known entity uuid
		}
	}

	@Override
	public void delete() {
		this.despawn();
	}

	protected void overwriteAI() {
		NMSManager.getProvider().overwriteLivingEntityAI(entity);
		if (Settings.silenceLivingShopEntities) {
			NMSManager.getProvider().setEntitySilent(entity, true);
		}

		// set the NoAI tag for certain entity types:
		switch (livingType) {
		case BAT:
		case ENDER_DRAGON:
		case ENDERMAN:
		case WITHER:
		case SILVERFISH:
		case BLAZE:
			NMSManager.getProvider().setNoAI(entity);
			break;
		default:
			break;
		}
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