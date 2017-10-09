package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.nisovin.shopkeepers.ShopkeeperCreateException;
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
		public TradingPlayerShopkeeper getShopkeeper() {
			return (TradingPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			final TradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add the shopkeeper's offers:
			List<ItemCount> chestItems = shopkeeper.getItemsFromChest();
			int column = 0;
			for (ItemCount itemCount : chestItems) {
				ItemStack tradedItem = itemCount.getItem(); // this item is already a copy with amount 1
				TradingOffer offer = shopkeeper.getOffer(tradedItem);
				if (offer != null) {
					// adjust traded item amount:
					tradedItem.setAmount(offer.getResultItem().getAmount());

					// fill in costs:
					ItemStack item1 = offer.getItem1();
					ItemStack item2 = offer.getItem2();
					if (!Utils.isEmpty(item1)) {
						inventory.setItem(column + 9, item1);
					}
					if (!Utils.isEmpty(item2)) {
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
			final TradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			event.setCancelled(true);
			final int slot = event.getRawSlot();
			if (slot >= 0 && slot <= 7) {
				// handle changing sell stack size:
				this.handleUpdateItemAmountOnClick(event, 1);
			} else if ((slot >= 9 && slot <= 16) || (slot >= 18 && slot <= 25)) {
				if (shopkeeper.clickedItem != null) {
					// placing item:
					final Inventory inventory = event.getInventory();
					Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {
						public void run() {
							inventory.setItem(slot, shopkeeper.clickedItem);
							shopkeeper.clickedItem = null;
						}
					}, 1);
				} else {
					// changing stack size of clicked item:
					this.handleUpdateItemAmountOnClick(event, 0);
				}
			} else if (slot >= 27) {
				// clicking in player inventory:
				if (event.isShiftClick() || !event.isLeftClick()) {
					return;
				}
				ItemStack cursor = event.getCursor();
				if (!Utils.isEmpty(cursor)) {
					return;
				}
				ItemStack current = event.getCurrentItem();
				if (!Utils.isEmpty(current)) {
					shopkeeper.clickedItem = current.clone();
					shopkeeper.clickedItem.setAmount(1);
				}
			} else {
				super.onInventoryClick(event, player);
			}
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			final TradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			for (int column = 0; column < 8; column++) {
				ItemStack resultItem = inventory.getItem(column);
				if (Utils.isEmpty(resultItem)) continue; // not valid recipe column

				ItemStack cost1 = Utils.getNullIfEmpty(inventory.getItem(column + 9));
				ItemStack cost2 = Utils.getNullIfEmpty(inventory.getItem(column + 18));

				// handle cost2 item as cost1 item if there is no cost1 item:
				if (cost1 == null) {
					cost1 = cost2;
					cost2 = null;
				}

				// add or remove offer:
				if (cost1 != null) {
					shopkeeper.addOffer(resultItem, cost1, cost2);
				} else {
					shopkeeper.removeOffer(resultItem);
				}
			}
			shopkeeper.clickedItem = null;
		}
	}

	protected static class TradingPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected TradingPlayerShopTradingHandler(UIType uiType, TradingPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		public TradingPlayerShopkeeper getShopkeeper() {
			return (TradingPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player, ItemStack[] usedRecipe, ItemStack offered1, ItemStack offered2) {
			super.onPurchaseClick(event, player, usedRecipe, offered1, offered2);
			if (event.isCancelled()) return;
			final TradingPlayerShopkeeper shopkeeper = this.getShopkeeper();

			// get chest:
			Block chest = shopkeeper.getChest();
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
				if (Utils.isEmpty(requiredItem)) continue;
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

	// contains only one offer for a specific type of item:
	private final List<TradingOffer> offers = new ArrayList<TradingOffer>();
	private final List<TradingOffer> offersView = Collections.unmodifiableList(offers);

	// TODO conflicts if multiple players are editing at the same time
	// TODO maybe enforce only one editor at the same time? (currently shop owner and admins can edit at the same time)
	private ItemStack clickedItem;

	/**
	 * For use in extending classes.
	 */
	protected TradingPlayerShopkeeper() {
	}

	public TradingPlayerShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.initOnLoad(config);
		this.onInitDone();
	}

	public TradingPlayerShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
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
	protected void load(ConfigurationSection config) throws ShopkeeperCreateException {
		super.load(config);
		// load offers:
		this.clearOffers();
		// TODO remove legacy: load offers from old costs section
		this.addOffers(TradingOffer.loadFromConfigOld(config, "costs"));
		this.addOffers(TradingOffer.loadFromConfig(config, "offers"));
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		// save offers:
		TradingOffer.saveToConfig(config, "offers", this.getOffers());
	}

	@Override
	public ShopType<?> getType() {
		return DefaultShopTypes.PLAYER_TRADING();
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		List<ItemCount> chestItems = this.getItemsFromChest();
		for (TradingOffer offer : this.getOffers()) {
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

	private List<ItemCount> getItemsFromChest() {
		return this.getItemsFromChest(null);
	}

	// OFFERS:

	public List<TradingOffer> getOffers() {
		return offersView;
	}

	public TradingOffer getOffer(ItemStack tradedItem) {
		for (TradingOffer offer : this.getOffers()) {
			if (Utils.isSimilar(offer.getResultItem(), tradedItem)) {
				return offer;
			}
		}
		return null;
	}

	public TradingOffer addOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		// create offer (also handles validation):
		TradingOffer newOffer = new TradingOffer(resultItem, item1, item2);

		// add new offer (replacing any previous offer for the same item):
		this.addOffer(newOffer);
		return newOffer;
	}

	private void addOffer(TradingOffer offer) {
		assert offer != null;
		// remove previous offer for the same item:
		this.removeOffer(offer.getResultItem());
		offers.add(offer);
	}

	private void addOffers(Collection<TradingOffer> offers) {
		assert offers != null;
		for (TradingOffer offer : offers) {
			if (offer == null) continue; // skip invalid entries
			// add new offer (replacing any previous offer for the same item):
			this.addOffer(offer);
		}
	}

	public void clearOffers() {
		offers.clear();
	}

	public void removeOffer(ItemStack tradedItem) {
		Iterator<TradingOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			if (Utils.isSimilar(iterator.next().getResultItem(), tradedItem)) {
				iterator.remove();
				break;
			}
		}
	}
}
