package com.nisovin.shopkeepers;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/**
 * Holds the different possible arguments which might be needed (or not needed) for the creation of a shopkeeper of a certain type.
 * By moving those here, into a separate class, we can later easily add new arguments here without breaking old code.
 */
public class ShopCreationData {
	/**
	 * The owner and/or creator of the new shop.
	 */
	public Player creator; // can be null of admin shops
	/**
	 * The chest which is backing the (player-)shop.
	 */
	public Block chest;
	/**
	 * The spawn location.
	 */
	public Location spawnLocation;
	/**
	 * The type of shop to create.
	 */
	public ShopType<?> shopType;
	/**
	 * The object type for this new shop.
	 */
	public ShopObjectType objectType;

	/**
	 * Used by sign shops to specify the direction the sign is facing.
	 */
	public BlockFace blockFace;

	/**
	 * Used by citizens shopkeepers which were created because of the CitizensShopkeeper trait.
	 */
	public Integer npcId;

	public ShopCreationData() {
	}

	// constructors for common attribute groups:

	/**
	 * AdminShopkeeper
	 * 
	 * @param creator
	 *            optional
	 * @param shopType
	 * @param objectType
	 * @param spawnLocation
	 * @param blockFace
	 *            optional
	 */
	public ShopCreationData(Player creator, ShopType<?> shopType, ShopObjectType objectType, Location spawnLocation, BlockFace blockFace) {
		this.creator = creator;
		this.shopType = shopType;
		this.objectType = objectType;
		this.spawnLocation = spawnLocation;
		this.blockFace = blockFace;
	}

	/**
	 * PlayerShopkeeper
	 * 
	 * @param creator
	 * @param shopType
	 * @param objectType
	 * @param spawnLocation
	 * @param blockFace
	 *            optional
	 * @param chest
	 */
	public ShopCreationData(Player creator, ShopType<?> shopType, ShopObjectType objectType, Location spawnLocation, BlockFace blockFace, Block chest) {
		this(creator, shopType, objectType, spawnLocation, blockFace);
		this.chest = chest;
	}
}