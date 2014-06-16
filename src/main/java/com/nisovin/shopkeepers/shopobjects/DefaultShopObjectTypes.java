package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityType;

public class DefaultShopObjectTypes {

	public static List<ShopObjectType> getAll() {
		List<ShopObjectType> defaults = new ArrayList<ShopObjectType>();
		for (LivingEntityType livingType : LivingEntityType.values()) {
			defaults.add(livingType.getObjectType());
		}
		defaults.add(SIGN);
		defaults.add(CITIZEN);
		return defaults;
	}

	// DEFAULT SHOP TYPES:
	// TODO maybe change permissions to 'shopkeeper.object.<type>'?

	// SIGN
	public static final ShopObjectType SIGN = new ShopObjectType("block", "shopkeeper.sign") {

		@Override
		protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
			return new BlockShop(shopkeeper, creationData);
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
		public boolean activateByChunk() {
			return false; // TODO maybe cleanup the shop signs on chunk unload in the future?
		}
	};

	// CITIZEN
	public static final ShopObjectType CITIZEN = new ShopObjectType("citizen", "shopkeeper.citizen") {

		@Override
		protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
			return new CitizensShop(shopkeeper, creationData);
		}

		@Override
		public boolean isEnabled() {
			return Settings.enableCitizenShops && CitizensHandler.isEnabled();
		}

		@Override
		public boolean matches(String identifier) {
			if (super.matches(identifier)) return true;
			String lower = identifier.toLowerCase();
			return lower.startsWith("citizen") || lower.startsWith("npc");
		}

		@Override
		public void onSelect(Player player) {
			Utils.sendMessage(player, Settings.msgSelectedCitizenShop);
		}

		@Override
		public boolean activateByChunk() {
			return false; // spawning and despawning is handled by citizens
		}
	};
}