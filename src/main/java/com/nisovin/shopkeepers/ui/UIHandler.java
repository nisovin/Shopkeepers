package com.nisovin.shopkeepers.ui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import com.nisovin.shopkeepers.Shopkeeper;

/**
 * The component which handles one specific type of user interface window for one specific shopkeeper.
 */
public abstract class UIHandler {

	protected final UIManager uiManager;

	protected UIHandler(UIManager uiManager) {
		this.uiManager = uiManager;
	}

	public UIManager getUIManager() {
		return this.uiManager;
	}

	/**
	 * Informs the uiManager that the window is/gets closed for this player.
	 * This is only needed if the inventory gets manually closed by a plugin.
	 * 
	 * @param player
	 *            the player whose user interface inventory gets closed
	 */
	protected void onClose(Player player) {
		assert player != null;
		this.uiManager.onClose(player);
	}

	/**
	 * Gets the shopkeeper for which this object is handling the specific interface type for.
	 * 
	 * @return the shopkeeper
	 */
	public abstract Shopkeeper getShopkeeper();

	/**
	 * Checks whether or not the given player can open the handled interface for this shopkeeper.
	 * This for example gets called when a player requests the interface for this shopkeeper.
	 * It should perform the necessary permission checks.
	 * 
	 * @param player
	 *            a player
	 * @return true, if the given player is allowed to open the interface window type this class is handling
	 */
	protected abstract boolean canOpen(Player player);

	/**
	 * This method should open the interface window for the given player.
	 * Generally {@link #canOpen(Player) canOpen} should be checked before this method gets called,
	 * however this method should not rely on that.
	 * 
	 * @param player
	 *            a player
	 * @return true, if the interface window was successfully opened
	 */
	protected abstract boolean openWindow(Player player);

	/**
	 * Checks whether or not the given inventory is a custom inventory created by this handler (for example
	 * by comparing the inventory titles). The result of this method gets checked before any inventory events
	 * are passed through to this handler.
	 * 
	 * @param inventory
	 *            an inventory
	 * @return true, if the given inventory is representing a custom interface window created and handled by this handler
	 */
	public abstract boolean isWindow(Inventory inventory);

	// handling of interface window interaction

	/**
	 * Called when a player closes an inventory for which (@link #isInterface(Inventory) isInterface) returned true.
	 * It is not guaranteed that this method gets called for all user interface windows which were opened by this handler (for example
	 * plugin triggered closing of a players inventory might not trigger a call to this method). So don't rely on it for cleanup.
	 * 
	 * @param event
	 *            the event which triggered this method call
	 * @param player
	 *            the player who closed the inventory
	 */
	protected abstract void onInventoryClose(InventoryCloseEvent event, Player player);

	/**
	 * Called when a player triggers an InventoryClickEvent for an inventory for which (@link #isInterface(Inventory) isInterface)
	 * returned true.
	 * 
	 * @param event
	 *            the event which triggered this method call
	 * @param player
	 *            the clicking player
	 */
	protected abstract void onInventoryClick(InventoryClickEvent event, Player player);
}