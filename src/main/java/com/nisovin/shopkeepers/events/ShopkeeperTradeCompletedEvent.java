package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.shopkeepers.Shopkeeper;

/**
 * This event is called after a shopkeeper has processed a trade.
 * 
 * <p>
 * Neither the original {@link InventoryClickEvent} nor the {@link ShopkeeperTradeEvent} should be modified under any
 * circumstances at this state!
 * </p>
 */
public class ShopkeeperTradeCompletedEvent extends Event {

	private final ShopkeeperTradeEvent completedTrade;

	public ShopkeeperTradeCompletedEvent(ShopkeeperTradeEvent completedTrade) {
		this.completedTrade = completedTrade;
	}

	/**
	 * Gets the trading player.
	 * 
	 * @return the player involved in this trade
	 */
	public Player getPlayer() {
		return completedTrade.getPlayer();
	}

	/**
	 * Gets the trading {@link Shopkeeper}.
	 * 
	 * @return the {@link Shopkeeper} involved in this trade
	 */
	public Shopkeeper getShopkeeper() {
		return completedTrade.getShopkeeper();
	}

	/**
	 * Gets the {@link InventoryClickEvent} which originally triggered this trade.
	 * 
	 * @return the original {@link InventoryClickEvent}
	 */
	public InventoryClickEvent getClickEvent() {
		return completedTrade.getClickEvent();
	}

	/**
	 * Gets the completed {@link ShopkeeperTradeEvent}.
	 * 
	 * <p>
	 * The {@link ShopkeeperTradeEvent} might hold additional information available about the completed trade.<br>
	 * Do not attempt to modify the {@link ShopkeeperTradeEvent} in any way!
	 * </p>
	 * 
	 * @return the completed {@link ShopkeeperTradeEvent}
	 */
	public ShopkeeperTradeEvent getCompletedTrade() {
		return completedTrade;
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
