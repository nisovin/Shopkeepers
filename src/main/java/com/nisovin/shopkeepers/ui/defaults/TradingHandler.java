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
import org.bukkit.inventory.meta.ItemMeta;
import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.events.OpenTradeEvent;
import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.UIManager;

public class TradingHandler extends UIHandler {

	protected final Shopkeeper shopkeeper;

	public TradingHandler(UIManager uiManager, Shopkeeper shopkeeper) {
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
		return player.hasPermission("shopkeeper.trade");
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
		// prevent unwanted special clicks
		InventoryAction action = event.getAction();
		if (action == InventoryAction.COLLECT_TO_CURSOR || (event.getRawSlot() == 2 && !event.isLeftClick())) {
			event.setCancelled(true);
			Utils.updateInventoryLater(player);
			return;
		}

		if (event.getRawSlot() != 2) {
			return;
		}

		String playerName = player.getName();
		ItemStack cursor = event.getCursor();
		if (cursor != null && cursor.getType() != Material.AIR && cursor.getAmount() >= cursor.getMaxStackSize()) {
			// minecraft doesn't handle the trading in this case, so we have to make sure our additional trading logic is not run as well:
			Log.debug("Skip trade by " + playerName + " with shopkeeper at " + shopkeeper.getPositionString() + ": cursor already has max stack size");
			return;
		}

		ItemStack item = event.getCurrentItem();
		if (item != null) {
			Inventory inventory = event.getInventory();

			// verify purchase
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
						break;
					}
				}
			}
			if (!ok) {
				Log.debug("Invalid trade by " + playerName + " with shopkeeper at " + shopkeeper.getPositionString() + ":");
				Log.debug("  " + this.itemStackToString(item1) + " and " + this.itemStackToString(item2) + " for " + this.itemStackToString(item));
				if (selectedRecipe != null) {
					Log.debug("  Required:" + this.itemStackToString(selectedRecipe[0]) + " and " + this.itemStackToString(selectedRecipe[1]));
				}
				event.setCancelled(true);
				Utils.updateInventoryLater(player);
				return;
			}

			// handle purchase click:
			this.onPurchaseClick(event, player);

			// log purchase
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
		}
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

	private String getNameOfItem(ItemStack item) {
		if (item != null && item.getType() != Material.AIR && item.hasItemMeta()) {
			ItemMeta meta = item.getItemMeta();
			if (meta.hasDisplayName()) {
				return meta.getDisplayName();
			}
		}
		return "";
	}

	private String itemStackToString(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) return "(nothing)";
		String displayName = this.getNameOfItem(item);
		StringBuilder result = new StringBuilder();
		result.append(item.getType().name()).append(':').append(item.getDurability());
		if (!displayName.isEmpty()) result.append(':').append(displayName);
		if (Log.isDebug()) {
			// append more, detailed (possibly ugly) information:
			result.append(':').append(item.getItemMeta().toString()); // TODO improve (ex. printing attributes info etc.)
		}
		return result.toString();
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