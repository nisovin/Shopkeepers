package com.nisovin.shopkeepers.ui.defaults;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.events.ShopkeeperDeletedEvent;
import com.nisovin.shopkeepers.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.UIManager;

public abstract class EditorHandler extends UIHandler {

	protected final Shopkeeper shopkeeper;

	protected EditorHandler(UIManager uiManager, Shopkeeper shopkeeper) {
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
		return this.getShopkeeper().getType().hasPermission(player); //TODO is this correct?
	}

	@Override
	public boolean isWindow(Inventory inventory) {
		return inventory != null && inventory.getTitle().equals(Settings.editorTitle);
	}

	@Override
	protected void onInventoryClose(InventoryCloseEvent event, Player player) {
		this.saveEditor(event.getInventory(), player);
		this.shopkeeper.closeAllOpenWindows();
	}

	@Override
	protected void onInventoryClick(InventoryClickEvent event, Player player) {
		assert event != null && player != null;
		// check for special action buttons:
		int slot = event.getRawSlot();
		if (slot == 26) {
			// delete button - delete shopkeeper:
			event.setCancelled(true);

			// return creation item for player shopkeepers:
			if (Settings.deletingPlayerShopReturnsCreationItem && this.shopkeeper.getType().isPlayerShopType()) {
				ItemStack creationItem = Settings.createCreationItem();
				Map<Integer, ItemStack> remaining = player.getInventory().addItem(creationItem);
				if (!remaining.isEmpty()) {
					player.getWorld().dropItem(this.shopkeeper.getActualLocation(), creationItem);
				}
			}

			// delete shopkeeper:
			this.shopkeeper.delete(); // this also closes all open windows for this shopkeeper

			// run event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperDeletedEvent(player, this.shopkeeper));

			// save:
			ShopkeepersPlugin.getInstance().save();
			/*
			 * } else if (result == DefaultUIs.EDITOR_BUTTONS.DONE) { // is this actually used anywhere?
			 * this.saveEditor(event.getInventory(), player);
			 * // end the editing session:
			 * shopkeeper.closeAllOpenWindows();
			 * // run event:
			 * Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(player, shopkeeper));
			 * // save:
			 * ShopkeepersPlugin.getInstance().save();
			 */
		} else if (slot == 17) {
			// cycle button - cycle to next object type variation:
			event.setCancelled(true);

			if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
				this.shopkeeper.getShopObject().setItem(event.getCursor().clone());
			} else {
				this.shopkeeper.getShopObject().cycleSubType();
				ItemStack typeItem = this.shopkeeper.getShopObject().getSubTypeItem();
				if (typeItem != null) {
					event.getInventory().setItem(17, Utils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
				}
			}

			// run event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(player, this.shopkeeper));
			// save:
			ShopkeepersPlugin.getInstance().save();
		} else if (slot == 8) {
			// name button - ask for new name:
			event.setCancelled(true);
			this.saveEditor(event.getInventory(), player);
			this.onClose(player); //
			// close editor window and ask for new name
			Utils.closeInventoryLater(player);
			this.shopkeeper.startNaming(player);
			Utils.sendMessage(player, Settings.msgTypeNewName);
			// run event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(player, this.shopkeeper));
			// save:
			ShopkeepersPlugin.getInstance().save();
		}
	}

	/**
	 * Saves the current state of the editor interface.
	 * 
	 * @param inventory
	 *            the inventory of the editor window
	 * @param player
	 *            the editing player
	 */
	protected abstract void saveEditor(Inventory inventory, Player player);

	protected int getNewAmountAfterEditorClick(InventoryClickEvent event, int amount) {
		if (event.isLeftClick()) {
			if (event.isShiftClick()) {
				amount += 10;
			} else {
				amount += 1;
			}
		} else if (event.isRightClick()) {
			if (event.isShiftClick()) {
				amount -= 10;
			} else {
				amount -= 1;
			}
		} else if (event.getClick() == ClickType.MIDDLE) {
			amount = 64;
		} else if (event.getHotbarButton() >= 0) {
			amount = event.getHotbarButton();
		}
		return amount;
	}

	protected void setActionButtons(Inventory inventory) {
		inventory.setItem(8, Settings.createNameButtonItem());
		ItemStack typeItem = this.shopkeeper.getShopObject().getSubTypeItem();
		if (typeItem != null) {
			inventory.setItem(17, Utils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
		}
		inventory.setItem(26, Settings.createDeleteButtonItem());
	}
}