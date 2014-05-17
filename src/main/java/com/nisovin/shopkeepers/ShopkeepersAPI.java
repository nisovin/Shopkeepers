package com.nisovin.shopkeepers;

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
	 * Gets the shopkeeper by the villager's entity id.
	 * 
	 * @param entityId
	 *            the entity id of the villager
	 * @return the Shopkeeper, or null if the entity with the given id is not a shopkeeper
	 */
	public Shopkeeper getShopkeeperByEntityId(int entityId);

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
	 * @param world
	 *            the world
	 * @param x
	 *            chunk x-coordinate
	 * @param z
	 *            chunk z-coordinate
	 * @return a list of shopkeepers, or null if there are none
	 */
	public List<Shopkeeper> getShopkeepersInChunk(String world, int x, int z);

	/**
	 * Checks if a given entity is a Shopkeeper.
	 * 
	 * @param entity
	 *            the entity to check
	 * @return whether the entity is a Shopkeeper
	 */
	public boolean isShopkeeper(Entity entity);

}