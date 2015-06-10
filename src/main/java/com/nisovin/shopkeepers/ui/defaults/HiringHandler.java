package com.nisovin.shopkeepers.ui.defaults;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersAPI;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.UIType;

public abstract class HiringHandler extends UIHandler {

	protected HiringHandler(UIType uiType, Shopkeeper shopkeeper) {
		super(uiType, shopkeeper);
	}

	@Override
	protected boolean canOpen(Player player) {
		assert player != null;
		// only allowed to hire, if the player also has the permission to create shopkeepers of that type
		return this.getShopkeeper().getType().hasPermission(player) && Utils.hasPermission(player, ShopkeepersAPI.HIRE_PERMISSION);
	}

	@Override
	public boolean isWindow(Inventory inventory) {
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