package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;

public class CreeperShop extends LivingEntityShop {

	private boolean powered = false;

	protected CreeperShop(Shopkeeper shopkeeper, ShopCreationData creationData, LivingEntityType livingType) {
		super(shopkeeper, creationData, livingType);
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		powered = config.getBoolean("powered", false);
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		config.set("powered", powered);
	}

	@Override
	public boolean spawn() {
		boolean spawned = super.spawn();
		if (spawned && entity != null && entity.isValid()) {
			assert entity.getType() == EntityType.CREEPER;
			((Creeper) entity).setPowered(powered);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.WOOL, 1, powered ? (short) 3 : (short) 5);
	}

	@Override
	public void cycleSubType() {
		powered = !powered;
		if (entity != null && entity.isValid()) {
			((Creeper) entity).setPowered(powered);
		}
	}
}