package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;

public abstract class ShopObject {

	protected Shopkeeper shopkeeper;

	public void setShopkeeper(Shopkeeper shopkeeper) {
		this.shopkeeper = shopkeeper;
	}

	public abstract void load(ConfigurationSection config);

	public abstract void save(ConfigurationSection config);

	public abstract boolean needsSpawned();

	public abstract boolean spawn(String world, int x, int y, int z);

	public abstract boolean isActive();

	public abstract String getId();

	public abstract Location getActualLocation();

	public abstract void setName(String name);

	public abstract void setItem(ItemStack item);

	public abstract boolean check(String world, int x, int y, int z);

	public abstract void despawn();

	public abstract void delete();

	public abstract ItemStack getTypeItem();

	public abstract ShopObjectType getObjectType();

	public abstract void cycleType();

	public static ShopObject getShopObject(ConfigurationSection config) {
		String obj = config.getString("object");
		if (obj != null) {
			if (obj.equals("villager")) {
				return new VillagerShop();
			} else if (obj.equals("block")) {
				return new BlockShop();
			} else if (obj.equals("witch")) {
				return new WitchShop();
			} else if (obj.equals("creeper")) {
				return new CreeperShop();
			}
		}
		return new VillagerShop();
	}

}
