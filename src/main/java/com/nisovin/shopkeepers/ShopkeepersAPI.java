package com.nisovin.shopkeepers;

import java.util.Collection;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public interface ShopkeepersAPI {

	/**
	 * Creates a new admin shopkeeper and spawns it into the world.
	 * 
	 * @param shopCreationData
	 *            a container holding the necessary arguments (spawn location, object type, etc.) for creating this shopkeeper
	 * @return the shopkeeper created
	 */
	public Shopkeeper createNewAdminShopkeeper(ShopCreationData shopCreationData);

	/**
	 * Creates a new player-based shopkeeper and spawns it into the world.
	 * 
	 * @param shopCreationData
	 *            a container holding the necessary arguments (spawn location, object type, owner, etc.) for creating this shopkeeper
	 * @return the shopkeeper created
	 */
	public Shopkeeper createNewPlayerShopkeeper(ShopCreationData shopCreationData);

	/**
	 * Gets the shopkeeper for a given entity.
	 * 
	 * @param entity
	 *            the entity
	 * @return the Shopkeeper, or null if the given entity is not a shopkeeper
	 */
	public Shopkeeper getShopkeeperByEntity(Entity entity);

	/**
	 * Gets the shopkeeper for a given block (ex: sign shops).
	 * 
	 * @param block
	 *            the block
	 * @return the shopkeeper, or null if the given block is not a shopkeeper
	 */
	public Shopkeeper getShopkeeperByBlock(Block block);

	/**
	 * Gets all shopkeepers from a given chunk. Returns null if there are no shopkeepers in that chunk.
	 * 
	 * @param worldName
	 *            the world name
	 * @param x
	 *            chunk x-coordinate
	 * @param z
	 *            chunk z-coordinate
	 * @return a list of shopkeepers, or null if there are none
	 */
	public List<Shopkeeper> getShopkeepersInChunk(String worldName, int x, int z);

	/**
	 * Checks if a given entity is a Shopkeeper.
	 * 
	 * @param entity
	 *            the entity to check
	 * @return whether the entity is a Shopkeeper
	 */
	public boolean isShopkeeper(Entity entity);

	/**
	 * Gets all loaded shopkeepers grouped by the chunks they are in.
	 * 
	 * @return all loaded shopkeepers
	 */
	public Collection<List<Shopkeeper>> getAllShopkeepersByChunks();

	/**
	 * Gets all active shopkeepers. Some shopkeeper types might be always active (like sign shops),
	 * others are only active as long as their chunk they are in is loaded.
	 * 
	 * @return all active shopkeepers
	 */
	public Collection<Shopkeeper> getActiveShopkeepers();

	/**
	 * Requests a save of all the loaded shopkeepers data.
	 * The actual saving might happen delayed depending on the 'save-instantly' setting from the config.
	 */
	public void save();

	/**
	 * Instantly saves the shopkeepers data of all loaded shopkeepers to file.
	 */
	public void saveReal();
}