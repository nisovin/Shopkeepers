package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;

/**
 * Sells written books.
 */
public class BookPlayerShopkeeper extends PlayerShopkeeper {

	protected static class WrittenBookPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected WrittenBookPlayerShopEditorHandler(UIType uiType, BookPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected boolean openWindow(Player player) {
			Inventory inv = Bukkit.createInventory(player, 27, Settings.editorTitle);
			List<ItemStack> books = ((BookPlayerShopkeeper) shopkeeper).getBooksFromChest();
			for (int column = 0; column < books.size() && column < 8; column++) {
				String title = getTitleOfBook(books.get(column));
				if (title != null) {
					int price = 0;
					if (((BookPlayerShopkeeper) shopkeeper).offers.containsKey(title)) {
						price = ((BookPlayerShopkeeper) shopkeeper).offers.get(title);
					}
					inv.setItem(column, books.get(column));
					this.setEditColumnCost(inv, column, price);
				}
			}

			// add the special buttons:
			this.setActionButtons(inv);
			// show editing inventory:
			player.openInventory(inv);
			return true;
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			event.setCancelled(true);
			super.onInventoryClick(event, player);
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			for (int column = 0; column < 8; column++) {
				ItemStack item = inventory.getItem(column);
				if (!Utils.isEmpty(item) && item.getType() == Material.WRITTEN_BOOK) {
					String title = getTitleOfBook(item);
					if (title != null) {
						int price = this.getPriceFromColumn(inventory, column);
						if (price > 0) {
							((BookPlayerShopkeeper) shopkeeper).offers.put(title, price);
						} else {
							((BookPlayerShopkeeper) shopkeeper).offers.remove(title);
						}
					}
				}
			}
		}
	}

	protected static class WrittenBookPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected WrittenBookPlayerShopTradingHandler(UIType uiType, BookPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player, ItemStack[] usedRecipe, ItemStack offered1, ItemStack offered2) {
			super.onPurchaseClick(event, player, usedRecipe, offered1, offered2);
			if (event.isCancelled()) return;

			ItemStack book = usedRecipe[2];
			String title = getTitleOfBook(book);
			if (title == null) {
				// this should not happen.. because the recipes were created based on the shopkeeper's offers
				event.setCancelled(true);
				return;
			}

			// get chest:
			Block chest = ((BookPlayerShopkeeper) shopkeeper).getChest();
			if (!Utils.isChest(chest.getType())) {
				event.setCancelled(true);
				return;
			}

			// remove blank book from chest:
			boolean removed = false;
			Inventory inv = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (int i = 0; i < contents.length; i++) {
				if (contents[i] != null && contents[i].getType() == Material.BOOK_AND_QUILL) {
					if (contents[i].getAmount() == 1) {
						contents[i] = null;
					} else {
						contents[i].setAmount(contents[i].getAmount() - 1);
					}
					removed = true;
					break;
				}
			}
			if (!removed) {
				event.setCancelled(true);
				return;
			}

			// get price:
			Integer priceInt = ((BookPlayerShopkeeper) shopkeeper).offers.get(title);
			if (priceInt == null) {
				event.setCancelled(true);
				return;
			}
			int price = this.getAmountAfterTaxes(priceInt.intValue());

			// add earnings to chest:
			if (price > 0) {
				int highCost = price / Settings.highCurrencyValue;
				int lowCost = price % Settings.highCurrencyValue;
				if (highCost > 0) {
					if (Utils.addItems(contents, Settings.createHighCurrencyItem(highCost)) != 0) {
						event.setCancelled(true);
						return;
					}
				}
				if (lowCost > 0) {
					if (Utils.addItems(contents, Settings.createCurrencyItem(lowCost)) != 0) {
						event.setCancelled(true);
						return;
					}
				}
			}

			// set chest contents:
			inv.setContents(contents);
		}
	}

	private final Map<String, Integer> offers = new HashMap<String, Integer>();

	/**
	 * For use in extending classes.
	 */
	protected BookPlayerShopkeeper() {
	}

	public BookPlayerShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.initOnLoad(config);
		this.onInitDone();
	}

	public BookPlayerShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException {
		this.initOnCreation(creationData);
		this.onInitDone();
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new WrittenBookPlayerShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new WrittenBookPlayerShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) throws ShopkeeperCreateException {
		super.load(config);
		offers.clear();
		ConfigurationSection costsSection = config.getConfigurationSection("costs");
		if (costsSection != null) {
			for (String key : costsSection.getKeys(false)) {
				offers.put(key, costsSection.getInt(key));
			}
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		ConfigurationSection costsSection = config.createSection("costs");
		for (String title : offers.keySet()) {
			costsSection.set(title, offers.get(title));
		}
	}

	@Override
	public ShopType<?> getType() {
		return DefaultShopTypes.PLAYER_BOOK();
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		if (this.chestHasBlankBooks()) {
			List<ItemStack> books = this.getBooksFromChest();
			for (ItemStack book : books) {
				if (book != null) {
					String title = getTitleOfBook(book);
					if (title != null && offers.containsKey(title)) {
						int price = offers.get(title);
						ItemStack[] recipe = new ItemStack[3];
						this.setRecipeCost(recipe, price);
						recipe[2] = book.clone();
						recipes.add(recipe);
					}
				}
			}
		}
		return recipes;
	}

	private List<ItemStack> getBooksFromChest() {
		List<ItemStack> list = new ArrayList<ItemStack>();
		Block chest = this.getChest();
		if (Utils.isChest(chest.getType())) {
			Inventory inv = ((Chest) chest.getState()).getInventory();
			for (ItemStack item : inv.getContents()) {
				if (item != null && item.getType() == Material.WRITTEN_BOOK && this.isBookAuthoredByShopOwner(item)) {
					list.add(item);
				}
			}
		}
		return list;
	}

	private boolean chestHasBlankBooks() {
		Block chest = this.getChest();
		if (Utils.isChest(chest.getType())) {
			Inventory inv = ((Chest) chest.getState()).getInventory();
			return inv.contains(Material.BOOK_AND_QUILL);
		}
		return false;
	}

	private boolean isBookAuthoredByShopOwner(ItemStack book) {
		assert book.getType() == Material.WRITTEN_BOOK;
		// checking for ownerName might break if the player changes his name but the book metadata doesn't get updated.
		// Also: why do we even filter for only books of the shop owner?
		/*
		 * if (book.hasItemMeta()) {
		 * BookMeta meta = (BookMeta) book.getItemMeta();
		 * if (meta.hasAuthor() && meta.getAuthor().equalsIgnoreCase(ownerName)) {
		 * return true;
		 * }
		 * }
		 * return false;
		 */
		return book.getType() == Material.WRITTEN_BOOK;
	}

	private static String getTitleOfBook(ItemStack book) {
		if (book.getType() == Material.WRITTEN_BOOK && book.hasItemMeta()) {
			BookMeta meta = (BookMeta) book.getItemMeta();
			return meta.getTitle();
		}
		return null;
	}
}
