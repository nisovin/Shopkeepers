package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Shopkeeper;

/**
 * This event is called whenever a player is about to trade with a shopkeeper.
 * <p>
 * It shares it's cancelled state with the original InventoryClickEvent:<br>
 * Canceling the click event will cancel the trade and canceling the trade is implemented by canceling the original
 * click event.<br>
 * It is recommended to not modify the original click event (besides the cancel state).<br>
 * Also note that the shopkeeper has the final say on whether the trade will be cancelled: It is quite possible that
 * this event is not cancelled but the shopkeeper cancels the trade afterwards nevertheless for some reason. So if you
 * are interested in the actual outcome of the trade take a look at the ShopkeeperTradeCompletedEvent.
 * </p>
 */
public class ShopkeeperTradeEvent extends Event implements Cancellable {

	private final Shopkeeper shopkeeper;
	private final Player player;
	private final InventoryClickEvent clickEvent;
	private final ItemStack[] tradeRecipe;

	public ShopkeeperTradeEvent(Shopkeeper shopkeeper, Player player, InventoryClickEvent clickEvent, ItemStack[] tradeRecipe) {
		this.shopkeeper = shopkeeper;
		this.player = player;
		this.clickEvent = clickEvent;
		this.tradeRecipe = tradeRecipe;
	}

	/**
	 * Gets the trading player.
	 * 
	 * @return the player involved in this trade
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the trading {@link Shopkeeper}.
	 * 
	 * @return the {@link Shopkeeper} involved in this trade
	 */
	public Shopkeeper getShopkeeper() {
		return shopkeeper;
	}

	/**
	 * Gets the {@link InventoryClickEvent} which originally triggered this trade.
	 * 
	 * <p>
	 * Do not modify the returned event!
	 * </p>
	 * 
	 * @return the original {@link InventoryClickEvent}
	 */
	public InventoryClickEvent getClickEvent() {
		return clickEvent;
	}

	/**
	 * Gets the recipe containing the required (index 0 and 1) and the resulting (index 2) {@link ItemStack}s of this
	 * trade.
	 * 
	 * <p>
	 * Do not modify this array, nor the items it contains!<br>
	 * In case you want to use the items of this trade for something else, create copies of the provided
	 * {@link ItemStack}s and use those copies instead.
	 * </p>
	 * 
	 * @return the first item of the trading recipe
	 */
	public ItemStack[] getTradeRecipe() {
		return tradeRecipe;
	}

	/**
	 * If cancelled the trade will not take place.
	 */
	@Override
	public boolean isCancelled() {
		return clickEvent.isCancelled();
	}

	/**
	 * If cancelled the trade will not take place.
	 */
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
