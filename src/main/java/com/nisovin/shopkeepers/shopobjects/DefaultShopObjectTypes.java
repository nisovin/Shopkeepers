package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;

public class DefaultShopObjectTypes {

	private final static List<ShopObjectType> VALUES = new ArrayList<ShopObjectType>();
	private final static List<ShopObjectType> unmodifiable = Collections.unmodifiableList(VALUES);

	private static ShopObjectType add(ShopObjectType objectType) {
		assert objectType != null;
		VALUES.add(objectType);
		return objectType;
	}

	public static List<ShopObjectType> getValues() {
		return unmodifiable;
	}

	public static boolean isDefaultObjectType(String identifier) {
		if (identifier == null) return false;
		for (ShopObjectType type : VALUES) {
			if (type.getIdentifier().equals(identifier)) {
				return true;
			}
		}
		return false;
	}

	// DEFAULT SHOP TYPES:

	// TODO maybe change permissions to 'shopkeeper.object.<type>'?
	// VILLAGER
	public static final ShopObjectType VILLAGER = add(new ShopObjectType("villager", "shopkeeper.villager") {

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
			player.sendMessage(Settings.msgSelectedVillagerShop);
		}
	});

	// SIGN
	public static final ShopObjectType SIGN = add(new ShopObjectType("block", "shopkeeper.sign") {

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
			player.sendMessage(Settings.msgSelectedSignShop);
		}
	});

	// WITCH
	public static final ShopObjectType WITCH = add(new ShopObjectType("witch", "shopkeeper.witch") {

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
			player.sendMessage(Settings.msgSelectedWitchShop);
		}
	});

	// CREEPER
	public static final ShopObjectType CREEPER = add(new ShopObjectType("creeper", "shopkeeper.creeper") {

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
			player.sendMessage(Settings.msgSelectedCreeperShop);
		}
	});
}