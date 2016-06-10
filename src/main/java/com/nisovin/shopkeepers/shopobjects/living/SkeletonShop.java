package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

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
		switch (skeletonType.ordinal()) {
		case 2:
			// stray (MC 1.10 addition):
			ItemStack skullItem = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
			SkullMeta skull = (SkullMeta) skullItem.getItemMeta();
			skull.setOwner("MHF_Ghast");
			skullItem.setItemMeta(skull);
			return skullItem;
		case 1:
			// wither:
			return new ItemStack(Material.SKULL_ITEM, 1, (byte) 1);
		case 0:
		default:
			// normal:
			return new ItemStack(Material.SKULL_ITEM, 1, (byte) 0);
		}
	}

	@Override
	public void cycleSubType() {
		SkeletonType[] skeletonTypes = SkeletonType.values();
		int id = skeletonType.ordinal() + 1;
		if (id >= skeletonTypes.length) id = 0;
		skeletonType = skeletonTypes[id];
		this.applySubType();
	}
}
