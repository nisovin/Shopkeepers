package com.nisovin.shopkeepers.ui.defaults;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.events.OpenTradeEvent;
import com.nisovin.shopkeepers.events.ShopkeeperTradeCompletedEvent;
import com.nisovin.shopkeepers.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.UIType;

public class TradingHandler extends UIHandler {

	protected final Shopkeeper shopkeeper;

	public TradingHandler(UIType uiManager, Shopkeeper shopkeeper) {
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
		return player.hasPermission(ShopkeepersPlugin.TRADE_PERMISSION);
	}

	@Override
	protected boolean openWindow(Player player) {
		OpenTradeEvent event = new OpenTradeEvent(player, shopkeeper);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			Log.debug("Trade window not opened: cancelled by another plugin");
			return false;
		}
		// open trading window:
		return NMSManager.getProvider().openTradeWindow(shopkeeper, player);
	}

	@Override
	public boolean isWindow(Inventory inventory) {
		return inventory != null && inventory.getName().equals("mob.villager");
	}

	@Override
	protected void onInventoryClose(InventoryCloseEvent event, Player player) {
		// nothing to do by default
	}

	@Override
	protected void onInventoryClick(InventoryClickEvent event, Player player) {
		assert event != null && player != null;
		if (event.isCancelled()) return;

		// prevent unwanted special clicks:
		boolean unwantedSpecialClick = false;
		InventoryAction action = event.getAction();
		if (action == InventoryAction.COLLECT_TO_CURSOR) {
			unwantedSpecialClick = true;
		} else if (event.getRawSlot() == 2) {
			// special clicks on result slot:
			if (!event.isLeftClick() || (event.isShiftClick() && !this.isShiftTradeAllowed(event))) {
				unwantedSpecialClick = true;
			}
		}
		if (unwantedSpecialClick) {
			event.setCancelled(true);
			Utils.updateInventoryLater(player);
			return;
		}

		if (event.getRawSlot() != 2) {
			return;
		}

		ItemStack item = event.getCurrentItem();
		if (item != null) {
			String playerName = player.getName();
			Inventory inventory = event.getInventory();

			// verify purchase:
			ItemStack item1 = inventory.getItem(0);
			ItemStack item2 = inventory.getItem(1);

			boolean ok = false;
			List<ItemStack[]> recipes = shopkeeper.getRecipes();
			ItemStack[] selectedRecipe = null;

			int currentRecipePage = NMSManager.getProvider().getCurrentRecipePage(inventory);
			if (currentRecipePage >= 0 && currentRecipePage < recipes.size()) {
				// scan the current recipe:
				selectedRecipe = recipes.get(currentRecipePage);
				if (this.itemEqualsAtLeast(item1, selectedRecipe[0], true) && this.itemEqualsAtLeast(item2, selectedRecipe[1], true) && this.itemEqualsAtLeast(item, selectedRecipe[2], false)) {
					ok = true;
				}
			} else {
				// scan all recipes:
				for (ItemStack[] recipe : recipes) {
					if (this.itemEqualsAtLeast(item1, recipe[0], true) && this.itemEqualsAtLeast(item2, recipe[1], true) && this.itemEqualsAtLeast(item, recipe[2], false)) {
						ok = true;
						selectedRecipe = recipe;
						break;
					}
				}
			}
			if (!ok) {
				if (Log.isDebug()) { // additional check so we don't do the item comparisons if not really needed
					Log.debug("Invalid trade by " + playerName + " with shopkeeper at " + shopkeeper.getPositionString() + ":");
					if (selectedRecipe != null) {
						String notSimilarReason1 = Utils.areSimilarReasoned(item1, selectedRecipe[0]);
						String notSimilarReason2 = Utils.areSimilarReasoned(item2, selectedRecipe[1]);
						String notSimilarReason3 = Utils.areSimilarReasoned(item, selectedRecipe[2]);
						Log.debug("Comparing item slot 0: " + (notSimilarReason1 == null ? "considered similar" : "not similar because '" + notSimilarReason1 + "'"));
						Log.debug("Comparing item slot 1: " + (notSimilarReason2 == null ? "considered similar" : "not similar because '" + notSimilarReason2 + "'"));
						Log.debug("Comparing item slot 2: " + (notSimilarReason3 == null ? "considered similar" : "not similar because '" + notSimilarReason3 + "'"));
					} else {
						Log.debug("No recipe selected or found.");
					}
				}
				event.setCancelled(true);
				Utils.updateInventoryLater(player);
				return;
			}

			ItemStack cursor = event.getCursor();
			if (cursor != null && cursor.getType() != Material.AIR) {
				// minecraft doesn't handle the trading in case the cursor cannot hold the resulting items
				// so we have to make sure that our trading logic is as well not run:
				if (!cursor.isSimilar(selectedRecipe[2]) || cursor.getAmount() + selectedRecipe[2].getAmount() > cursor.getMaxStackSize()) {
					Log.debug("Skip trade by " + playerName + " with shopkeeper at " + shopkeeper.getPositionString() + ": the cursor cannot carry the resulting items");
					event.setCancelled(true); // making sure minecraft really doesn't process the trading
					return;
				}
			}

			// call trade event, giving other plugins a chance to cancel the trade before the shopkeeper processes it:
			ShopkeeperTradeEvent tradeEvent = new ShopkeeperTradeEvent(shopkeeper, player, event);
			Bukkit.getPluginManager().callEvent(tradeEvent);
			if (tradeEvent.isCancelled()) {
				Log.debug("Trade was cancelled by some other plugin.");
				return;
			}

			// let shopkeeper handle the purchase click: // TODO maybe pass selectedRecipe to this method?
			this.onPurchaseClick(event, player);

			// log purchase:
			if (Settings.enablePurchaseLogging && !event.isCancelled()) {
				// TODO maybe move this somewhere else:
				try {
					String owner = (shopkeeper instanceof PlayerShopkeeper) ? ((PlayerShopkeeper) shopkeeper).getOwnerAsString() : "[Admin]";
					File file = new File(ShopkeepersPlugin.getInstance().getDataFolder(), "purchases-" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".csv");
					boolean isNew = !file.exists();
					BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
					if (isNew) writer.append("TIME,PLAYER,SHOP TYPE,SHOP POS,OWNER,ITEM TYPE,DATA,QUANTITY,CURRENCY 1,CURRENCY 2\n");
					writer.append("\"" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\",\"" + playerName + "\",\"" + shopkeeper.getType().getIdentifier()
							+ "\",\"" + shopkeeper.getPositionString() + "\",\"" + owner + "\",\"" + item.getType().name() + "\",\"" + item.getDurability()
							+ "\",\"" + item.getAmount() + "\",\"" + (item1 != null ? item1.getType().name() + ":" + item1.getDurability() : "")
							+ "\",\"" + (item2 != null ? item2.getType().name() + ":" + item2.getDurability() : "") + "\"\n");
					writer.close();
				} catch (IOException e) {
					Log.severe("IO exception while trying to log purchase");
				}
			}

			// call trade-completed event:
			ShopkeeperTradeCompletedEvent tradeCompletedEvent = new ShopkeeperTradeCompletedEvent(shopkeeper, player, event);
			Bukkit.getPluginManager().callEvent(tradeCompletedEvent);
		}
	}

	// whether or not the player can buy via shift click on the result slot:
	protected boolean isShiftTradeAllowed(InventoryClickEvent event) {
		return false; // not allowed by default, just in case
	}

	protected void onPurchaseClick(InventoryClickEvent event, Player player) {
		// nothing to do by default
	}

	private boolean itemEqualsAtLeast(ItemStack item1, ItemStack item2, boolean checkAmount) {
		if (!Utils.areSimilar(item1, item2)) {
			return false;
		}

		return (!checkAmount || item1 == null || item1.getAmount() >= item2.getAmount());
	}

	protected int getAmountAfterTaxes(int amount) {
		if (Settings.taxRate == 0) return amount;
		int taxes = 0;
		if (Settings.taxRoundUp) {
			taxes = (int) Math.ceil((double) amount * (Settings.taxRate / 100F));
		} else {
			taxes = (int) Math.floor((double) amount * (Settings.taxRate / 100F));
		}
		return amount - taxes;
	}
}