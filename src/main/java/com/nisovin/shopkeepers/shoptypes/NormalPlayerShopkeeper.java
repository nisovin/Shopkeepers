package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopObjectType;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.ui.UIManager;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;

public class NormalPlayerShopkeeper extends PlayerShopkeeper {

	protected class NormalPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected NormalPlayerShopEditorHandler(UIManager uiManager, NormalPlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		protected boolean openWindow(Player player) {
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add the sale types
			Map<ItemStack, Integer> typesFromChest = getItemsFromChest();
			int i = 0;
			for (ItemStack item : typesFromChest.keySet()) {
				Cost cost = ((NormalPlayerShopkeeper) this.shopkeeper).costs.get(item);
				if (cost != null) {
					ItemStack saleItem = item.clone();
					saleItem.setAmount(cost.amount);
					inventory.setItem(i, saleItem);
					this.setEditColumnCost(inventory, i, cost.cost);
				} else {
					inventory.setItem(i, item);
					this.setEditColumnCost(inventory, i, 0);
				}
				i++;
				if (i > 8) break;
			}

			// add the special buttons
			this.setActionButtons(inventory);

			player.openInventory(inventory);

			return true;
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			event.setCancelled(true);
			if (event.getRawSlot() >= 0 && event.getRawSlot() <= 7) {
				// handle changing sell stack size
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
			for (int i = 0; i < 8; i++) {
				ItemStack item = inventory.getItem(i);
				if (item != null && item.getType() != Material.AIR) {
					int cost = this.getCostFromColumn(inventory, i);
					if (cost > 0) {
						ItemStack saleItem = item.clone();
						saleItem.setAmount(1);
						((NormalPlayerShopkeeper) this.shopkeeper).costs.put(saleItem, new Cost(item.getAmount(), cost));
					} else {
						ItemStack saleItem = item.clone();
						saleItem.setAmount(1);
						((NormalPlayerShopkeeper) this.shopkeeper).costs.remove(saleItem);
					}
				}
			}
		}
	}

	protected class NormalPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected NormalPlayerShopTradingHandler(UIManager uiManager, NormalPlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		protected void onPurchaseClick(InventoryClickEvent event, Player player) {
			super.onPurchaseClick(event, player);
			if (event.isCancelled()) return;

			// get type and cost
			ItemStack item = event.getCurrentItem();
			ItemStack type = item.clone();
			type.setAmount(1);

			Cost cost = ((NormalPlayerShopkeeper) this.shopkeeper).costs.get(type);
			if (cost == null) {
				event.setCancelled(true);
				return;
			}

			if (cost.amount != item.getAmount()) {
				event.setCancelled(true);
				return;
			}

			// get chest
			Block chest = ((NormalPlayerShopkeeper) this.shopkeeper).getChest();
			if (chest.getType() != Material.CHEST) {
				event.setCancelled(true);
				return;
			}

			// remove item from chest
			Inventory inventory = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inventory.getContents();
			boolean removed = this.removeFromInventory(item, contents);
			if (!removed) {
				event.setCancelled(true);
				return;
			}

			// add earnings to chest
			int amount = this.getAmountAfterTaxes(cost.cost);
			if (amount > 0) {
				if (Settings.highCurrencyItem == Material.AIR || cost.cost <= Settings.highCurrencyMinCost) {
					boolean added = this.addToInventory(new ItemStack(Settings.currencyItem, amount, Settings.currencyItemData), contents);
					if (!added) {
						event.setCancelled(true);
						return;
					}
				} else {
					int highCost = amount / Settings.highCurrencyValue;
					int lowCost = amount % Settings.highCurrencyValue;
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
			}

			// save chest contents
			inventory.setContents(contents);
		}
	}

	// private Map<ItemType, Cost> costs;
	private Map<ItemStack, Cost> costs;

	public NormalPlayerShopkeeper(ConfigurationSection config) {
		super(config);
		this.onConstruction();
	}

	public NormalPlayerShopkeeper(Player owner, Block chest, Location location, ShopObjectType objectType) {
		super(owner, chest, location, objectType);
		costs = new HashMap<ItemStack, Cost>();
		this.onConstruction();
	}

	private final void onConstruction() {
		this.registerUIHandler(new NormalPlayerShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new NormalPlayerShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) {
		super.load(config);
		this.costs = new HashMap<ItemStack, Cost>();
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
				Cost cost = new Cost(itemSection.getInt("amount"), itemSection.getInt("cost"));
				this.costs.put(item, cost);
			}
		}
	}

	@Override
	protected void save(ConfigurationSection config) {
		super.save(config);
		ConfigurationSection costsSection = config.createSection("costs");
		int count = 0;
		for (ItemStack item : this.costs.keySet()) {
			Cost cost = this.costs.get(item);
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
	public ShopType getType() {
		return DefaultShopTypes.PLAYER_NORMAL;
	}

	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		Map<ItemStack, Integer> chestItems = getItemsFromChest();
		for (ItemStack item : this.costs.keySet()) {
			if (chestItems.containsKey(item)) {
				Cost cost = this.costs.get(item);
				int chestAmt = chestItems.get(item);
				if (chestAmt >= cost.amount) {
					ItemStack[] recipe = new ItemStack[3];
					setRecipeCost(recipe, cost.cost);
					recipe[2] = item.clone();
					recipe[2].setAmount(cost.amount);
					recipes.add(recipe);
				}
			}
		}
		return recipes;
	}

	public Map<ItemStack, Cost> getCosts() {
		return this.costs;
	}

	private Map<ItemStack, Integer> getItemsFromChest() {
		Map<ItemStack, Integer> map = new LinkedHashMap<ItemStack, Integer>();
		Block chest = this.getChest();
		if (chest.getType() == Material.CHEST) {
			Inventory inv = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (item != null && item.getType() != Material.AIR && item.getType() != Settings.currencyItem && item.getType() != Settings.highCurrencyItem) {
					ItemStack i = item.clone();
					i.setAmount(1);
					if (map.containsKey(i)) {
						map.put(i, map.get(i) + item.getAmount());
					} else {
						map.put(i, item.getAmount());
					}
				}
			}
		}
		return map;
	}

	protected class Cost {
		int amount;
		int cost;

		public Cost(int amount, int cost) {
			this.amount = amount;
			this.cost = cost;
		}

		public int getAmount() {
			return this.amount;
		}

		public void setAmount(int amount) {
			this.amount = amount;
		}

		public int getCost() {
			return this.cost;
		}

		public void setCost(int cost) {
			this.cost = cost;
		}
	}
}