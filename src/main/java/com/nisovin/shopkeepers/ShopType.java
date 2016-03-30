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

	// TODO also put the sending of messages to the shop creator inside this method? ex: 'you need to select a chest
	// first'
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
	 * @return the created Shopkeeper, or null if it couldn't be created
	 */
	public abstract T createShopkeeper(ShopCreationData data);

	/**
	 * Creates the shopkeeper of this type by loading the needed data from the given configuration section.
	 * 
	 * @param config
	 *            the config section to load the shopkeeper data from
	 * @return the created shopkeeper, or null if it couldn't be loaded
	 */
	protected abstract T loadShopkeeper(ConfigurationSection config);

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

	// TODO instead of returning null: throw (illegal argument) exceptions and catch those?
	/*
	 * Returns false if some check fails.
	 */
	protected boolean commonPreChecks(ShopCreationData creationData) {
		// common null checks:
		if (creationData == null || creationData.spawnLocation == null || creationData.objectType == null) {
			Log.debug("Couldn't create shopkeeper: null");
			return false;
		}
		return true;
	}

	protected boolean commonPlayerPreChecks(ShopCreationData creationData) {
		if (!this.commonPreChecks(creationData)) return false;
		if (creationData.creator == null || creationData.chest == null) {
			Log.debug("Couldn't create shopkeeper: null");
			return false;
		}

		return true;
	}

	protected boolean commonPreChecks(ConfigurationSection section) {
		// common null checks:
		if (section == null) {
			Log.debug("Couldn't create shopkeeper: null");
			return false;
		}
		return true;
	}
}
