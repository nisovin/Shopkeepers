package com.nisovin.shopkeepers.shopobjects.living;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;

public enum LivingEntityType {

	VILLAGER(EntityType.VILLAGER), // default
	//BAT(EntityType.BAT),
	//BLAZE(EntityType.BLAZE),
	//CAVE_SPIDER(EntityType.CAVE_SPIDER),
	CHICKEN(EntityType.CHICKEN),
	COW(EntityType.COW),
	CREEPER(EntityType.CREEPER),
	// ENDER_DRAGON(EntityType.ENDER_DRAGON),
	//ENDERMAN(EntityType.ENDERMAN),
	//GHAST(EntityType.GHAST),
	//GIANT(EntityType.GIANT),
	// HORSE(EntityType.HORSE),
	IRON_GOLEM(EntityType.IRON_GOLEM),
	//MAGMA_CUBE(EntityType.MAGMA_CUBE),
	MUSHROOM_COW(EntityType.MUSHROOM_COW),
	OCELOT(EntityType.OCELOT),
	PIG(EntityType.PIG),
	//PIG_ZOMBIE(EntityType.PIG_ZOMBIE),
	SHEEP(EntityType.SHEEP),
	//SILVERFISH(EntityType.SILVERFISH),
	SKELETON(EntityType.SKELETON),
	//SLIME(EntityType.SLIME),
	SNOWMAN(EntityType.SNOWMAN),
	//SPIDER(EntityType.SPIDER),
	//SQUID(EntityType.SQUID),
	WITCH(EntityType.WITCH),
	// WITHER(EntityType.WITHER),
	WOLF(EntityType.WOLF),
	ZOMBIE(EntityType.ZOMBIE);

	private static final List<String> entityTypeNames = new ArrayList<String>();

	private final EntityType entityType;
	private final ShopObjectType objectType;

	private LivingEntityType(EntityType entityType, String... aliases) {
		assert entityType != null;
		this.entityType = entityType;

		String typeName = entityType.name().toLowerCase();
		// TODO maybe change permission to: 'shopkeeper.entity.<entityType>'
		String permission = "shopkeeper.entity." + typeName;

		switch (entityType) {
		case VILLAGER:
			this.objectType = new LivingEntityObjectType(this, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper) {
					return new VillagerShop(shopkeeper, type);
				}
			};
			break;
		case CREEPER:
			this.objectType = new LivingEntityObjectType(this, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper) {
					return new CreeperShop(shopkeeper, type);
				}
			};
			break;
		default:
			this.objectType = new LivingEntityObjectType(this, aliases, typeName, permission);
			break;
		}
	}

	public EntityType getEntityType() {
		return this.entityType;
	}

	public ShopObjectType getObjectType() {
		return this.objectType;
	}

	static {
		for (LivingEntityType type : values()) {
			entityTypeNames.add(type.entityType.name());
		}
	}

	public static List<String> getAvailableEntityTypeNames() {
		return entityTypeNames;
	}
}