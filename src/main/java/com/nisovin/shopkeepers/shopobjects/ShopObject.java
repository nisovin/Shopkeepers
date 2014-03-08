package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Shopkeeper;

public abstract class ShopObject {

	protected final Shopkeeper shopkeeper;

	protected ShopObject(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		this.shopkeeper = shopkeeper;
	}

	public abstract void load(ConfigurationSection config);

	public abstract void save(ConfigurationSection config);

	public abstract boolean needsSpawned();

	public abstract boolean spawn();

	public abstract boolean isActive();

	public abstract String getId();

	public abstract Location getActualLocation();

	public abstract void setName(String name);

	public abstract void setItem(ItemStack item);

	public abstract boolean check();

	public abstract void despawn();

	public abstract void delete();

	public abstract ItemStack getTypeItem();

	public abstract ShopObjectType getObjectType();

	public abstract void cycleType();

}