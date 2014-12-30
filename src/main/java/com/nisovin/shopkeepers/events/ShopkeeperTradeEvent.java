package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.shopkeepers.Shopkeeper;

/**
 * This event is called whenever a player is about to trade with a shopkeeper.
 * It shares it's cancelled state with the original InventoryClickEvent:
 * Canceling the click event will cancel the trade and canceling the trade
 * is implemented by canceling the original click event.
 * It is recommended to not modify the original click event (besides the cancel state).
 * Also note that the shopkeeper has the final say on whether the trade will be cancelled:
 * It is quite possible that this event is not cancelled but the shopkeeper cancels
 * the trade afterwards nevertheless for some reason.
 * So if you are interested in the actual outcome of the trade take a look at the ShopkeeperTradeCompletedEvent.
 */
public class ShopkeeperTradeEvent extends Event implements Cancellable {

	private final Shopkeeper shopkeeper;
	private final Player player;
	private final InventoryClickEvent clickEvent;

	public ShopkeeperTradeEvent(Shopkeeper shopkeeper, Player player, InventoryClickEvent clickEvent) {
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

	@Override
	public boolean isCancelled() {
		return clickEvent.isCancelled();
	}

	@Override
	public void setCancelled(boolean cancelled) {
		clickEvent.setCancelled(cancelled);
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