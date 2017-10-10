package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.Shopkeeper;

public class SheepShop extends LivingEntityShop {

	private DyeColor color = DyeColor.WHITE; // default white

	protected SheepShop(Shopkeeper shopkeeper, ShopCreationData creationData, LivingEntityObjectType livingObjectType) {
		super(shopkeeper, creationData, livingObjectType);
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		this.color = DyeColor.getByWoolData((byte) config.getInt("color"));
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		config.set("color", color.getWoolData());
	}

	@Override
	protected void applySubType() {
		super.applySubType();
		if (!this.isActive()) return;
		assert entity.getType() == EntityType.SHEEP;
		((Sheep) entity).setColor(color);
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.WOOL, 1, color.getWoolData());
	}

	@Override
	public void cycleSubType() {
		byte colorByte = color.getWoolData();
		colorByte += 1;
		color = DyeColor.getByWoolData(colorByte);
		if (color == null) {
			color = DyeColor.WHITE;
		}
		this.applySubType();
	}
}
