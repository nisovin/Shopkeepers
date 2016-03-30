package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.Utils;

public class SignShopObjectType extends ShopObjectType {

	SignShopObjectType() {
		super("block", "shopkeeper.sign");
	}

	@Override
	protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
		return new SignShop(shopkeeper, creationData);
	}

	@Override
	public boolean isEnabled() {
		return Settings.enableSignShops;
	}

	@Override
	public boolean matches(String identifier) {
		if (super.matches(identifier)) return true;
		String lower = identifier.toLowerCase();
		return lower.startsWith("sign");
	}

	@Override
	public void onSelect(Player player) {
		Utils.sendMessage(player, Settings.msgSelectedSignShop);
	}

	@Override
	public boolean needsSpawning() {
		return false; // TODO maybe cleanup the shop signs on chunk unload in the future?
	}
}
