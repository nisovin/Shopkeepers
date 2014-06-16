package com.nisovin.shopkeepers;

import com.nisovin.shopkeepers.abstractTypes.SelectableType;

public abstract class ShopObjectType extends SelectableType {

	protected ShopObjectType(String identifier, String permission) {
		super(identifier, permission);
	}

	/*
	 * public final ShopObjectType selectNext(Player player) {
	 * return ShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().selectNext(player);
	 * }
	 */

	/**
	 * Whether or not this shop object type represents a living entity shop object.
	 * 
	 * @return true if this shop object represents a living entity
	 */
	// public abstract boolean isLivingEntityType(); // TODO is this needed or could be hidden behind some abstraction?

	protected abstract ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData);

	/**
	 * Whether or not shop objects of this type shall be spawned and despawned on chunk load and unload.
	 * 
	 * @return true, if the shop object of this type shall be (de-)spawned together with chunk (un-)loads
	 */
	public abstract boolean activateByChunk();
}