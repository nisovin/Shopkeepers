package com.nisovin.shopkeepers;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.shopobjects.*;

public enum ShopObjectType {

	NONE(0, ""),
	
	VILLAGER(1, "villager"),
	
	SIGN(2, "sign"),
	
	WITCH(3, "witch"),
	
	CREEPER(4, "creeper");
	
	private static final int MAX_ID = 4;
	
	private final int id;
	private final String permission;
	
	private ShopObjectType(int id, String permission) {
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
	
	public ShopObject createObject() {
		if (id == 1) {
			return new VillagerShop();
		} else if (id == 2) {
			return new BlockShop();
		} else if (id == 3) {
			return new WitchShop();
		} else if (id == 4) {
			return new CreeperShop();
		} else {
			return null;
		}
	}
	
	public boolean isLivingEntityType() {
		switch (this) {
		case VILLAGER:
		case WITCH:
		case CREEPER:
			return true;

		default:
			return false;
		}
	}
	
	private static final Map<Integer, ShopObjectType> typeMap = new HashMap<Integer, ShopObjectType>();
		
	static {
		for (ShopObjectType type : values()) {
			typeMap.put(type.id, type);
		}
	}
	
	public static ShopObjectType next(Player player, ShopObjectType current) {
		ShopObjectType next = null;
		if (current == null) {
			next = NONE;
		} else {
			next = current;
		}
		
		for (int i = 0; i < MAX_ID; i++) {
			int nextId = next.id + 1;
			if (nextId > MAX_ID) nextId = 1;
			next = typeMap.get(nextId);
			
			if (player.hasPermission("shopkeeper." + next.permission)) {
				if (next == VILLAGER && !Settings.enableVillagerShops)
					continue;
				else if (next == WITCH && !Settings.enableWitchShops)
					continue;
				else if (next == SIGN && !Settings.enableSignShops)
					continue;
				else if (next == CREEPER && !Settings.enableCreeperShops)
					continue;
				return next;
			}
		}
		
		return current;
	}
	
}
