package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.shopkeepers.Shopkeeper;

/**
 * This event is called after a shopkeeper has processed a trade.
 * The original InventoryClickEvent should not be modified under any circumstances at this state!
 */
public class ShopkeeperTradeCompletedEvent extends Event {

	private final Shopkeeper shopkeeper;
	private final Player player;
	private final InventoryClickEvent clickEvent;

	public ShopkeeperTradeCompletedEvent(Shopkeeper shopkeeper, Player player, InventoryClickEvent clickEvent) {
		this.shopkeeper = shopkeeper;
		this.player = player;
		this.clickEvent = clickEvent;
	}

	public Player getPlayer() {
		return player;
	}

	public Shopkeeper getShopkeeper() {
		return shopkeeper;
	}

	public InventoryClickEvent getClickEvent() {
		return clickEvent;
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