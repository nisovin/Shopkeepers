package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.ItemCount;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.shoptypes.offers.TradingOffer;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;

public class TradingPlayerShopkeeper extends PlayerShopkeeper {

	protected static class TradingPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected TradingPlayerShopEditorHandler(UIType uiType, TradingPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected boolean openWindow(Player player) {
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add the shopkeeper's offers:
			List<ItemCount> chestItems = ((TradingPlayerShopkeeper) shopkeeper).getItemsFromChest();
			int column = 0;
			for (ItemCount itemCount : chestItems) {
				ItemStack tradedItem = itemCount.getItem(); // this item is already a copy with amount 1
				TradingOffer offer = ((TradingPlayerShopkeeper) shopkeeper).getOffer(tradedItem);
				if (offer != null) {
					// adjust traded item amount:
					tradedItem.setAmount(offer.getResultItem().getAmount());

					// fill in costs:
					ItemStack item1 = offer.getItem1();
					ItemStack item2 = offer.getItem2();
					if (item1 != null && item1.getType() != Material.AIR && item1.getAmount() > 0) {
						inventory.setItem(column + 9, item1);
					}
					if (item2 != null && item2.getType() != Material.AIR && item2.getAmount() > 0) {
						inventory.setItem(column + 18, item2);
					}
				}

				// fill in traded item:
				inventory.setItem(column, tradedItem);

				column++;
				if (column > 8) break;
			}

			// add the special buttons:
			this.setActionButtons(inventory);
			// show editing inventory:
			player.openInventory(inventory);
			return true;
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			event.setCancelled(true);
			final int slot = event.getRawSlot();
			if (slot >= 0 && slot <= 7) {
				// handle changing sell stack size:
				ItemStack item = event.getCurrentItem();
				if (item != null && item.getType() != Material.AIR) {
					int amount = item.getAmount();
					amount = this.getNewAmountAfterEditorClick(event, amount);
					if (amount <= 0) amount = 1;
					if (amount > item.getMaxStackSize()) amount = item.getMaxStackSize();
					item.setAmount(amount);
				}
			} else if ((slot >= 9 && slot <= 16) || (slot >= 18 && slot <= 25)) {
				if (((TradingPlayerShopkeeper) shopkeeper).clickedItem != null) {
					// placing item:
					final Inventory inventory = event.getInventory();
					Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {
						public void run() {
							inventory.setItem(slot, ((TradingPlayerShopkeeper) shopkeeper).clickedItem);
							((TradingPlayerShopkeeper) shopkeeper).clickedItem = null;
						}
					}, 1);
				} else {
					// changing stack size:
					ItemStack item = event.getCurrentItem();
					if (item != null && item.getType() != Material.AIR) {
						int amount = item.getAmount();
						amount = this.getNewAmountAfterEditorClick(event, amount);
						if (amount <= 0) {
							event.getInventory().setItem(slot, null);
						} else {
							if (amount > item.getMaxStackSize()) amount = item.getMaxStackSize();
							item.setAmount(amount);
						}
					}
				}
			} else if (slot >= 27) {
				// clicking in player inventory:
				if (event.isShiftClick() || !event.isLeftClick()) {
					return;
				}
				ItemStack cursor = event.getCursor();
				if (cursor != null && cursor.getType() != Material.AIR) {
					return;
				}
				ItemStack current = event.getCurrentItem();
				if (current != null && current.getType() != Material.AIR) {
					((TradingPlayerShopkeeper) shopkeeper).clickedItem = current.clone();
					((TradingPlayerShopkeeper) shopkeeper).clickedItem.setAmount(1);
				}
			} else {
				super.onInventoryClick(event, player);
			}
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			for (int column = 0; column < 8; column++) {
				ItemStack item = inventory.getItem(column);
				if (item != null && item.getType() != Material.AIR) {
					ItemStack cost1 = null, cost2 = null;
					ItemStack item1 = inventory.getItem(column + 9);
					ItemStack item2 = inventory.getItem(column + 18);
					if (item1 != null && item1.getType() != Material.AIR) {
						cost1 = item1;
						if (item2 != null && item2.getType() != Material.AIR) {
							cost2 = item2;
						}
					} else if (item2 != null && item2.getType() != Material.AIR) {
						cost1 = item2;
					}
					if (cost1 != null) {
						((TradingPlayerShopkeeper) shopkeeper).addOffer(item, cost1, cost2);
					} else {
						((TradingPlayerShopkeeper) shopkeeper).removeOffer(item);
					}
				}
			}
			((TradingPlayerShopkeeper) shopkeeper).clickedItem = null;
		}
	}

	protected static class TradingPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected TradingPlayerShopTradingHandler(UIType uiType, TradingPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player, ItemStack[] usedRecipe, ItemStack offered1, ItemStack offered2) {
			super.onPurchaseClick(event, player, usedRecipe, offered1, offered2);
			if (event.isCancelled()) return;

			// get chest:
			Block chest = ((TradingPlayerShopkeeper) shopkeeper).getChest();
			if (!Utils.isChest(chest.getType())) {
				event.setCancelled(true);
				return;
			}

			// remove result item from chest:
			ItemStack resultItem = usedRecipe[2];
			assert resultItem != null;
			Inventory inventory = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inventory.getContents();
			if (Utils.removeItems(contents, resultItem) != 0) {
				event.setCancelled(true);
				return;
			}

			// add traded items to chest:
			for (int i = 0; i < 2; i++) {
				ItemStack requiredItem = usedRecipe[i];
				if (requiredItem == null || requiredItem.getType() == Material.AIR) continue;
				int amountAfterTaxes = this.getAmountAfterTaxes(requiredItem.getAmount());
				if (amountAfterTaxes > 0) {
					// the items the trading player gave might slightly differ from the required items,
					// but are still accepted, depending on item comparison and settings:
					ItemStack receivedItem = (i == 0 ? offered1 : offered2);
					receivedItem = receivedItem.clone(); // create a copy, just in case
					receivedItem.setAmount(amountAfterTaxes);
					if (Utils.addItems(contents, receivedItem) != 0) {
						event.setCancelled(true);
						return;
					}
				}
			}

			// save chest contents:
			inventory.setContents(contents);
		}
	}

	private final List<TradingOffer> offers = new ArrayList<TradingOffer>();
	private ItemStack clickedItem;

	/**
	 * For use in extending classes.
	 */
	protected TradingPlayerShopkeeper() {
	}

	public TradingPlayerShopkeeper(ConfigurationSection config) {
		this.initOnLoad(config);
		this.onInitDone();
	}

	public TradingPlayerShopkeeper(ShopCreationData creationData) {
		this.initOnCreation(creationData);
		this.onInitDone();
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new TradingPlayerShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new TradingPlayerShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		// load offers:
		offers.clear();
		// legacy: load offers from old costs section
		offers.addAll(TradingOffer.loadFromConfigOld(config, "costs"));
		offers.addAll(TradingOffer.loadFromConfig(config, "offers"));
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		// save offers:
		TradingOffer.saveToConfig(config, "offers", offers);
	}

	@Override
	public ShopType<TradingPlayerShopkeeper> getType() {
		return DefaultShopTypes.PLAYER_TRADE;
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		List<ItemCount> chestItems = this.getItemsFromChest();
		for (TradingOffer offer : offers) {
			ItemStack resultItem = offer.getResultItem();
			ItemCount itemCount = ItemCount.findSimilar(chestItems, resultItem);
			if (itemCount != null) {
				int chestAmt = itemCount.getAmount();
				if (chestAmt >= resultItem.getAmount()) {
					ItemStack item1 = offer.getItem1();
					ItemStack item2 = offer.getItem2();

					ItemStack[] recipe = new ItemStack[3];
					int slot = 0;
					if (item1 != null && item1.getType() != Material.AIR && item1.getAmount() > 0) {
						recipe[slot++] = item1.clone();
					}
					if (item2 != null && item2.getType() != Material.AIR && item2.getAmount() > 0) {
						recipe[slot] = item2.clone();
					}
					recipe[2] = resultItem.clone();
					recipes.add(recipe);
				}
			}
		}
		return recipes;
	}

	public TradingOffer getOffer(ItemStack item) {
		for (TradingOffer offer : offers) {
			if (Utils.isSimilar(offer.getResultItem(), item)) {
				return offer;
			}
		}
		return null;
	}

	public TradingOffer addOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		assert resultItem != null;
		assert item1 != null;
		// remove multiple offers for the same item:
		this.removeOffer(resultItem);

		// making copies from item stacks, just in case the provided items are used elsewhere as well:
		TradingOffer newOffer = new TradingOffer(resultItem.clone(), item1.clone(), item2 != null ? item2.clone() : null);
		offers.add(newOffer);
		return newOffer;
	}

	public void clearOffers() {
		offers.clear();
	}

	public void removeOffer(ItemStack item) {
		Iterator<TradingOffer> iter = offers.iterator();
		while (iter.hasNext()) {
			if (Utils.isSimilar(iter.next().getResultItem(), item)) {
				iter.remove();
				return;
			}
		}
	}

	private List<ItemCount> getItemsFromChest() {
		return this.getItemsFromChest(null);
	}
}