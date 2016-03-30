package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeepersAPI;
import com.nisovin.shopkeepers.Utils;

public class NormalPlayerShopType extends ShopType<NormalPlayerShopkeeper> {

	NormalPlayerShopType() {
		super("player", ShopkeepersAPI.PLAYER_NORMAL_PERMISSION);
	}

	@Override
	public NormalPlayerShopkeeper loadShopkeeper(ConfigurationSection config) {
		if (this.commonPreChecks(config)) {
			NormalPlayerShopkeeper shopkeeper = new NormalPlayerShopkeeper(config);
			this.registerShopkeeper(shopkeeper);
			return shopkeeper;
		}
		return null;
	}

	@Override
	public NormalPlayerShopkeeper createShopkeeper(ShopCreationData creationData) {
		if (this.commonPlayerPreChecks(creationData)) {
			NormalPlayerShopkeeper shopkeeper = new NormalPlayerShopkeeper(creationData);
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
		return Settings.msgPlayerShopCreated;
	}

	@Override
	public boolean matches(String identifier) {
		if (super.matches(identifier)) return true;
		String lower = identifier.toLowerCase();
		return lower.startsWith("norm") || lower.startsWith("sell");
	}

	@Override
	public void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedNormalShop);
	}
}
