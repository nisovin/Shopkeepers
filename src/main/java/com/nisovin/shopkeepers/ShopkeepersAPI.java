package com.nisovin.shopkeepers;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

public interface ShopkeepersAPI {

	public static final String HELP_PERMISSION = "shopkeeper.help";
	public static final String TRADE_PERMISSION = "shopkeeper.trade";
	public static final String RELOAD_PERMISSION = "shopkeeper.reload";
	public static final String DEBUG_PERMISSION = "shopkeeper.debug";
	public static final String LIST_OWN_PERMISSION = "shopkeeper.list.own";
	public static final String LIST_OTHERS_PERMISSION = "shopkeeper.list.others";
	public static final String LIST_ADMIN_PERMISSION = "shopkeeper.list.admin";
	public static final String REMOVE_OWN_PERMISSION = "shopkeeper.remove.own";
	public static final String REMOVE_OTHERS_PERMISSION = "shopkeeper.remove.others";
	public static final String REMOVE_ADMIN_PERMISSION = "shopkeeper.remove.admin";
	public static final String REMOTE_PERMISSION = "shopkeeper.remote";
	public static final String TRANSFER_PERMISSION = "shopkeeper.transfer";
	public static final String SETFORHIRE_PERMISSION = "shopkeeper.setforhire";
	public static final String HIRE_PERMISSION = "shopkeeper.hire";
	public static final String BYPASS_PERMISSION = "shopkeeper.bypass";
	public static final String ADMIN_PERMISSION = "shopkeeper.admin";

	/**
	 * Creates a new admin shopkeeper and spawns it into the world.
	 * 
	 * @param shopCreationData
	 *            a container holding the necessary arguments (spawn location, object type, etc.) for creating this shopkeeper
	 * @return the shopkeeper created, or null if creation wasn't successful for some reason
	 */
	public Shopkeeper createNewAdminShopkeeper(ShopCreationData shopCreationData);

	/**
	 * Creates a new player-based shopkeeper and spawns it into the world.
	 * 
	 * @param shopCreationData
	 *            a container holding the necessary arguments (spawn location, object type, owner, etc.) for creating this shopkeeper
	 * @return the shopkeeper created, or null if creation wasn't successful for some reason
	 */
	public Shopkeeper createNewPlayerShopkeeper(ShopCreationData shopCreationData);

	/**
	 * Gets a shopkeeper by a given shopkeeper uuid (note: this is not the entity uuid).
	 * 
	 * @param shopkeeperUUID
	 *            the shopkeeper uuid
	 * @return the shopkeeper for the given uuid, or null
	 */
	public Shopkeeper getShopkeeper(UUID shopkeeperUUID);

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
	 * Gets all shopkeepers for a given chunk. Returns null if there are no shopkeepers in that chunk.
	 * 
	 * @param worldName
	 *            the world name
	 * @param x
	 *            chunk x-coordinate
	 * @param z
	 *            chunk z-coordinate
	 * @return an unmodifiable list of shopkeepers, or null if there are none
	 */
	public List<Shopkeeper> getShopkeepersInChunk(String worldName, int x, int z);

	/**
	 * Gets all shopkeepers for a given chunk. Returns null if there are no shopkeepers in that chunk.
	 * Similar to {@link #getShopkeepersInChunk(String, int, int)}.
	 * 
	 * @param chunkData
	 *            specifies the chunk
	 * @return an unmodifiable list of the shopkeepers in the specified chunk, or null if there are none
	 */
	public List<Shopkeeper> getShopkeepersInChunk(ChunkData chunkData);

	/**
	 * Checks if a given entity is a Shopkeeper.
	 * 
	 * @param entity
	 *            the entity to check
	 * @return whether the entity is a Shopkeeper
	 */
	public boolean isShopkeeper(Entity entity);

	/**
	 * Gets all loaded shopkeepers.
	 * 
	 * @return an unmodifiable view on all loaded shopkeepers
	 */
	public Collection<Shopkeeper> getAllShopkeepers();

	/**
	 * Gets all loaded shopkeepers grouped by the chunks they are in.
	 * 
	 * @return all loaded shopkeepers
	 * @deprecated Use {@link #getAllShopkeepers()} instead.
	 */
	@Deprecated
	public Collection<List<Shopkeeper>> getAllShopkeepersByChunks();

	/**
	 * Gets all active shopkeepers. Some shopkeeper types might be always active (like sign shops),
	 * others are only active as long as their chunk they are in is loaded.
	 * 
	 * @return an unmodifiable view on all active shopkeepers
	 */
	public Collection<Shopkeeper> getActiveShopkeepers();

	/**
	 * Requests a save of all the loaded shopkeepers data.
	 * The actual saving might happen delayed depending on the 'save-instantly' setting from the config.
	 */
	public void save();

	/**
	 * Instantly saves the shopkeepers data of all loaded shopkeepers to file.
	 * File IO is going to happen asynchronous.
	 */
	public void saveReal();
}