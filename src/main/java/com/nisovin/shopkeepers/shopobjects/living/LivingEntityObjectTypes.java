package com.nisovin.shopkeepers.shopobjects.living;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.Shopkeeper;

public class LivingEntityObjectTypes {

	/*
	 * Notes about individual differences and issues for specific entity types:
	 * All non-listed entity types are completely untested and therefore 'experimental' as well.
	 * 
	 * <ul>
	 * <li> VILLAGER: okay, default
	 * <li> BAT: experimental: requires NoAI, sleeping by default, but starts flying when 'hit'
	 * <li> BLAZE: experimental: starts flying upwards -> NoAI for now
	 * <li> CAVE_SPIDER: experimental
	 * <li> CHICKEN: TODO still lays eggs
	 * <li> COW: okay
	 * <li> CREEPER: okay
	 * <li> ENDER_DRAGON: experimental: requires NoAI, shows boss bar, not clickable..
	 * <li> ENDERMAN: experimental: requires NoAI, still teleports away if hit by projectile, starts starring
	 * <li> GHAST: experimental
	 * <li> GIANT: experimental
	 * <li> HORSE: experimental: if clicking with empty hand, the player turns into fixed direction (horse direction?)
	 * <li> IRON_GOLEM: okay
	 * <li> MAGMA_CUBE: experimental
	 * <li> MUSHROOM_COW: okay
	 * <li> OCELOT: okay
	 * <li> PIG: seems to work fine
	 * <li> PIG_ZOMBIE: experimental
	 * <li> SHEEP: okay
	 * <li> SILVERFISH: experimental, strange movement when the player is standing behind it -> NoAI for now
	 * <li> SKELETON: okay
	 * <li> SLIME: experimental
	 * <li> SNOWMAN: okay
	 * <li> SPIDER: experimental
	 * <li> SQUID: experimental
	 * <li> WITCH: okay
	 * <li> WITHER: experimental: requires NoAI, shows boss bar
	 * <li> WOLF: okay
	 * <li> ZOMBIE: okay
	 * <ul>
	 */

	// order is specified by the 'enabled-living-shops' config setting:
	private final Map<EntityType, LivingEntityObjectType> objectTypes = new LinkedHashMap<EntityType, LivingEntityObjectType>();

	public LivingEntityObjectTypes() {
		// first, create the enabled living object types, in the same order as specified in the config:
		for (String enabledEntityType : Settings.enabledLivingShops) {
			if (enabledEntityType == null) continue; // just in case
			EntityType entityType;
			try {
				entityType = EntityType.valueOf(enabledEntityType);
			} catch (IllegalArgumentException e) {
				// unknown entity type:
				continue;
			}
			if (entityType != null && entityType.isAlive() && entityType.isSpawnable()) {
				// not using aliases (yet?)
				objectTypes.put(entityType, this.createLivingEntityObjectType(entityType));
			}
		}

		// register object types for all other remaining living entity types:
		for (EntityType entityType : EntityType.values()) {
			if (entityType.isAlive() && entityType.isSpawnable() && !objectTypes.containsKey(entityType)) {
				// not using aliases (yet?)
				objectTypes.put(entityType, this.createLivingEntityObjectType(entityType));
			}
		}
	}

	public Collection<LivingEntityObjectType> getAllObjectTypes() {
		return Collections.unmodifiableCollection(objectTypes.values());
	}

	public LivingEntityObjectType getObjectType(EntityType entityType) {
		return objectTypes.get(entityType);
	}

	private LivingEntityObjectType createLivingEntityObjectType(EntityType entityType, String... aliases) {
		String typeName = entityType.name().toLowerCase();
		String permission = "shopkeeper.entity." + typeName;

		LivingEntityObjectType objectType;

		switch (entityType) {
		case VILLAGER:
			objectType = new LivingEntityObjectType(entityType, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new VillagerShop(shopkeeper, creationData, this);
				}
			};
			break;
		case CREEPER:
			objectType = new LivingEntityObjectType(entityType, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new CreeperShop(shopkeeper, creationData, this);
				}
			};
			break;
		case OCELOT:
			objectType = new LivingEntityObjectType(entityType, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new CatShop(shopkeeper, creationData, this);
				}
			};
			break;
		case SHEEP:
			objectType = new LivingEntityObjectType(entityType, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new SheepShop(shopkeeper, creationData, this);
				}
			};
			break;
		case SKELETON:
			objectType = new LivingEntityObjectType(entityType, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new SkeletonShop(shopkeeper, creationData, this);
				}
			};
			break;
		case ZOMBIE:
			objectType = new LivingEntityObjectType(entityType, aliases, typeName, permission) {
				@Override
				protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
					return new ZombieShop(shopkeeper, creationData, this);
				}
			};
			break;
		default:
			objectType = new LivingEntityObjectType(entityType, aliases, typeName, permission);
			break;
		}

		assert objectType != null;
		return objectType;
	}
}
