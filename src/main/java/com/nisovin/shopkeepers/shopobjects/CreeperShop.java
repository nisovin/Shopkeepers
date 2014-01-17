package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.ShopObjectType;

public class CreeperShop extends LivingEntityShop {
	
	private boolean powered = false;
	
	@Override
	public void load(ConfigurationSection config) {
		super.load(config);
		powered = config.getBoolean("powered", false);
	}
	
	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("object", "creeper");
		config.set("powered", powered);
	}

	@Override
	protected EntityType getEntityType() {
		return EntityType.CREEPER;
	}
	
	@Override
	public boolean spawn(String world, int x, int y, int z) {
		boolean spawned = super.spawn(world, x, y, z);
		if (spawned && entity != null && entity.isValid() && entity instanceof Creeper) {
			((Creeper)entity).setPowered(powered);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ItemStack getTypeItem() {
		return new ItemStack(Material.WOOL, 1, powered ? (short)3 : (short)5);
	}

	@Override
	public ShopObjectType getObjectType() {
		return ShopObjectType.CREEPER;
	}

	@Override
	public void cycleType() {
		powered = !powered;
		if (entity != null && entity.isValid()) {
			((Creeper)entity).setPowered(powered);			
		}
	}

}
