package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityType;

public class DefaultShopObjectTypes {

	public static List<ShopObjectType> getAll() {
		List<ShopObjectType> defaults = new ArrayList<ShopObjectType>();
		//defaults.add(VILLAGER);
		//defaults.add(WITCH);
		//defaults.add(CREEPER);
		for (LivingEntityType livingType : LivingEntityType.values()) {
			defaults.add(livingType.getObjectType());
		}
		defaults.add(SIGN);
		//defaults.add(CITIZEN);
		return defaults;
	}

	// DEFAULT SHOP TYPES:
	// TODO maybe change permissions to 'shopkeeper.object.<type>'?

	// SIGN
	public static final ShopObjectType SIGN = new ShopObjectType("block", "shopkeeper.sign") {

		@Override
		public boolean isLivingEntityType() {
			return false;
		}

		@Override
		protected ShopObject createObject(Shopkeeper shopkeeper) {
			return new BlockShop(shopkeeper);
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
	};

	// VILLAGER
	/*public static final ShopObjectType VILLAGER = new ShopObjectType("villager", "shopkeeper.villager") {

		@Override
		public boolean isLivingEntityType() {
			return true;
		}

		@Override
		protected ShopObject createObject(Shopkeeper shopkeeper) {
			return new VillagerShop(shopkeeper);
		}

		@Override
		public boolean isEnabled() {
			return Settings.enableVillagerShops;
		}

		@Override
		public boolean matches(String identifier) {
			if (super.matches(identifier)) return true;
			String lower = identifier.toLowerCase();
			return lower.startsWith("villager");
		}

		@Override
		public void onSelect(Player player) {
			Utils.sendMessage(player, Settings.msgSelectedVillagerShop);
		}
	};

	// WITCH
	public static final ShopObjectType WITCH = new ShopObjectType("witch", "shopkeeper.witch") {

		@Override
		public boolean isLivingEntityType() {
			return true;
		}

		@Override
		protected ShopObject createObject(Shopkeeper shopkeeper) {
			return new WitchShop(shopkeeper);
		}

		@Override
		public boolean isEnabled() {
			return Settings.enableWitchShops;
		}

		@Override
		public boolean matches(String identifier) {
			if (super.matches(identifier)) return true;
			String lower = identifier.toLowerCase();
			return lower.startsWith("witch");
		}

		@Override
		public void onSelect(Player player) {
			Utils.sendMessage(player, Settings.msgSelectedWitchShop);
		}
	};

	// CREEPER
	public static final ShopObjectType CREEPER = new ShopObjectType("creeper", "shopkeeper.creeper") {

		@Override
		public boolean isLivingEntityType() {
			return true;
		}

		@Override
		protected ShopObject createObject(Shopkeeper shopkeeper) {
			return new CreeperShop(shopkeeper);
		}

		@Override
		public boolean isEnabled() {
			return Settings.enableCreeperShops;
		}

		@Override
		public boolean matches(String identifier) {
			if (super.matches(identifier)) return true;
			String lower = identifier.toLowerCase();
			return lower.startsWith("creeper");
		}

		@Override
		public void onSelect(Player player) {
			Utils.sendMessage(player, Settings.msgSelectedCreeperShop);
		}
	};*/

	// CITIZEN
	/*public static final ShopObjectType CITIZEN = new ShopObjectType("citizen", "shopkeeper.citizen") {

		@Override
		public boolean isLivingEntityType() {
			return true;
		}

		@Override
		protected ShopObject createObject(Shopkeeper shopkeeper) {
			return new CitizenShop(shopkeeper);
		}

		@Override
		public boolean isEnabled() {
			return Settings.enableCitizenShops;
		}

		@Override
		public boolean matches(String identifier) {
			if (super.matches(identifier)) return true;
			String lower = identifier.toLowerCase();
			return lower.startsWith("citizen") || lower.startsWith("npc");
		}

		@Override
		public void onSelect(Player player) {
			Utils.sendMessage(player, Settings.msgSelectedVillagerShop);
		}
	};*/
}