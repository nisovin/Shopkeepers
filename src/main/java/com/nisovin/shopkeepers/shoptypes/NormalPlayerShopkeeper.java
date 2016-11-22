package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.Arrays;
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

import com.nisovin.shopkeepers.Filter;
import com.nisovin.shopkeepers.ItemCount;
import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.shoptypes.offers.PriceOffer;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;

public class NormalPlayerShopkeeper extends PlayerShopkeeper {

	protected static class NormalPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected NormalPlayerShopEditorHandler(UIType uiType, NormalPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		public NormalPlayerShopkeeper getShopkeeper() {
			return (NormalPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			final NormalPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add offers:
			List<ItemCount> chestItems = shopkeeper.getItemsFromChest();
			int column = 0;
			for (ItemCount itemCount : chestItems) {
				ItemStack item = itemCount.getItem(); // this item is already a copy with amount 1
				int price = 0;
				PriceOffer offer = shopkeeper.getOffer(item);
				if (offer != null) {
					price = offer.getPrice();
					item.setAmount(offer.getItem().getAmount());
				}

				// add offer to inventory:
				inventory.setItem(column, item);
				this.setEditColumnCost(inventory, column, price);

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
			if (event.getRawSlot() >= 0 && event.getRawSlot() <= 7) {
				// handle changing sell stack size:
				this.handleUpdateItemAmountOnClick(event, 1);
			} else {
				super.onInventoryClick(event, player);
			}
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			final NormalPlayerShopkeeper shopkeeper = this.getShopkeeper();
			for (int column = 0; column < 8; column++) {
				ItemStack tradedItem = inventory.getItem(column);
				if (!Utils.isEmpty(tradedItem)) {
					int price = this.getPriceFromColumn(inventory, column);
					if (price > 0) {
						shopkeeper.addOffer(tradedItem, price);
					} else {
						shopkeeper.removeOffer(tradedItem);
					}
				}
			}
		}
	}

	protected static class NormalPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected NormalPlayerShopTradingHandler(UIType uiManager, NormalPlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		public NormalPlayerShopkeeper getShopkeeper() {
			return (NormalPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player, ItemStack[] usedRecipe, ItemStack offered1, ItemStack offered2) {
			super.onPurchaseClick(event, player, usedRecipe, offered1, offered2);
			if (event.isCancelled()) return;
			final NormalPlayerShopkeeper shopkeeper = this.getShopkeeper();

			// get offer for this type of item:
			ItemStack resultItem = usedRecipe[2];
			PriceOffer offer = shopkeeper.getOffer(resultItem);
			if (offer == null) {
				// this should not happen.. because the recipes were created based on the shopkeeper's offers
				event.setCancelled(true);
				return;
			}

			int tradedItemAmount = offer.getItem().getAmount();
			if (tradedItemAmount != resultItem.getAmount()) {
				// this shouldn't happen .. because the recipe was created based on this offer
				event.setCancelled(true);
				return;
			}

			// get chest:
			Block chest = shopkeeper.getChest();
			if (!Utils.isChest(chest.getType())) {
				event.setCancelled(true);
				return;
			}

			// remove result items from chest:
			Inventory inventory = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inventory.getContents();
			contents = Arrays.copyOf(contents, contents.length);
			if (Utils.removeItems(contents, resultItem) != 0) {
				Log.debug("Chest does not contain the required items.");
				event.setCancelled(true);
				return;
			}

			// add earnings to chest:
			// TODO maybe add the actual items the trading player gave, instead of creating new currency items?
			int amount = this.getAmountAfterTaxes(offer.getPrice());
			if (amount > 0) {
				if (Settings.highCurrencyItem == Material.AIR || offer.getPrice() <= Settings.highCurrencyMinCost) {
					if (Utils.addItems(contents, Settings.createCurrencyItem(amount)) != 0) {
						Log.debug("Chest cannot hold the given items.");
						event.setCancelled(true);
						return;
					}
				} else {
					int highCost = amount / Settings.highCurrencyValue;
					int lowCost = amount % Settings.highCurrencyValue;
					if (highCost > 0) {
						if (Utils.addItems(contents, Settings.createHighCurrencyItem(highCost)) != 0) {
							Log.debug("Chest cannot hold the given items.");
							event.setCancelled(true);
							return;
						}
					}
					if (lowCost > 0) {
						if (Utils.addItems(contents, Settings.createCurrencyItem(lowCost)) != 0) {
							Log.debug("Chest cannot hold the given items.");
							event.setCancelled(true);
							return;
						}
					}
				}
			}

			// save chest contents:
			inventory.setContents(contents);
		}
	}

	private static final Filter<ItemStack> ITEM_FILTER = new Filter<ItemStack>() {

		@Override
		public boolean accept(ItemStack item) {
			if (Settings.isCurrencyItem(item) || Settings.isHighCurrencyItem(item)) return false;
			return true;
		}
	};

	private final List<PriceOffer> offers = new ArrayList<PriceOffer>();

	/**
	 * For use in extending classes.
	 */
	protected NormalPlayerShopkeeper() {
	}

	public NormalPlayerShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.initOnLoad(config);
		this.onInitDone();
	}

	public NormalPlayerShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		this.initOnCreation(creationData);
		this.onInitDone();
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new NormalPlayerShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new NormalPlayerShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) throws ShopkeeperCreateException {
		super.load(config);
		// load offers:
		offers.clear();
		// legacy: load offers from old costs section
		offers.addAll(PriceOffer.loadFromConfigOld(config, "costs"));
		offers.addAll(PriceOffer.loadFromConfig(config, "offers"));
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		// save offers:
		PriceOffer.saveToConfig(config, "offers", offers);
	}

	@Override
	public ShopType<?> getType() {
		return DefaultShopTypes.PLAYER_NORMAL();
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		List<ItemCount> chestItems = this.getItemsFromChest();
		for (PriceOffer offer : offers) {
			ItemStack tradedItem = offer.getItem();
			ItemCount itemCount = ItemCount.findSimilar(chestItems, tradedItem);
			if (itemCount != null) {
				int chestAmt = itemCount.getAmount();
				if (chestAmt >= offer.getItem().getAmount()) {
					ItemStack[] merchantRecipe = new ItemStack[3];
					this.setRecipeCost(merchantRecipe, offer.getPrice());
					merchantRecipe[2] = tradedItem;
					recipes.add(merchantRecipe);
				}
			}
		}
		return recipes;
	}

	public PriceOffer getOffer(ItemStack item) {
		for (PriceOffer offer : offers) {
			if (Utils.isSimilar(offer.getItem(), item)) {
				return offer;
			}
		}
		return null;
	}

	public PriceOffer addOffer(ItemStack tradedItem, int price) {
		// create offer (also handles validation):
		PriceOffer newOffer = new PriceOffer(tradedItem, price);

		// remove multiple offers for the same item:
		this.removeOffer(tradedItem);

		// add new offer:
		offers.add(newOffer);
		return newOffer;
	}

	public void clearOffers() {
		offers.clear();
	}

	public void removeOffer(ItemStack tradedItem) {
		Iterator<PriceOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			if (Utils.isSimilar(iterator.next().getItem(), tradedItem)) {
				iterator.remove();
				return;
			}
		}
	}

	private List<ItemCount> getItemsFromChest() {
		return this.getItemsFromChest(ITEM_FILTER);
	}
}
