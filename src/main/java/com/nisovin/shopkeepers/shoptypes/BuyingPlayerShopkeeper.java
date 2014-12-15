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

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;

public class BuyingPlayerShopkeeper extends PlayerShopkeeper {

	protected static class BuyingPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected BuyingPlayerShopEditorHandler(UIType uiType, BuyingPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected boolean openWindow(Player player) {
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			List<ItemStack> types = ((BuyingPlayerShopkeeper) shopkeeper).getTypesFromChest();
			for (int i = 0; i < types.size() && i < 8; i++) {
				ItemStack type = types.get(i);
				Cost cost = ((BuyingPlayerShopkeeper) shopkeeper).costs.get(type);

				if (cost != null) {
					if (cost.cost == 0) {
						inventory.setItem(i, createZeroCurrencyItem());
					} else {
						inventory.setItem(i, createCurrencyItem(cost.cost));
					}
					int amount = cost.amount;
					if (amount <= 0) amount = 1;
					type.setAmount(amount);
					inventory.setItem(i + 18, type);
				} else {
					inventory.setItem(i, createZeroCurrencyItem());
					inventory.setItem(i + 18, type);
				}
			}

			this.setActionButtons(inventory);
			player.openInventory(inventory);
			return true;
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			event.setCancelled(true);
			if (event.getRawSlot() >= 0 && event.getRawSlot() <= 7) {
				// modifying cost
				ItemStack item = event.getCurrentItem();
				if (item != null) {
					if (item.getType() == Settings.currencyItem) {
						int amount = item.getAmount();
						amount = this.getNewAmountAfterEditorClick(event, amount);
						if (amount > 64) amount = 64;
						if (amount <= 0) {
							setZeroCurrencyItem(item);
							item.setAmount(1);
						} else {
							item.setAmount(amount);
						}
					} else if (item.getType() == Settings.zeroCurrencyItem) {
						setCurrencyItem(item);
						item.setAmount(1);
					}
				}

			} else if (event.getRawSlot() >= 18 && event.getRawSlot() <= 25) {
				// modifying quantity
				ItemStack item = event.getCurrentItem();
				if (item != null && item.getType() != Material.AIR) {
					int amount = item.getAmount();
					amount = this.getNewAmountAfterEditorClick(event, amount);
					if (amount <= 0) amount = 1;
					if (amount > item.getMaxStackSize()) amount = item.getMaxStackSize();
					item.setAmount(amount);
				}
			} else if (event.getRawSlot() >= 9 && event.getRawSlot() <= 16) {
			} else {
				super.onInventoryClick(event, player);
			}
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			for (int i = 0; i < 8; i++) {
				ItemStack item = inventory.getItem(i + 18);
				if (item != null) {
					ItemStack costItem = inventory.getItem(i);
					ItemStack saleItem = item.clone();
					saleItem.setAmount(1);
					if (costItem != null && costItem.getType() == Settings.currencyItem && costItem.getAmount() > 0) {
						((BuyingPlayerShopkeeper) shopkeeper).costs.put(saleItem, new Cost(item.getAmount(), costItem.getAmount()));
					} else {
						((BuyingPlayerShopkeeper) shopkeeper).costs.remove(saleItem);
					}
				}
			}
		}
	}

	protected class BuyingPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected BuyingPlayerShopTradingHandler(UIType uiType, BuyingPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player) {
			super.onPurchaseClick(event, player);
			if (event.isCancelled()) return;

			// get type and cost
			ItemStack item = event.getInventory().getItem(0);
			ItemStack type = item.clone();
			type.setAmount(1);

			Cost cost = ((BuyingPlayerShopkeeper) shopkeeper).costs.get(type);
			if (cost == null) {
				event.setCancelled(true);
				return;
			}

			if (cost.amount > item.getAmount()) {
				event.setCancelled(true);
				return;
			}

			// get chest
			Block chest = ((BuyingPlayerShopkeeper) shopkeeper).getChest();
			if (!Utils.isChest(chest.getType())) {
				event.setCancelled(true);
				return;
			}

			// remove currency from chest
			Inventory inventory = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inventory.getContents();
			boolean removed = this.removeCurrencyFromChest(cost.cost, contents);
			if (!removed) {
				event.setCancelled(true);
				return;
			}

			// add items to chest
			int amount = this.getAmountAfterTaxes(cost.amount);
			if (amount > 0) {
				type.setAmount(amount);
				boolean added = this.addToInventory(type, contents);
				if (!added) {
					event.setCancelled(true);
					return;
				}
			}

			// save chest contents
			inventory.setContents(contents);
		}

