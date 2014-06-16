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
		// nothing to do by default;
	}

	public abstract boolean needsSpawning();

	public abstract boolean spawn();

	public abstract boolean isActive();

	public abstract String getId();

	public abstract Location getActualLocation();

	// naming is done through the Shopkeeper instance
	protected abstract void setName(String name);

	protected abstract int getNameLengthLimit();

	public abstract void setItem(ItemStack item);

	public abstract boolean check();

	public abstract void despawn();

	public abstract void delete();

	public abstract ShopObjectType getObjectType();

	public abstract ItemStack getSubTypeItem();

	public abstract void cycleSubType();
}