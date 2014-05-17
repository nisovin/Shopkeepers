package com.nisovin.shopkeepers.ui.defaults;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.UIManager;

public abstract class HiringHandler extends UIHandler {

	protected final Shopkeeper shopkeeper;

	protected HiringHandler(UIManager uiManager, Shopkeeper shopkeeper) {
		super(uiManager);
		this.shopkeeper = shopkeeper;
	}

	@Override
	public Shopkeeper getShopkeeper() {
		return this.shopkeeper;
	}

	@Override
	protected boolean canOpen(Player player) {
		assert player != null;
		// only allowed to hire, if the player has the permission to create shopkeepers of that type
		// (this permission is also needed for editing of the shopkeeper)
		return this.getShopkeeper().getType().hasPermission(player) && player.hasPermission("shopkeeper.hire");
	}

	@Override
	public boolean isInterface(Inventory inventory) {
		return inventory != null && inventory.getTitle().equals(Settings.forHireTitle);
	}

	@Override
	protected void onInventoryClose(InventoryCloseEvent event, Player player) {
		// nothing to do by default
	}

	@Override
	protected void onInventoryClick(InventoryClickEvent event, Player player) {
		assert event != null && player != null;
		event.setCancelled(true);
	}
}