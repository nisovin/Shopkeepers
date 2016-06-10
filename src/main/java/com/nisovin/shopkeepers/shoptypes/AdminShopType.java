package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.ShopkeepersAPI;

public class AdminShopType extends ShopType<AdminShopkeeper> {

	AdminShopType() {
		super("admin", ShopkeepersAPI.ADMIN_PERMISSION);
	}

	@Override
	public AdminShopkeeper loadShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.commonPreChecks(config);
		AdminShopkeeper shopkeeper = new AdminShopkeeper(config);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
	}

	@Override
	public AdminShopkeeper createShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		this.commonPreChecks(creationData);
		AdminShopkeeper shopkeeper = new AdminShopkeeper(creationData);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
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
