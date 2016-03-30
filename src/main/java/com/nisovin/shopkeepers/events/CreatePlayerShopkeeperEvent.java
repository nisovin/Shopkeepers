package com.nisovin.shopkeepers.events;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;

/**
 * This event is called whenever a player attempts to create a player shopkeeper.
 * 
 * <p>
 * It is called before the max shops limits is checked for the player.<br>
 * The location, shopkeeper type, and player's max shops can be modified.<br>
 * If this event is cancelled, the shop will not be created.
 * </p>
 */
public class CreatePlayerShopkeeperEvent extends Event implements Cancellable {

	private final ShopCreationData creationData;

	private int maxShops;
	private boolean cancelled;

	public CreatePlayerShopkeeperEvent(ShopCreationData creationData, int maxShops) {
		this.creationData = creationData;
		this.maxShops = maxShops;
	}

	/**
	 * Gets the raw shop creation data.
	 * 
	 * <p>
	 * Only modify the returned object if you know what you are doing.
	 * </p>
	 * 
	 * @return the raw shop creation data
	 */
	public ShopCreationData getShopCreationData() {
		return creationData;
	}

	/**
	 * Gets the player trying to create the shop.
	 * 
	 * @return the player
	 */
	public Player getPlayer() {
		return creationData.creator;
	}

	/**
	 * Gets the chest block that will be backing this shop.
	 * 
	 * @return the chest block
	 */
	public Block getChest() {
		return creationData.chest;
	}

	/**
	 * Gets the location the villager will spawn at.
	 * 
	 * @return the spawn location
	 */
	public Location getSpawnLocation() {
		return creationData.spawnLocation;
	}

	/**
	 * Gets the type of shopkeeper that will spawn (ex: normal, book, buying, trading, etc.).
	 * 
	 * @return the shopkeeper type
	 */
	public ShopType<?> getType() {
		return creationData.shopType;
	}

	/**
	 * Gets the maximum number of shops this player can have.
	 * 
	 * @return player max shops
	 */
	public int getMaxShopsForPlayer() {
		return maxShops;
	}

	/**
	 * Sets the location the villager will spawn at.
	 * Do not use an invalid location here!
	 * 
	 * @param location
	 *            the spawn location
	 */
	public void setSpawnLocation(Location location) {
		Validate.notNull(location);
		creationData.spawnLocation = location;
	}

	/**
	 * Sets the type of shopkeeper. This cannot be set to an admin shop.
	 * 
	 * @param shopType
	 *            shopkeeper type
	 */
	public void setType(ShopType<?> shopType) {
		Validate.notNull(shopType);
		Validate.isTrue(shopType.isPlayerShopType());
		creationData.shopType = shopType;
	}

	/**
	 * Sets the maximum number of shops the creating player can have.
	 * 
	 * <p>
	 * If the player has more than this number, the shop will not be created.
	 * </p>
	 * 
	 * @param maxShops
	 *            the player's max shops
	 */
	public void setMaxShopsForPlayer(int maxShops) {
		this.maxShops = maxShops;
	}

	/**
	 * If cancelled the shop won't be created.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled the shop won't be created.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
