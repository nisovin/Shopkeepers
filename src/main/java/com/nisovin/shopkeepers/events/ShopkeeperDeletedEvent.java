package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.Shopkeeper;

/**
 * This event is called whenever a {@link Shopkeeper} got deleted by a player.
 * TODO: Maybe also call when the plugin itself removes a shopkeeper?
 */
public class ShopkeeperDeletedEvent extends Event {

	private final Player player;
	private final Shopkeeper shopkeeper;

	public ShopkeeperDeletedEvent(Player player, Shopkeeper shopkeeper) {
		this.player = player;
		this.shopkeeper = shopkeeper;
	}

	/**
	 * The player who removed the {@link Shopkeeper}.
	 * 
	 * @return the player who removed the {@link Shopkeeper}
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * The {@link Shopkeeper} which got removed.
	 * 
	 * @return
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