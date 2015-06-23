package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.Shopkeeper;

/**
 * This event is called whenever a {@link Shopkeeper} got freshly created.
 */
public class ShopkeeperCreatedEvent extends Event {

	private final Player player;
	private final Shopkeeper shopkeeper;

	public ShopkeeperCreatedEvent(Player player, Shopkeeper shopkeeper) {
		this.player = player;
		this.shopkeeper = shopkeeper;
	}

	/**
	 * The player creating the {@link Shopkeeper}.
	 * 
	 * @return the player creating the {@link Shopkeeper}, possibly null if the {@link Shopkeeper} is created by a
	 *         plugin
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * The created {@link Shopkeeper}.
	 * 
	 * @return the created {@link Shopkeeper}
	 */
	public Shopkeeper getShopkeeper() {
		return shopkeeper;
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