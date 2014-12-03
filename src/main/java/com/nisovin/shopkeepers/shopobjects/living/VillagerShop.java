package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.compat.NMSManager;

public class VillagerShop extends LivingEntityShop {

	private int profession;

	protected VillagerShop(Shopkeeper shopkeeper, ShopCreationData creationData, LivingEntityType livingType) {
		super(shopkeeper, creationData, livingType);
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		this.profession = Math.max(config.getInt("prof"), NMSManager.getProvider().getMaxVillagerProfession());
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		config.set("prof", profession);
	}

	@Override
	public boolean spawn() {
		boolean spawned = super.spawn();
		if (spawned && entity != null && entity.isValid()) {
			assert entity.getType() == EntityType.VILLAGER;
			NMSManager.getProvider().setVillagerProfession((Villager) entity, profession);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.WOOL, 1, getProfessionWoolColor());
	}

	@Override
	public void cycleSubType() {
		profession += 1;
		if (profession > NMSManager.getProvider().getMaxVillagerProfession()) profession = 0;
		assert entity.getType() == EntityType.VILLAGER;
		NMSManager.getProvider().setVillagerProfession((Villager) entity, profession);
	}

	private short getProfessionWoolColor() {
		switch (profession) {
		case 0:
			return DyeColor.BROWN.getWoolData();
		case 1:
			return DyeColor.WHITE.getWoolData();
		case 2:
			return DyeColor.MAGENTA.getWoolData();
		case 3:
			return DyeColor.GRAY.getWoolData();
		case 4:
			return DyeColor.SILVER.getWoolData();
		case 5:
			return DyeColor.LIME.getWoolData(); // pre MC 1.8
		default:
			return DyeColor.RED.getWoolData(); // unknown profession
		}
	}

	@Override
	protected void overwriteAI() {
		NMSManager.getProvider().overwriteVillagerAI(entity);
		if (Settings.silenceLivingShopEntities) NMSManager.getProvider().setEntitySilent(entity, true);
	}
}