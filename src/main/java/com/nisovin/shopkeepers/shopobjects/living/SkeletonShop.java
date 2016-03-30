package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;

public class SkeletonShop extends LivingEntityShop {

	private SkeletonType skeletonType = SkeletonType.NORMAL;

	protected SkeletonShop(Shopkeeper shopkeeper, ShopCreationData creationData, LivingEntityObjectType livingObjectType) {
		super(shopkeeper, creationData, livingObjectType);
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		String skeletonTypeName = config.getString("skeletonType");
		try {
			skeletonType = SkeletonType.valueOf(skeletonTypeName);
		} catch (Exception e) {
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		config.set("skeletonType", skeletonType.name());
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
		assert entity.getType() == EntityType.SKELETON;
		((Skeleton) entity).setSkeletonType(skeletonType);
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.SKULL_ITEM, 1, this.getSkullItemData(skeletonType));
	}

	@Override
	public void cycleSubType() {
		int id = skeletonType.getId();
		skeletonType = SkeletonType.getType(++id);
		if (skeletonType == null) {
			skeletonType = SkeletonType.NORMAL; // id 0
		}
		this.applySubType();
	}

	private short getSkullItemData(SkeletonType skeletonType) {
		switch (skeletonType) {
		case WITHER:
			return 1;

		case NORMAL:
		default:
			return 0;
		}
	}
}
