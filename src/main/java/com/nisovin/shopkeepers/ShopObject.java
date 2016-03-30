package com.nisovin.shopkeepers;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public abstract class ShopObject {

	protected final Shopkeeper shopkeeper;

	// fresh creation
	protected ShopObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
		assert shopkeeper != null;
		this.shopkeeper = shopkeeper;
	}

	public abstract ShopObjectType getObjectType();

	protected void load(ConfigurationSection config) {
		// nothing to load by default
	}

	protected void save(ConfigurationSection config) {
		config.set("object", this.getObjectType().getIdentifier());
	}

	/**
	 * Called after the ShopObject and the Shopkeeper was fully created and loaded.
	 * Called before the underlying shopkeeper gets registered.
	 * Ideal to initialize any remaining things, like creating the citizens npc
	 * for citizens shopkeepers, if none was loaded before / is existent.
	 */
	protected void onInit() {
		// nothing to do by default
	}

	protected void onChunkLoad() {
		// nothing by default
	}

	protected void onChunkUnload() {
		// nothing by default
	}

	public abstract boolean spawn();

	public abstract boolean isActive();

	/**
	 * Gets an unique id for this shop object. This id can change when the shop object (ex. shop entity) gets respawned.
	 * 
	 * @return the shop object id, or null if the shopkeeper is currently not active
	 */
	public abstract String getId();

	public abstract Location getActualLocation();

	// naming is done through the Shopkeeper instance
	public abstract void setName(String name);

	public abstract int getNameLengthLimit();

	protected String trimToNameLength(String name) {
		if (name == null) return null;
		int lengthLimit = this.getNameLengthLimit();
		if (name.length() > lengthLimit) name = name.substring(0, lengthLimit);
		return name;
	}

	public abstract void setItem(ItemStack item);

	/**
	 * This is periodically called for active shopkeepers.
	 * It makes sure that everything is still alright with the shop object.
	 * Ex: Attempts to respawn shop entities, teleports them back into place, informs about their removal.
	 * 
	 * @return true, if the shopkeeper needs to be removed or freshly added to the active shopkeepers
	 */
	public abstract boolean check();

	public abstract void despawn();

	public abstract void delete();

	public abstract ItemStack getSubTypeItem();

	public abstract void cycleSubType();
}
