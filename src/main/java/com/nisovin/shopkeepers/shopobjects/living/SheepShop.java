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

	protected SheepShop(Shopkeeper shopkeeper, ShopCreationData creationData, LivingEntityType livingType) {
		super(shopkeeper, creationData, livingType);
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		this.color = DyeColor.getByWoolData((byte)config.getInt("color"));
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		config.set("color", this.color.getWoolData());
	}

	@Override
	public boolean spawn() {
		boolean spawned = super.spawn();
		if (spawned && this.entity != null && this.entity.isValid()) {
			assert this.entity.getType() == EntityType.SHEEP;
			((Sheep) this.entity).setColor(this.color);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.WOOL, 1, this.color.getWoolData());
	}

	@Override
	public void cycleSubType() {
		byte colorByte = this.color.getWoolData();
		colorByte += 1;
		this.color = DyeColor.getByWoolData(colorByte);
		if (this.color == null) {
			this.color = DyeColor.WHITE;
		}
		assert this.entity.getType() == EntityType.SHEEP;
		((Sheep) this.entity).setColor(this.color);
	}
}