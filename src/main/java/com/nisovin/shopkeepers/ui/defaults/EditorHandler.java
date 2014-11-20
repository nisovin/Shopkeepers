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
import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;
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
		return shopkeeper;
	}

	@Override
	protected boolean canOpen(Player player) {
		assert player != null;
		return this.getShopkeeper().getType().hasPermission(player);
		// owner is checked in the PlayerShopkeeper specific EditorHandler
	}

	@Override
	public boolean isWindow(Inventory inventory) {
		return inventory != null && inventory.getTitle().equals(Settings.editorTitle);
	}

	@Override
	protected void onInventoryClose(InventoryCloseEvent event, Player player) {
		this.saveEditor(event.getInventory(), player);
		shopkeeper.closeAllOpenWindows();
		ShopkeepersPlugin.getInstance().save();
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
			if (Settings.deletingPlayerShopReturnsCreationItem && shopkeeper.getType().isPlayerShopType()) {
				ItemStack creationItem = Settings.createCreationItem();
				Map<Integer, ItemStack> remaining = player.getInventory().addItem(creationItem);
				if (!remaining.isEmpty()) {
					player.getWorld().dropItem(shopkeeper.getActualLocation(), creationItem);
				}
			}

			// delete shopkeeper:
			shopkeeper.delete(); // this also closes all open windows for this shopkeeper

			// run event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperDeletedEvent(player, shopkeeper));

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
				shopkeeper.getShopObject().setItem(event.getCursor().clone());
			} else {
				shopkeeper.getShopObject().cycleSubType();
				ItemStack typeItem = shopkeeper.getShopObject().getSubTypeItem();
				if (typeItem != null) {
					event.getInventory().setItem(17, Utils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
				}
			}

			// run event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(player, shopkeeper));
			// save:
			ShopkeepersPlugin.getInstance().save();
		} else if (slot == 8) {
			if (!Settings.allowRenamingOfPlayerNpcShops && shopkeeper.getType().isPlayerShopType() && shopkeeper.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN) {
				return; // renaming is disabled for citizens player shops
				// TODO restructure this all, to allow for dynamic editor buttons depending on shop (object) types and settings
			}
			// name button - ask for new name:
			event.setCancelled(true);
			this.saveEditor(event.getInventory(), player);
			this.onClose(player); //
			// close editor window and ask for new name
			Utils.closeInventoryLater(player);
			shopkeeper.startNaming(player);
			Utils.sendMessage(player, Settings.msgTypeNewName);
			// run event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(player, shopkeeper));
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
		// no naming button for citizens player shops if renaming id disabled for those
		if (Settings.allowRenamingOfPlayerNpcShops || !shopkeeper.getType().isPlayerShopType() || shopkeeper.getShopObject().getObjectType() != DefaultShopObjectTypes.CITIZEN) {
			inventory.setItem(8, Settings.createNameButtonItem());
			// TODO restructure this, so that the button types can be registered and unregistered (instead of this condition check here)
		}
		ItemStack typeItem = shopkeeper.getShopObject().getSubTypeItem();
		if (typeItem != null) {
			inventory.setItem(17, Utils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
		}
		inventory.setItem(26, Settings.createDeleteButtonItem());
	}
}