package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;

public class ZombieShop extends LivingEntityShop {

	private boolean villagerZombie = false;

	protected ZombieShop(Shopkeeper shopkeeper, ShopCreationData creationData, LivingEntityType livingType) {
		super(shopkeeper, creationData, livingType);
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		villagerZombie = config.getBoolean("villagerZombie");
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		config.set("villagerZombie", villagerZombie);
	}

	@Override
	public boolean spawn() {
		boolean spawned = super.spawn();
		if (spawned && entity != null && entity.isValid()) {
			this.applySubType();
			return true;
		} else {
			return false;
		}
	}

	private void applySubType() {
		if (entity == null || !entity.isValid()) return;
		assert entity.getType() == EntityType.ZOMBIE;
		((Zombie) entity).setVillager(villagerZombie);
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.MONSTER_EGG, 1, villagerZombie ? (short) 120 : (short) 54);
	}

	@Override
	public void cycleSubType() {
		villagerZombie = !villagerZombie;
		this.applySubType();
	}
}