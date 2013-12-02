package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.ShopkeepersPlugin;

public class VillagerShop extends LivingEntityShop {

	private int profession;
	
	@Override
	public void load(ConfigurationSection config) {
		super.load(config);
		profession = config.getInt("prof");
	}

	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("prof", profession);
		config.set("object", "villager");
	}

	@Override
	protected EntityType getEntityType() {
		return EntityType.VILLAGER;
	}
	
	@Override
	public boolean spawn(String world, int x, int y, int z) {
		boolean spawned = super.spawn(world, x, y, z);
		if (spawned && entity != null && entity.isValid() && entity.getType() == EntityType.VILLAGER) {
			ShopkeepersPlugin.getVolatileCode().setVillagerProfession((Villager)entity, profession);
			((Villager)entity).setBreed(false);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ItemStack getTypeItem() {
		return new ItemStack(Material.WOOL, 1, getProfessionWoolColor());
	}

	@Override
	public void cycleType() {
		profession += 1;
		if (profession > 5) profession = 0;
		if (entity instanceof Villager) {
			ShopkeepersPlugin.getVolatileCode().setVillagerProfession((Villager)entity, profession);
		}
	}

	private short getProfessionWoolColor() {
		switch (profession) {
		case 0: return 12;
		case 1: return 0;
		case 2: return 2;
		case 3: return 7;
		case 4: return 8;
		case 5: return 5;
		default: return 14;
		}
	}
	
	@Override
	protected void overwriteAI() {
		ShopkeepersPlugin.getVolatileCode().overwriteVillagerAI(entity);
	}

}
