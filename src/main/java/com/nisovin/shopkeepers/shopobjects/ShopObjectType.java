package com.nisovin.shopkeepers.shopobjects;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;

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

	public ShopObject createObject(Shopkeeper shopkeeper) {
		assert shopkeeper != null;
		switch (this) {
		case VILLAGER:
			return new VillagerShop(shopkeeper);
		case SIGN:
			return new BlockShop(shopkeeper);
		case WITCH:
			return new WitchShop(shopkeeper);
		case CREEPER:
			return new CreeperShop(shopkeeper);

		default:
			return new VillagerShop(shopkeeper);
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

	public static ShopObjectType getTypeFromName(String objectTypeName) {
		if (objectTypeName != null) {
			if (objectTypeName.equals("villager")) {
				return ShopObjectType.VILLAGER;
			} else if (objectTypeName.equals("block")) {
				return ShopObjectType.SIGN;
			} else if (objectTypeName.equals("witch")) {
				return ShopObjectType.WITCH;
			} else if (objectTypeName.equals("creeper")) {
				return ShopObjectType.CREEPER;
			}
		}
		return ShopObjectType.VILLAGER;
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
				if (next == VILLAGER && !Settings.enableVillagerShops) continue;
				else if (next == WITCH && !Settings.enableWitchShops) continue;
				else if (next == SIGN && !Settings.enableSignShops) continue;
				else if (next == CREEPER && !Settings.enableCreeperShops) continue;
				return next;
			}
		}

		return current;
	}

}