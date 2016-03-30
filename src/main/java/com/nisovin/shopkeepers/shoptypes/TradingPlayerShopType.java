package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeepersAPI;
import com.nisovin.shopkeepers.Utils;

public class TradingPlayerShopType extends ShopType<TradingPlayerShopkeeper> {

	TradingPlayerShopType() {
		super("trade", ShopkeepersAPI.PLAYER_TRADE_PERMISSION);
	}

	@Override
	public TradingPlayerShopkeeper loadShopkeeper(ConfigurationSection config) {
		if (this.commonPreChecks(config)) {
			TradingPlayerShopkeeper shopkeeper = new TradingPlayerShopkeeper(config);
			this.registerShopkeeper(shopkeeper);
			return shopkeeper;
		}
		return null;
	}

	@Override
	public TradingPlayerShopkeeper createShopkeeper(ShopCreationData creationData) {
		if (this.commonPlayerPreChecks(creationData)) {
			TradingPlayerShopkeeper shopkeeper = new TradingPlayerShopkeeper(creationData);
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
		return Settings.msgTradeShopCreated;
	}

	@Override
	public boolean matches(String identifier) {
		if (super.matches(identifier)) return true;
		String lower = identifier.toLowerCase();
		return lower.startsWith("trad");
	}

	@Override
	public void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedTradeShop);
	}
}
