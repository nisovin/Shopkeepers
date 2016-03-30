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

import com.nisovin.shopkeepers.Filter;
import com.nisovin.shopkeepers.ItemCount;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
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
		protected boolean openWindow(Player player) {
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add offers:
			List<ItemCount> chestItems = ((NormalPlayerShopkeeper) shopkeeper).getItemsFromChest();
			int column = 0;
			for (ItemCount itemCount : chestItems) {
				ItemStack item = itemCount.getItem(); // this item is already a copy with amount 1
				int price = 0;
				PriceOffer offer = ((NormalPlayerShopkeeper) shopkeeper).getOffer(item);
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
				ItemStack item = event.getCurrentItem();
				if (item != null && item.getType() != Material.AIR) {
					int amount = item.getAmount();
					amount = this.getNewAmountAfterEditorClick(event, amount);
					if (amount <= 0) amount = 1;
					if (amount > item.getMaxStackSize()) amount = item.getMaxStackSize();
					item.setAmount(amount);
				}
			} else {
				super.onInventoryClick(event, player);
			}
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			for (int column = 0; column < 8; column++) {
				ItemStack tradedItem = inventory.getItem(column);
				if (tradedItem != null && tradedItem.getType() != Material.AIR) {
					int price = this.getPriceFromColumn(inventory, column);
					if (price > 0) {
						((NormalPlayerShopkeeper) shopkeeper).addOffer(tradedItem, price);
					} else {
						((NormalPlayerShopkeeper) shopkeeper).removeOffer(tradedItem);
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
		protected void onPurchaseClick(InventoryClickEvent event, Player player, ItemStack[] usedRecipe, ItemStack offered1, ItemStack offered2) {
			super.onPurchaseClick(event, player, usedRecipe, offered1, offered2);
			if (event.isCancelled()) return;

			// get offer for this type of item:
			ItemStack resultItem = usedRecipe[2];
			PriceOffer offer = ((NormalPlayerShopkeeper) shopkeeper).getOffer(resultItem);
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
			Block chest = ((NormalPlayerShopkeeper) shopkeeper).getChest();
			if (!Utils.isChest(chest.getType())) {
				event.setCancelled(true);
				return;
			}

			// remove item from chest:
			Inventory inventory = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inventory.getContents();
			if (Utils.removeItems(contents, resultItem) != 0) {
				event.setCancelled(true);
				return;
			}

			// add earnings to chest:
			// TODO maybe add the actual items the trading player gave, instead of creating new currency items?
			// TODO the currency items used in the trade can slightly differ, depending on item comparison
			int amount = this.getAmountAfterTaxes(offer.getPrice());
			if (amount > 0) {
				if (Settings.highCurrencyItem == Material.AIR || offer.getPrice() <= Settings.highCurrencyMinCost) {
					if (Utils.addItems(contents, createCurrencyItem(amount)) != 0) {
						event.setCancelled(true);
						return;
					}
				} else {
					int highCost = amount / Settings.highCurrencyValue;
					int lowCost = amount % Settings.highCurrencyValue;
					if (highCost > 0) {
						if (Utils.addItems(contents, createHighCurrencyItem(highCost)) != 0) {
							event.setCancelled(true);
							return;
						}
					}
					if (lowCost > 0) {
						if (Utils.addItems(contents, createCurrencyItem(lowCost)) != 0) {
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
			if (isCurrencyItem(item) || isHighCurrencyItem(item)) return false;
			return true;
		}
	};

	private final List<PriceOffer> offers = new ArrayList<PriceOffer>();

	/**
	 * For use in extending classes.
	 */
	protected NormalPlayerShopkeeper() {
	}

	public NormalPlayerShopkeeper(ConfigurationSection config) {
		this.initOnLoad(config);
		this.onInitDone();
	}

	public NormalPlayerShopkeeper(ShopCreationData creationData) {
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
	protected void load(ConfigurationSection config) {
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
					merchantRecipe[2] = tradedItem.clone();
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
		assert tradedItem != null;
		// remove multiple offers for the same item:
		this.removeOffer(tradedItem);

		// making a copy of the item stack, just in case it is used elsewhere as well:
		PriceOffer newOffer = new PriceOffer(tradedItem.clone(), price);
		offers.add(newOffer);
		return newOffer;
	}

	public void clearOffers() {
		offers.clear();
	}

	public void removeOffer(ItemStack item) {
		Iterator<PriceOffer> iter = offers.iterator();
		while (iter.hasNext()) {
			if (Utils.isSimilar(iter.next().getItem(), item)) {
				iter.remove();
				return;
			}
		}
	}

	private List<ItemCount> getItemsFromChest() {
		return this.getItemsFromChest(ITEM_FILTER);
	}
}