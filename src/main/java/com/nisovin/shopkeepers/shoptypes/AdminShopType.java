package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeepersAPI;

public class AdminShopType extends ShopType<AdminShopkeeper> {

	AdminShopType() {
		super("admin", ShopkeepersAPI.ADMIN_PERMISSION);
	}

	@Override
	public AdminShopkeeper loadShopkeeper(ConfigurationSection config) {
		if (this.commonPreChecks(config)) {
			AdminShopkeeper shopkeeper = new AdminShopkeeper(config);
			this.registerShopkeeper(shopkeeper);
			return shopkeeper;
		}
		return null;
	}

	@Override
	public AdminShopkeeper createShopkeeper(ShopCreationData creationData) {
		if (this.commonPreChecks(creationData)) {
			AdminShopkeeper shopkeeper = new AdminShopkeeper(creationData);
			this.registerShopkeeper(shopkeeper);
			return shopkeeper;
		}
		return null;
	}

	@Override
	public boolean isPlayerShopType() {
		return false;
	}

	@Override
	public String getCreatedMessage() {
		return Settings.msgAdminShopCreated;
	}

	@Override
	public void onSelect(Player player) {
		// currently can't be 'selected'
	}
}
