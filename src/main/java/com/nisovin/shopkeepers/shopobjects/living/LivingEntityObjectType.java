package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.Utils;

public class LivingEntityObjectType extends ShopObjectType {

	protected final EntityType entityType;
	protected final String[] aliases;

	protected LivingEntityObjectType(EntityType entityType, String[] aliases, String identifier, String permission) {
		super(identifier, permission);
		this.entityType = entityType;
		assert entityType.isAlive();
		this.aliases = aliases != null ? aliases : new String[0];
	}

	public EntityType getEntityType() {
		return entityType;
	}

	@Override
	public boolean hasPermission(Player player) {
		return super.hasPermission(player) || Utils.hasPermission(player, "shopkeeper.entity.*");
	}

	@Override
	protected ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData) {
		return new LivingEntityShop(shopkeeper, creationData, this);
	}

	@Override
	public boolean isEnabled() {
		return Settings.enabledLivingShops.contains(entityType.name());
	}

	@Override
	public boolean matches(String identifier) {
		if (super.matches(identifier)) return true;
		String lower = identifier.toLowerCase();
		for (String alias : aliases) {
			if (lower.startsWith(alias)) return true;
		}
		return false;
	}

	@Override
	public void onSelect(Player player) {
		// TODO translation support for the entity type name?
		Utils.sendMessage(player, Settings.msgSelectedLivingShop, "{type}", entityType.name());
	}

	@Override
	public boolean needsSpawning() {
		return true; // despawn shop entities on chunk unload, and spawn them again on chunk load
	}
}
