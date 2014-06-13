package com.nisovin.shopkeepers;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.abstractTypes.SelectableType;
import com.nisovin.shopkeepers.shopobjects.ShopObject;

public abstract class ShopObjectType extends SelectableType {

	protected ShopObjectType(String identifier, String permission) {
		super(identifier, permission);
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