package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeepersAPI;
import com.nisovin.shopkeepers.Utils;

public class BuyingPlayerShopType extends ShopType<BuyingPlayerShopkeeper> {

	BuyingPlayerShopType() {
		super("buy", ShopkeepersAPI.PLAYER_BUY_PERMISSION);
	}

	@Override
	public BuyingPlayerShopkeeper loadShopkeeper(ConfigurationSection config) {
		if (this.commonPreChecks(config)) {
			BuyingPlayerShopkeeper shopkeeper = new BuyingPlayerShopkeeper(config);
			this.registerShopkeeper(shopkeeper);
			return shopkeeper;
		}
		return null;
	}

	@Override
	public BuyingPlayerShopkeeper createShopkeeper(ShopCreationData creationData) {
		if (this.commonPlayerPreChecks(creationData)) {
			BuyingPlayerShopkeeper shopkeeper = new BuyingPlayerShopkeeper(creationData);
			this.registerShopkeeper(shopkeeper);
			return shopkeeper;
		}
		return null;
	}

	@Override
	public boolean isPlayerShopType() {
		return true;
	}

	@Override
	public String getCreatedMessage() {
		return Settings.msgBuyShopCreated;
	}

	@Override
	public boolean matches(String identifier) {
		if (super.matches(identifier)) return true;
		String lower = identifier.toLowerCase();
		return lower.startsWith("buy");
	}

	@Override
	public void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedBuyShop);
	}
}
