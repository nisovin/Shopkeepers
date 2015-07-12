package com.nisovin.shopkeepers.shopobjects.living;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;

public enum LivingEntityType {

	VILLAGER(EntityType.VILLAGER), // default
	BAT(EntityType.BAT), // experimental: requires NoAI, sleeping by default, but starts flying when 'hit'
	BLAZE(EntityType.BLAZE), // experimental: starts flying upwards -> NoAI for now
	CAVE_SPIDER(EntityType.CAVE_SPIDER), // experimental
	CHICKEN(EntityType.CHICKEN), // TODO still lays eggs
	COW(EntityType.COW),
	CREEPER(EntityType.CREEPER),
	ENDER_DRAGON(EntityType.ENDER_DRAGON), // experimental: requires NoAI, shows boss bar, not clickable..
	ENDERMAN(EntityType.ENDERMAN), // experimental: requires NoAI, still teleports away if hit by projectile, starts starring
	GHAST(EntityType.GHAST), // experimental
	GIANT(EntityType.GIANT), // experimental
	HORSE(EntityType.HORSE), // experimental: if clicking with empty hand, the player turns into fixed direction (horse direction?)
	IRON_GOLEM(EntityType.IRON_GOLEM),
	MAGMA_CUBE(EntityType.MAGMA_CUBE), // experimental
	MUSHROOM_COW(EntityType.MUSHROOM_COW),
	OCELOT(EntityType.OCELOT),
	PIG(EntityType.PIG),
	PIG_ZOMBIE(EntityType.PIG_ZOMBIE), // experimental
	SHEEP(EntityType.SHEEP),
	SILVERFISH(EntityType.SILVERFISH), // experimental, strange movement when the player is standing behind it -> NoAI for now
	SKELETON(EntityType.SKELETON),
	SLIME(EntityType.SLIME), // experimental
	SNOWMAN(EntityType.SNOWMAN),
	SPIDER(EntityType.SPIDER), // experimental
	SQUID(EntityType.SQUID), // experimental
	WITCH(EntityType.WITCH),
	WITHER(EntityType.WITHER), // experimental: requires NoAI, shows boss bar
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
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new VillagerShop(shopkeeper, creationData, type);
				}
			};
			break;
		case CREEPER:
			this.objectType = new LivingEntityObjectType(this, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new CreeperShop(shopkeeper, creationData, type);
				}
			};
			break;
		case OCELOT:
			this.objectType = new LivingEntityObjectType(this, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new CatShop(shopkeeper, creationData, type);
				}
			};
			break;
		case SHEEP:
			this.objectType = new LivingEntityObjectType(this, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new SheepShop(shopkeeper, creationData, type);
				}
			};
			break;
		case SKELETON:
			this.objectType = new LivingEntityObjectType(this, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new SkeletonShop(shopkeeper, creationData, type);
				}
			};
			break;
		case ZOMBIE:
			this.objectType = new LivingEntityObjectType(this, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new ZombieShop(shopkeeper, creationData, type);
				}
			};
			break;
		default:
			this.objectType = new LivingEntityObjectType(this, aliases, typeName, permission);
			break;
		}
	}

	public EntityType getEntityType() {
		return entityType;
	}

	public ShopObjectType getObjectType() {
		return objectType;
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