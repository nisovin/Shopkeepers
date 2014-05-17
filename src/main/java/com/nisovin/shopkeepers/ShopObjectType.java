package com.nisovin.shopkeepers;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.abstractTypes.SelectableType;
import com.nisovin.shopkeepers.shopobjects.ShopObject;

public abstract class ShopObjectType extends SelectableType {

	protected ShopObjectType(String identifier, String permission) {
		super(identifier, permission);

		// automatically register this shop object type:
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) throw new IllegalStateException("Cannot initialize a new shop object type while Shopkeepers is disabled.");
		plugin.getShopObjectTypeRegistry().register(this);
	}

	public final ShopObjectType selectNext(Player player) {
		return ShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().selectNext(player);
	}

	/**
	 * Whether or not this shop object type represents a living entity shop object.
	 * 
	 * @return true if this shop object represents a living entity
	 */
	public abstract boolean isLivingEntityType(); // TODO is this needed or could be hidden behind the abstraction?

	protected abstract ShopObject createObject(Shopkeeper shopkeeper);
}