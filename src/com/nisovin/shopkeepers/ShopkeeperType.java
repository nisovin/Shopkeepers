package com.nisovin.shopkeepers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

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
