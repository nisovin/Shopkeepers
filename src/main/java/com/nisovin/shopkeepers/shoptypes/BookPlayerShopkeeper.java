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
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.shoptypes.offers.BookOffer;
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
		public BookPlayerShopkeeper getShopkeeper() {
			return (BookPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			final BookPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inv = Bukkit.createInventory(player, 27, Settings.editorTitle);
			List<ItemStack> books = shopkeeper.getBooksFromChest();
			for (int column = 0; column < books.size() && column < 8; column++) {
				String bookTitle = getTitleOfBook(books.get(column));
				if (bookTitle != null) {
					int price = 0;
					BookOffer offer = shopkeeper.getOffer(bookTitle);
					if (offer != null) {
						price = offer.getPrice();
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
			final BookPlayerShopkeeper shopkeeper = this.getShopkeeper();
			for (int column = 0; column < 8; column++) {
				ItemStack item = inventory.getItem(column);
				if (!Utils.isEmpty(item) && item.getType() == Material.WRITTEN_BOOK) {
					String bookTitle = getTitleOfBook(item);
					if (bookTitle != null) {
						int price = this.getPriceFromColumn(inventory, column);
						if (price > 0) {
							shopkeeper.addOffer(bookTitle, price);
						} else {
							shopkeeper.removeOffer(bookTitle);
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
		public BookPlayerShopkeeper getShopkeeper() {
			return (BookPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player, ItemStack[] usedRecipe, ItemStack offered1, ItemStack offered2) {
			super.onPurchaseClick(event, player, usedRecipe, offered1, offered2);
			if (event.isCancelled()) return;
			final BookPlayerShopkeeper shopkeeper = this.getShopkeeper();

			ItemStack book = usedRecipe[2];
			String bookTitle = getTitleOfBook(book);
			if (bookTitle == null) {
				// this should not happen.. because the recipes were created based on the shopkeeper's offers
				event.setCancelled(true);
				return;
			}

			// get chest:
			Block chest = shopkeeper.getChest();
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
			BookOffer offer = shopkeeper.getOffer(bookTitle);
			if (offer == null) {
				event.setCancelled(true);
				return;
			}
			int price = this.getAmountAfterTaxes(offer.getPrice());

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

	// contains only one offer for a specific book (book title):
	private final List<BookOffer> offers = new ArrayList<BookOffer>();
	private final List<BookOffer> offersView = Collections.unmodifiableList(offers);

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
		// load offers:
		this.clearOffers();
		// TODO remove legacy: load offers from old costs section (since late MC 1.12.2)
		this.addOffers(BookOffer.loadFromConfig(config, "costs"));
		this.addOffers(BookOffer.loadFromConfig(config, "offers"));
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		// save offers: // TODO previous saved to 'costs'
		BookOffer.saveToConfig(config, "offers", this.getOffers());
	}

	@Override
	public ShopType<?> getType() {
		return DefaultShopTypes.PLAYER_BOOK();
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		if (this.hasChestBlankBooks()) {
			List<ItemStack> books = this.getBooksFromChest();
			for (ItemStack book : books) {
				assert !Utils.isEmpty(book);
				String bookTitle = getTitleOfBook(book); // can be null
				BookOffer offer = this.getOffer(bookTitle);
				if (offer != null) {
					int price = offer.getPrice();
					ItemStack[] recipe = new ItemStack[3];
					this.setRecipeCost(recipe, price);
					recipe[2] = book.clone();
					recipes.add(recipe);
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
				if (!Utils.isEmpty(item) && item.getType() == Material.WRITTEN_BOOK && this.isBookAuthoredByShopOwner(item)) {
					list.add(item);
				}
			}
		}
		return list;
	}

	private boolean hasChestBlankBooks() {
		Block chest = this.getChest();
		if (Utils.isChest(chest.getType())) {
			Inventory chestInventory = ((Chest) chest.getState()).getInventory();
			return chestInventory.contains(Material.BOOK_AND_QUILL);
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

	// OFFERS:

	public List<BookOffer> getOffers() {
		return offersView;
	}

	public BookOffer getOffer(String bookTitle) {
		for (BookOffer offer : this.getOffers()) {
			if (offer.getBookTitle().equals(bookTitle)) {
				return offer;
			}
		}
		return null;
	}

	public BookOffer addOffer(String bookTitle, int price) {
		// create offer (also handles validation):
		BookOffer newOffer = new BookOffer(bookTitle, price);

		// add new offer (replacing any previous offer for the same book):
		this.addOffer(newOffer);
		return newOffer;
	}

	private void addOffer(BookOffer offer) {
		assert offer != null;
		// remove previous offer for the same book:
		this.removeOffer(offer.getBookTitle());
		offers.add(offer);
	}

	private void addOffers(Collection<BookOffer> offers) {
		assert offers != null;
		for (BookOffer offer : offers) {
			if (offer == null) continue; // skip invalid entries
			// add new offer (replacing any previous offer for the same book):
			this.addOffer(offer);
		}
	}

	public void clearOffers() {
		offers.clear();
	}

	public void removeOffer(String bookTitle) {
		Iterator<BookOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getBookTitle().equals(bookTitle)) {
				iterator.remove();
				break;
			}
		}
	}
}
