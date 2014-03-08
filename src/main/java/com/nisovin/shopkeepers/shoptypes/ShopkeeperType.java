package com.nisovin.shopkeepers.shoptypes;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectType;

/**
 * Type of shopkeeper.
 * 
 */
public enum ShopkeeperType {

	/**
	 * A admin shopkeeper, which has infinite supply and does not store income anywhere.
	 */
	ADMIN(0, "admin"),

	/**
	 * A normal player shopkeeper that uses a chest for the item supply and to store income.
	 */
	PLAYER_NORMAL(1, "player.normal"),

	/**
	 * A player shopkeeper that sells books, using a chest for the written book source,
	 * a supply of empty book & quills, and to store income.
	 */
	PLAYER_BOOK(2, "player.book"),

	/**
	 * A player shopkeeper that buys items instead of selling, using a chest as a source
	 * for the currency and to deposit items.
	 */
	PLAYER_BUY(3, "player.buy"),

	/**
	 * A player shopkeeper that sells items for other items instead of for currency items.
	 * Uses a chest to store items.
	 */
	PLAYER_TRADE(4, "player.trade");

	private static final int MAX_ID = 4;

	private final int id;
	private final String permission;

	private ShopkeeperType(int id, String permission) {
		this.id = id;
		this.permission = permission;
	}

	public String getPermission() {
		return permission;
	}

	public int getId() {
		return id;
	}

	public boolean hasPermission(Player player) {
		return player.hasPermission("shopkeeper." + permission);
	}

	public Shopkeeper createShopkeeper(Player player, Block chest, Location location, ShopObjectType shopObjectType) {
		switch (this) {
		case ADMIN:
			// player and chest can be null in this case:
			return new AdminShopkeeper(location, shopObjectType);
		case PLAYER_NORMAL:
			return new NormalPlayerShopkeeper(player, chest, location, shopObjectType);
		case PLAYER_BOOK:
			return new WrittenBookPlayerShopkeeper(player, chest, location, shopObjectType);
		case PLAYER_BUY:
			return new BuyingPlayerShopkeeper(player, chest, location, shopObjectType);
		case PLAYER_TRADE:
			return new TradingPlayerShopkeeper(player, chest, location, shopObjectType);

		default:
			return null;
		}
	}

	public Shopkeeper createShopkeeper(ConfigurationSection config) {
		switch (this) {
		case ADMIN:
			return new AdminShopkeeper(config);
		case PLAYER_NORMAL:
			return new NormalPlayerShopkeeper(config);
		case PLAYER_BOOK:
			return new WrittenBookPlayerShopkeeper(config);
		case PLAYER_BUY:
			return new BuyingPlayerShopkeeper(config);
		case PLAYER_TRADE:
			return new TradingPlayerShopkeeper(config);

		default:
			return null;
		}
	}

	public static ShopkeeperType getTypeFromName(String typeName) {
		if (typeName == null || typeName.isEmpty()) return ShopkeeperType.ADMIN;
		if (typeName.equals("book")) {
			return ShopkeeperType.PLAYER_BOOK;
		} else if (typeName.equals("buy")) {
			return ShopkeeperType.PLAYER_BUY;
		} else if (typeName.equals("trade")) {
			return ShopkeeperType.PLAYER_TRADE;
		} else if (typeName.equals("player")) {
			return ShopkeeperType.PLAYER_NORMAL;
		} else {
			return ShopkeeperType.ADMIN;
		}
	}

	private static final Map<Integer, ShopkeeperType> typeMap = new HashMap<Integer, ShopkeeperType>();

	static {
		for (ShopkeeperType type : values()) {
			typeMap.put(type.id, type);
		}
	}

	public static ShopkeeperType next(Player player, ShopkeeperType current) {
		if (current == ADMIN) return ADMIN;

		ShopkeeperType next = null;
		if (current == null) {
			next = ADMIN;
		} else {
			next = current;
		}

		for (int i = 0; i < MAX_ID; i++) {
			int nextId = next.id + 1;
			if (nextId > MAX_ID) nextId = 1;
			next = typeMap.get(nextId);

			if (player.hasPermission("shopkeeper." + next.permission)) {
				return next;
			}
		}

		return current;
	}

}