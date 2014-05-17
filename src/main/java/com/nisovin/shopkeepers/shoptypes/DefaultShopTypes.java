package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.Shopkeeper;

public class DefaultShopTypes {

	private final static List<ShopType> VALUES = new ArrayList<ShopType>();
	private final static List<ShopType> unmodifiable = Collections.unmodifiableList(VALUES);

	private static ShopType add(ShopType shopType) {
		assert shopType != null;
		VALUES.add(shopType);
		return shopType;
	}

	public static List<ShopType> getValues() {
		return unmodifiable;
	}

	public static boolean isDefaultShopType(String identifier) {
		if (identifier == null) return false;
		for (ShopType type : VALUES) {
			if (type.getIdentifier().equals(identifier)) {
				return true;
			}
		}
		return false;
	}

	private static abstract class DefaultShopType extends ShopType {

		protected DefaultShopType(String identifier, String permission) {
			super(identifier, permission);
		}

		// TODO instead of returning null: throw (illegal argument) exceptions and catch those?
		/*
		 * Returns false if some check fails.
		 */
		protected boolean commonPreChecks(ShopCreationData data) {
			// common null checks:
			if (data == null || data.location == null || data.objectType == null) {
				Log.debug("Couldn't create shopkeeper: null");
				return false;
			}
			return true;
		}

		protected boolean commonPlayerPreChecks(ShopCreationData data) {
			if (!this.commonPreChecks(data)) return false;
			if (data.creator == null || data.chest == null) {
				Log.debug("Couldn't create shopkeeper: null");
				return false;
			}

			return true;
		}

		protected boolean commonPreChecks(ConfigurationSection section) {
			// common null checks:
			if (section == null) {
				Log.debug("Couldn't create shopkeeper: null");
				return false;
			}
			return true;
		}
	}

	// DEFAULT SHOP TYPES:

	// ADMIN SHOP
	public final static ShopType ADMIN = add(new DefaultShopType("admin", "shopkeeper.admin") {

		@Override
		public Shopkeeper createShopkeeper(ConfigurationSection config) {
			if (this.commonPreChecks(config)) {
				return new AdminShopkeeper(config);
			}
			return null;
		}

		@Override
		public Shopkeeper createShopkeeper(ShopCreationData data) {
			if (this.commonPreChecks(data)) {
				return new AdminShopkeeper(data.location, data.objectType);
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
	});

	// NORMAL PLAYER SHOP
	public final static ShopType PLAYER_NORMAL = add(new DefaultShopType("player", "shopkeeper.player.normal") {

		@Override
		public Shopkeeper createShopkeeper(ConfigurationSection config) {
			if (this.commonPreChecks(config)) {
				return new NormalPlayerShopkeeper(config);
			}
			return null;
		}

		@Override
		public Shopkeeper createShopkeeper(ShopCreationData data) {
			if (this.commonPlayerPreChecks(data)) {
				return new NormalPlayerShopkeeper(data.creator, data.chest, data.location, data.objectType);
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
			player.sendMessage(Settings.msgSelectedNormalShop);
		}
	});

	// PLAYER BOOK SHOP
	public final static ShopType PLAYER_BOOK = add(new DefaultShopType("book", "shopkeeper.player.book") {

		@Override
		public Shopkeeper createShopkeeper(ConfigurationSection config) {
			if (this.commonPreChecks(config)) {
				return new WrittenBookPlayerShopkeeper(config);
			}
			return null;
		}

		@Override
		public Shopkeeper createShopkeeper(ShopCreationData data) {
			if (this.commonPlayerPreChecks(data)) {
				return new WrittenBookPlayerShopkeeper(data.creator, data.chest, data.location, data.objectType);
			}
			return null;
		}

		@Override
		public boolean isPlayerShopType() {
			return true;
		}

		@Override
		public String getCreatedMessage() {
			return Settings.msgBookShopCreated;
		}

		@Override
		public boolean matches(String identifier) {
			if (super.matches(identifier)) return true;
			String lower = identifier.toLowerCase();
			return lower.startsWith("book");
		}

		@Override
		public void onSelect(Player player) {
			player.sendMessage(Settings.msgSelectedBookShop);
		}
	});

	// BUYING PLAYER SHOP
	public final static ShopType PLAYER_BUY = add(new DefaultShopType("buy", "shopkeeper.player.buy") {

		@Override
		public Shopkeeper createShopkeeper(ConfigurationSection config) {
			if (this.commonPreChecks(config)) {
				return new BuyingPlayerShopkeeper(config);
			}
			return null;
		}

		@Override
		public Shopkeeper createShopkeeper(ShopCreationData data) {
			if (this.commonPlayerPreChecks(data)) {
				return new BuyingPlayerShopkeeper(data.creator, data.chest, data.location, data.objectType);
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
			player.sendMessage(Settings.msgSelectedBuyShop);
		}
	});

	// TRADING PLAYER SHOP
	public final static ShopType PLAYER_TRADE = add(new DefaultShopType("trade", "shopkeeper.player.trade") {

		@Override
		public Shopkeeper createShopkeeper(ConfigurationSection config) {
			if (this.commonPreChecks(config)) {
				return new TradingPlayerShopkeeper(config);
			}
			return null;
		}

		@Override
		public Shopkeeper createShopkeeper(ShopCreationData data) {
			if (this.commonPlayerPreChecks(data)) {
				return new TradingPlayerShopkeeper(data.creator, data.chest, data.location, data.objectType);
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
			player.sendMessage(Settings.msgSelectedTradeShop);
		}
	});
}