package com.nisovin.shopkeepers.shoptypes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.ShopkeepersAPI;
import com.nisovin.shopkeepers.Utils;

public class TradingPlayerShopType extends ShopType<TradingPlayerShopkeeper> {

	TradingPlayerShopType() {
		super("trade", ShopkeepersAPI.PLAYER_TRADE_PERMISSION);
	}

	@Override
	public TradingPlayerShopkeeper loadShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.commonPreChecks(config);
		TradingPlayerShopkeeper shopkeeper = new TradingPlayerShopkeeper(config);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
	}

	@Override
	public TradingPlayerShopkeeper createShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		this.commonPlayerPreChecks(creationData);
		TradingPlayerShopkeeper shopkeeper = new TradingPlayerShopkeeper(creationData);
		this.registerShopkeeper(shopkeeper);
		return shopkeeper;
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
		identifier = Utils.normalize(identifier);
		if (super.matches(identifier)) return true;
		return identifier.startsWith("trad");
	}

	@Override
	public void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedTradeShop);
	}
}
