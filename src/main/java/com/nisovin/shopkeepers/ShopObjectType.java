package com.nisovin.shopkeepers;

import com.nisovin.shopkeepers.abstractTypes.SelectableType;

public abstract class ShopObjectType extends SelectableType {

	protected ShopObjectType(String identifier, String permission) {
		super(identifier, permission);
	}

	protected abstract ShopObject createObject(Shopkeeper shopkeeper, ShopCreationData creationData);

	/**
	 * Whether or not shop objects of this type shall be spawned and despawned on chunk load and unload.
	 * 
	 * @return true, if the shop object of this type shall be (de-)spawned together with chunk (un-)loads
	 */
	public abstract boolean needsSpawning();
}
