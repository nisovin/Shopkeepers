package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ui.UIManager;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;

public class WrittenBookPlayerShopkeeper extends PlayerShopkeeper {

	protected class WrittenBookPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected WrittenBookPlayerShopEditorHandler(UIManager uiManager, WrittenBookPlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		protected boolean openInterface(Player player) {
			Inventory inv = Bukkit.createInventory(player, 27, Settings.editorTitle);

			List<ItemStack> books = ((WrittenBookPlayerShopkeeper) this.shopkeeper).getBooksFromChest();

			for (int i = 0; i < books.size() && i < 8; i++) {
				String title = getTitleOfBook(books.get(i));
				if (title != null) {
					int cost = 0;
					if (((WrittenBookPlayerShopkeeper) this.shopkeeper).costs.containsKey(title)) {
						cost = ((WrittenBookPlayerShopkeeper) this.shopkeeper).costs.get(title);
					}
					inv.setItem(i, books.get(i));
					this.setEditColumnCost(inv, i, cost);
				}

			}

			// add the special buttons
			this.setActionButtons(inv);

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
			for (int i = 0; i < 8; i++) {
				ItemStack item = inventory.getItem(i);
				if (item != null && item.getType() == Material.WRITTEN_BOOK) {
					String title = getTitleOfBook(item);
					if (title != null) {
						int cost = this.getCostFromColumn(inventory, i);
						if (cost > 0) {
							((WrittenBookPlayerShopkeeper) this.shopkeeper).costs.put(title, cost);
						} else {
							((WrittenBookPlayerShopkeeper) this.shopkeeper).costs.remove(title);
						}
					}
				}
			}
		}
	}

	protected class WrittenBookPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected WrittenBookPlayerShopTradingHandler(UIManager uiManager, WrittenBookPlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player) {
			super.onPurchaseClick(event, player);
			if (event.isCancelled()) return;

			ItemStack book = event.getCurrentItem();
			String title = getTitleOfBook(book);
			if (title == null) {
				event.setCancelled(true);
				return;
			}

			// get chest
			Block chest = ((WrittenBookPlayerShopkeeper) this.shopkeeper).getChest();
			if (chest.getType() != Material.CHEST) {
				event.setCancelled(true);
				return;
			}

			// remove blank book from chest
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

			// get cost
			Integer costInt = ((WrittenBookPlayerShopkeeper) this.shopkeeper).costs.get(title);
			if (costInt == null) {
				event.setCancelled(true);
				return;
			}
			int cost = this.getAmountAfterTaxes(costInt.intValue());

			// add earnings to chest
			if (cost > 0) {
				int highCost = cost / Settings.highCurrencyValue;
				int lowCost = cost % Settings.highCurrencyValue;
				boolean added = false;
				if (highCost > 0) {
					added = this.addToInventory(new ItemStack(Settings.highCurrencyItem, highCost, Settings.highCurrencyItemData), contents);
					if (!added) {
						event.setCancelled(true);
						return;
					}
				}
				if (lowCost > 0) {
					added = this.addToInventory(new ItemStack(Settings.currencyItem, lowCost, Settings.currencyItemData), contents);
					if (!added) {
						event.setCancelled(true);
						return;
					}
				}
			}

			// set chest contents
			inv.setContents(contents);
		}
	}

	private Map<String, Integer> costs;

	public WrittenBookPlayerShopkeeper(ConfigurationSection config) {
		super(config);
		this.onConstruction();
	}

	public WrittenBookPlayerShopkeeper(Player owner, Block chest, Location location, ShopObjectType objectType) {
		super(owner, chest, location, objectType);
		costs = new HashMap<String, Integer>();
		this.onConstruction();
	}

	private final void onConstruction() {
		this.registerUIHandler(new WrittenBookPlayerShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new WrittenBookPlayerShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		this.costs = new HashMap<String, Integer>();
		ConfigurationSection costsSection = config.getConfigurationSection("costs");
		if (costsSection != null) {
			for (String key : costsSection.getKeys(false)) {
				this.costs.put(key, costsSection.getInt(key));
			}
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		ConfigurationSection costsSection = config.createSection("costs");
		for (String title : this.costs.keySet()) {
			costsSection.set(title, this.costs.get(title));
		}
	}

	@Override
	public ShopType getType() {
		return DefaultShopTypes.PLAYER_BOOK;
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		if (chestHasBlankBooks()) {
			List<ItemStack> books = getBooksFromChest();
			for (ItemStack book : books) {
				if (book != null) {
					String title = getTitleOfBook(book);
					if (title != null && this.costs.containsKey(title)) {
						int cost = this.costs.get(title);
						ItemStack[] recipe = new ItemStack[3];
						setRecipeCost(recipe, cost);
						recipe[2] = book.clone();
						recipes.add(recipe);
					}
				}
			}
		}
		return recipes;
	}

	public Map<String, Integer> getCosts() {
		return this.costs;
	}

	private List<ItemStack> getBooksFromChest() {
		List<ItemStack> list = new ArrayList<ItemStack>();
		Block chest = this.getChest();
		if (chest.getType() == Material.CHEST) {
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
		if (chest.getType() == Material.CHEST) {
			Inventory inv = ((Chest) chest.getState()).getInventory();
			return inv.contains(Material.BOOK_AND_QUILL);
		}
		return false;
	}

	private boolean isBookAuthoredByShopOwner(ItemStack book) {
		assert book.getType() == Material.WRITTEN_BOOK;
		// checking for ownerName might break if the player changes his name but the book metadata doesn't get updated. Also: why do we even filter for only books of the shop owner?
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
		assert book.getType() == Material.WRITTEN_BOOK;
		if (book.hasItemMeta()) {
			BookMeta meta = (BookMeta) book.getItemMeta();
			return meta.getTitle();
		}
		return null;
	}
}