		protected boolean removeCurrencyFromChest(int amount, ItemStack[] contents) {
			int remaining = amount;

			// first pass - remove currency
			int emptySlot = -1;
			for (int i = 0; i < contents.length; i++) {
				ItemStack item = contents[i];
				if (item != null) {
					if (isHighCurrencyItem(item) && remaining >= Settings.highCurrencyValue) {
						int needed = remaining / Settings.highCurrencyValue;
						int amt = item.getAmount();
						if (amt > needed) {
							item.setAmount(amt - needed);
							remaining = remaining - (needed * Settings.highCurrencyValue);
						} else {
							contents[i] = null;
							remaining = remaining - (amt * Settings.highCurrencyValue);
						}
					} else if (isCurrencyItem(item)) {
						int amt = item.getAmount();
						if (amt > remaining) {
							item.setAmount(amt - remaining);
							return true;
						} else if (amt == remaining) {
							contents[i] = null;
							return true;
						} else {
							contents[i] = null;
							remaining -= amt;
						}
					}
				} else if (emptySlot < 0) {
					emptySlot = i;
				}
				if (remaining <= 0) {
					return true;
				}
			}

			// second pass - try to make change
			if (remaining > 0 && remaining <= Settings.highCurrencyValue && Settings.highCurrencyItem != Material.AIR && emptySlot >= 0) {
				for (int i = 0; i < contents.length; i++) {
					ItemStack item = contents[i];
					if (isHighCurrencyItem(item)) {
						if (item.getAmount() == 1) {
							contents[i] = null;
						} else {
							item.setAmount(item.getAmount() - 1);
						}
						int stackSize = Settings.highCurrencyValue - remaining;
						if (stackSize > 0) {
							contents[emptySlot] = createCurrencyItem(stackSize);
						}
						return true;
					}
				}
			}

			return false;
		}
	}

	// TODO how to handle equal items with different costs? on purchase: take the currentSelectedPage/recipe into account?
	private Map<ItemStack, Cost> costs;

	public BuyingPlayerShopkeeper(ConfigurationSection config) {
		super(config);
		this.onConstruction();
	}

	public BuyingPlayerShopkeeper(ShopCreationData creationData) {
		super(creationData);
		this.costs = new HashMap<ItemStack, Cost>();
		this.onConstruction();
	}

	private final void onConstruction() {
		this.registerUIHandler(new BuyingPlayerShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new BuyingPlayerShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		costs = new HashMap<ItemStack, Cost>();
		ConfigurationSection costsSection = config.getConfigurationSection("costs");
		if (costsSection != null) {
			for (String key : costsSection.getKeys(false)) {
				ConfigurationSection itemSection = costsSection.getConfigurationSection(key);
				ItemStack item = itemSection.getItemStack("item");
				if (itemSection.contains("attributes")) {
					String attr = itemSection.getString("attributes");
					if (attr != null && !attr.isEmpty()) {
						item = NMSManager.getProvider().loadItemAttributesFromString(item, attr);
					}
				}
				Cost cost = new Cost();
				cost.amount = itemSection.getInt("amount");
				cost.cost = itemSection.getInt("cost");
				costs.put(item, cost);
			}
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		ConfigurationSection costsSection = config.createSection("costs");
		int count = 0;
		for (ItemStack item : costs.keySet()) {
			Cost cost = costs.get(item);
			ConfigurationSection itemSection = costsSection.createSection(count + "");
			itemSection.set("item", item);
			String attr = NMSManager.getProvider().saveItemAttributesToString(item);
			if (attr != null && !attr.isEmpty()) {
				itemSection.set("attributes", attr);
			}
			itemSection.set("amount", cost.amount);
			itemSection.set("cost", cost.cost);
			count++;
		}
	}

	@Override
	public ShopType<BuyingPlayerShopkeeper> getType() {
		return DefaultShopTypes.PLAYER_BUY;
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		List<ItemStack> chestItems = this.getTypesFromChest();
		int chestTotal = this.getCurrencyInChest();
		for (ItemStack type : costs.keySet()) {
			if (chestItems.contains(type)) {
				Cost cost = costs.get(type);
				if (chestTotal >= cost.cost) {
					ItemStack[] recipe = new ItemStack[3];
					recipe[0] = type.clone();
					recipe[0].setAmount(cost.amount);
					recipe[2] = createCurrencyItem(cost.cost);
					recipes.add(recipe);
				}
			}
		}
		return recipes;
	}

	public Map<ItemStack, Cost> getCosts() {
		return costs;
	}

	private List<ItemStack> getTypesFromChest() {
		List<ItemStack> boughtItems = new ArrayList<ItemStack>();
		Block chest = this.getChest();
		if (Utils.isChest(chest.getType())) {
			Inventory inv = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (item == null || item.getType() == Material.AIR) continue;
				if (isCurrencyItem(item) || isHighCurrencyItem(item)) continue;
				if (item.getType() == Material.WRITTEN_BOOK) continue;
				if (item.getEnchantments().size() != 0) continue;

				ItemStack boughtItem = item.clone();
				boughtItem.setAmount(1);
				if (!boughtItems.contains(boughtItem)) {
					boughtItems.add(boughtItem);
				}

			}
		}
		return boughtItems;
	}

	private int getCurrencyInChest() {
		int total = 0;
		Block chest = this.getChest();
		if (Utils.isChest(chest.getType())) {
			Inventory inv = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (isCurrencyItem(item)) {
					total += item.getAmount();
				} else if (isHighCurrencyItem(item)) {
					total += item.getAmount() * Settings.highCurrencyValue;
				}
			}
		}
		return total;
	}

	protected static class Cost {

		int amount;
		int cost;

		public Cost() {
		}

		public Cost(int amount, int cost) {
			this.amount = amount;
			this.cost = cost;
		}

		public int getAmount() {
			return amount;
		}

		public void setAmount(int amount) {
			this.amount = amount;
		}

		public int getCost() {
			return cost;
		}

		public void setCost(int cost) {
			this.cost = cost;
		}
	}
}