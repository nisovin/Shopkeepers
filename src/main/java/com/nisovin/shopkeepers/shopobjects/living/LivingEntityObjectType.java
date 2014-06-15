package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopObject;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.Utils;

public class LivingEntityObjectType extends ShopObjectType {

	protected final LivingEntityType type;
	protected final String[] aliases;

	protected LivingEntityObjectType(LivingEntityType type, String[] aliases, String identifier, String permission) {
		super(identifier, permission);
		this.type = type;
		this.aliases = aliases != null ? aliases : new String[0];
	}

	@Override
	public boolean hasPermission(Player player) {
		return super.hasPermission(player) || player.hasPermission("shopkeeper.entity.*");
	}

	/*
	 * @Override
	 * public boolean isLivingEntityType() {
	 * return true;
	 * }
	 */

	@Override
	protected ShopObject createObject(Shopkeeper shopkeeper) {
		return new LivingEntityShop(shopkeeper, this.type);
	}

	@Override
	public boolean isEnabled() {
		return !Settings.disabledLivingShops.contains(this.type.getEntityType().name());
	}

	@Override
	public boolean matches(String identifier) {
		if (super.matches(identifier)) return true;
		String lower = identifier.toLowerCase();
		for (String alias : this.aliases) {
			if (lower.startsWith(alias)) return true;
		}
		return false;
	}

	@Override
	public void onSelect(Player player) {
		// TODO translation support for the entity type name?
		Utils.sendMessage(player, Settings.msgSelectedLivingShop, "{type}", this.type.getEntityType().name());
	}

	@Override
	public boolean activateByChunk() {
		return true; // despawn shop entities on chunk unload, and spawn them again on chunk load
	}
}