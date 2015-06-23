package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

/**
 * This event is called whenever a player is about to hire a player shopkeeper.
 * 
 * <p>
 * It is called before the max shops limit is checked for the player.<br>
 * If this event is cancelled or the player has reached the max shops limit, the shop will not be hired.
 * </p>
 */
public class PlayerShopkeeperHiredEvent extends Event implements Cancellable {

	private final Player player;
	private final PlayerShopkeeper shopkeeper;
	private int maxShops;

	private boolean cancelled;

	public PlayerShopkeeperHiredEvent(Player player, PlayerShopkeeper shopkeeper, int maxShops) {
		this.player = player;
		this.shopkeeper = shopkeeper;
		this.maxShops = maxShops;
	}

	/**
	 * Gets the player trying to hire the shop.
	 * 
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the shokeeper the player is about to hire.
	 * 
	 * @return the shopkeeper
	 */
	public PlayerShopkeeper getShopkeeper() {
		return shopkeeper;
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
	 * Sets the maximum number of shops the creating player can have.
	 * If the player has more than this number, the shop will not be hired.
	 * 
	 * @param maxShops
	 *            the player's max shops
	 */
	public void setMaxShopsForPlayer(int maxShops) {
		this.maxShops = maxShops;
	}

	/**
	 * If cancelled the hiring will not take place.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled the hiring will not take place.
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