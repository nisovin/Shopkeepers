package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Shopkeeper;

public class WitchShop extends LivingEntityShop {

	protected WitchShop(Shopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("object", "witch");
	}

	@Override
	protected EntityType getEntityType() {
		return EntityType.WITCH;
	}

	@Override
	public ItemStack getTypeItem() {
		return null;
	}

	@Override
	public ShopObjectType getObjectType() {
		return ShopObjectType.WITCH;
	}

	@Override
	public void cycleType() {
	}
}