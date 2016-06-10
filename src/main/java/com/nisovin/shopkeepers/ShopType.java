package com.nisovin.shopkeepers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.abstractTypes.SelectableType;

public abstract class ShopType<T extends Shopkeeper> extends SelectableType {

	protected ShopType(String identifier, String permission) {
		super(identifier, permission);
	}

	/**
	 * Whether or not this shop type is a player shop type.
	 * 
	 * @return false if it is an admin shop type
	 */
	public abstract boolean isPlayerShopType(); // TODO is this needed or could be hidden behind some abstraction?

	public abstract String getCreatedMessage();

	public final ShopType<?> selectNext(Player player) {
		return ShopkeepersPlugin.getInstance().getShopTypeRegistry().selectNext(player);
	}

	/**
	 * Creates a shopkeeper of this type.
	 * This has to check that all data needed for the shop creation are given and valid.
	 * For example: the owner and chest argument might be null for creating admin shopkeepers
	 * while they are needed for player shops.
	 * Returning null indicates that something is preventing the shopkeeper creation.
	 * 
	 * @param data
	 *            a container holding the necessary arguments (spawn location, object type, owner, etc.) for creating
	 *            this shopkeeper
	 * @return the created Shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created
	 */
	public abstract T createShopkeeper(ShopCreationData data) throws ShopkeeperCreateException;

	/**
	 * Creates the shopkeeper of this type by loading the needed data from the given configuration section.
	 * 
	 * @param config
	 *            the config section to load the shopkeeper data from
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be loaded
	 */
	protected abstract T loadShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException;

	/**
	 * This needs to be called right after the creation or loading of a shopkeeper.
	 * 
	 * @param shopkeeper
	 *            the freshly created shopkeeper
	 */
	protected void registerShopkeeper(T shopkeeper) {
		shopkeeper.shopObject.onInit();
		ShopkeepersPlugin.getInstance().registerShopkeeper(shopkeeper);
	}

	// common checks, which might be useful for extending classes:

	protected void commonPreChecks(ShopCreationData creationData) throws ShopkeeperCreateException {
		// common null checks:
		if (creationData == null || creationData.spawnLocation == null || creationData.objectType == null) {
			throw new ShopkeeperCreateException("null");
		}
	}

	protected void commonPlayerPreChecks(ShopCreationData creationData) throws ShopkeeperCreateException {
		this.commonPreChecks(creationData);
		if (creationData.creator == null || creationData.chest == null) {
			throw new ShopkeeperCreateException("null");
		}
	}

	protected void commonPreChecks(ConfigurationSection section) throws ShopkeeperCreateException {
		// common null checks:
		if (section == null) {
			throw new ShopkeeperCreateException("null");
		}
	}
}
