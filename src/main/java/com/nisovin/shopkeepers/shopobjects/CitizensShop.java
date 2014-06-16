package com.nisovin.shopkeepers.shopobjects;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

public class CitizensShop extends ShopObject {

	private Integer npcId = null;

	protected CitizensShop(Shopkeeper shopkeeper, ShopCreationData creationData) {
		super(shopkeeper, creationData);
		this.npcId = creationData.npcId; // can be null, currently only used for NPC shopkeepers created by the shopkeeper trait
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		if (config.contains("npcId")) {
			this.npcId = config.getInt("npcId");
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		if (this.npcId != null) {
			config.set("npcId", this.npcId);
		}
	}

	@Override
	protected void onInit() {
		if (this.isActive()) return;
		if (!CitizensHandler.isEnabled()) return;

		EntityType entityType;
		String name;
		if (this.shopkeeper instanceof PlayerShopkeeper) {
			// player shops will use a player npc:
			entityType = EntityType.PLAYER;
			name = ((PlayerShopkeeper) this.shopkeeper).getOwnerName();
		} else {
			entityType = EntityType.VILLAGER;
			name = "Shopkeeper";
		}
		this.npcId = CitizensHandler.createNPC(this.shopkeeper.getLocation(), entityType, name);
	}

	@Override
	public boolean needsSpawning() {
		return false; // handled by citizens
	}

	public void setNPCId(Integer npcId) {
		this.npcId = npcId;
	}

	/*
	 * @Override
	 * public boolean attach(LivingEntity entity) {
	 * NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
	 * if (npc == null) return false;
	 * this.npcId = npc.getId();
	 * return true;
	 * }
	 */

	@Override
	public boolean spawn() {
		return false; // handled by citizens
	}

	@Override
	public boolean isActive() {
		return this.npcId != null;
	}

	@Override
	public String getId() {
		return this.npcId == null ? null : "NPC-" + this.npcId;
	}

	public NPC getNPC() {
		if (this.npcId == null) return null;
		return CitizensAPI.getNPCRegistry().getById(this.npcId);
	}

	public LivingEntity getLivingEntity() {
		NPC npc = this.getNPC();
		return npc != null ? npc.getBukkitEntity() : null;
	}

	@Override
	public Location getActualLocation() {
		LivingEntity entity = this.getLivingEntity();
		return entity != null ? entity.getLocation() : null;
	}

	@Override
	protected void setName(String name) {
		if (!this.isActive()) return;
		NPC npc = this.getNPC();
		assert npc != null;
		if (Settings.showNameplates && name != null && !name.isEmpty()) {
			if (Settings.nameplatePrefix != null && !Settings.nameplatePrefix.isEmpty()) {
				name = Settings.nameplatePrefix + name;
			}
			name = ChatColor.translateAlternateColorCodes('&', name); // this shouldn't increase name length
			assert name.length() <= this.getNameLengthLimit(); // this should already be checked by the shopkeeper
			// set entity name plate:
			npc.setName(name);
			// this.entity.setCustomNameVisible(Settings.alwaysShowNameplates);
		} else {
			// remove name plate:
			npc.setName(null);
			// this.entity.setCustomNameVisible(false);
		}
	}

	@Override
	protected int getNameLengthLimit() {
		return 32; // TODO citizens seem to have different limits depending on mob type (16 for mobs, 64 for players)
	}

	@Override
	public void setItem(ItemStack item) {
		// TODO: No Citizens API for equipping items?
	}

	@Override
	public boolean check() {
		String worldName = this.shopkeeper.getWorldName();
		int x = this.shopkeeper.getX();
		int y = this.shopkeeper.getY();
		int z = this.shopkeeper.getZ();

		if (this.isActive()) {
			World world = Bukkit.getWorld(worldName);
			NPC npc = this.getNPC();
			assert npc != null;

			// Not going to force Citizens creation, this seems like it could go really wrong.
			if (npc != null) {
				Location currentLocation = npc.getStoredLocation();
				Location loc = new Location(world, x + .5, y, z + .5);
				if (currentLocation == null) {
					npc.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
					Log.debug("Shopkeeper NPC (" + worldName + "," + x + "," + y + "," + z + ") had no location, teleported");
				} else if (!currentLocation.getWorld().equals(loc.getWorld()) || currentLocation.distanceSquared(loc) > 1) {
					this.shopkeeper.setLocation(currentLocation);
					Log.debug("Shopkeeper NPC (" + worldName + "," + x + "," + y + "," + z + ") out of place, re-indexing");
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void despawn() {
		// handled by citizens
	}

	@Override
	public void delete() {
		if (this.isActive()) {
			NPC npc = this.getNPC();
			if (npc.hasTrait(CitizensShopkeeperTrait.class)) {
				npc.removeTrait(CitizensShopkeeperTrait.class);
			} else {
				npc.destroy();
			}
		}
		this.npcId = null;
	}

	@Override
	public ItemStack getSubTypeItem() {
		// TODO: A menu of entity types here would be cool
		return null;
	}

	@Override
	public ShopObjectType getObjectType() {
		return DefaultShopObjectTypes.CITIZEN;
	}

	@Override
	public void cycleSubType() {
	}
}