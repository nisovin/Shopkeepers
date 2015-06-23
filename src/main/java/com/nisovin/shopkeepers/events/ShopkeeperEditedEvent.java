package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.Shopkeeper;

/**
 * This event is called whenever a player edited a {@link Shopkeeper} in some way.
 */
public class ShopkeeperEditedEvent extends Event {

	private final Player player;
	private final Shopkeeper shopkeeper;

	public ShopkeeperEditedEvent(Player player, Shopkeeper shopkeeper) {
		this.player = player;
		this.shopkeeper = shopkeeper;
	}

	/**
	 * The editing player.
	 * 
	 * @return the editing player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * The edited {@link Shopkeeper}.
	 * 
	 * @return the edited {@link Shopkeeper}
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