package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;

public abstract class ShopObject {

	protected final Shopkeeper shopkeeper;

	protected ShopObject(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		this.shopkeeper = shopkeeper;
	}

	public abstract void load(ConfigurationSection config);

	public void save(ConfigurationSection config) {
		config.set("object", this.getObjectType().getIdentifier());
	}

	public abstract boolean needsSpawning();

	public abstract boolean spawn();

	public abstract boolean isActive();

	public abstract String getId();

	public abstract Location getActualLocation();

	public abstract void setName(String name);

	public abstract void setItem(ItemStack item);

	public abstract boolean check();

	public abstract void despawn();

	public abstract void delete();

	public abstract ShopObjectType getObjectType();

	public abstract ItemStack getSubTypeItem();

	public abstract void cycleSubType();

}