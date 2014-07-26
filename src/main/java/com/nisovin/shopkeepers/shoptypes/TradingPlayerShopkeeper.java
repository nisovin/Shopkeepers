package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.ui.UIManager;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;

public class TradingPlayerShopkeeper extends PlayerShopkeeper {

	protected class TradingPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected TradingPlayerShopEditorHandler(UIManager uiManager, TradingPlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		protected boolean openWindow(Player player) {
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add the sale types
			Map<ItemStack, Integer> typesFromChest = getItemsFromChest();
			int i = 0;
			for (ItemStack item : typesFromChest.keySet()) {
				Cost cost = ((TradingPlayerShopkeeper) shopkeeper).costs.get(item);
				if (cost != null) {
					item.setAmount(cost.amount);
					inventory.setItem(i, item);
					if (cost.item1 != null && cost.item1.getType() != Material.AIR && cost.item1.getAmount() > 0) {
						inventory.setItem(i + 9, cost.item1);
					}
					if (cost.item2 != null && cost.item2.getType() != Material.AIR && cost.item2.getAmount() > 0) {
						inventory.setItem(i + 18, cost.item2);
					}
				} else {
					inventory.setItem(i, item);
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
			final int slot = event.getRawSlot();
			if (slot >= 0 && slot <= 7) {
				// handle changing sell stack size
				ItemStack item = event.getCurrentItem();
				if (item != null && item.getType() != Material.AIR) {
					int amount = item.getAmount();
					amount = this.getNewAmountAfterEditorClick(event, amount);
					if (amount <= 0) amount = 1;
					if (amount > item.getMaxStackSize()) amount = item.getMaxStackSize();
					item.setAmount(amount);
				}
			} else if ((slot >= 9 && slot <= 16) || (slot >= 18 && slot <= 25)) {
				if (((TradingPlayerShopkeeper) this.shopkeeper).clickedItem != null) {
					// placing item
					final Inventory inventory = event.getInventory();
					Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {
						public void run() {
							inventory.setItem(slot, ((TradingPlayerShopkeeper) shopkeeper).clickedItem);
							((TradingPlayerShopkeeper) shopkeeper).clickedItem = null;
						}
					}, 1);
				} else {
					// changing stack size
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
			} else if (slot > 27) {
				// clicking in player inventory
				if (event.isShiftClick() || !event.isLeftClick()) {
					return;
				}
				ItemStack cursor = event.getCursor();
				if (cursor != null && cursor.getType() != Material.AIR) {
					return;
				}
				ItemStack current = event.getCurrentItem();
				if (current != null && current.getType() != Material.AIR) {
					((TradingPlayerShopkeeper) this.shopkeeper).clickedItem = current.clone();
					((TradingPlayerShopkeeper) this.shopkeeper).clickedItem.setAmount(1);
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
					ItemStack cost1 = null, cost2 = null;
					ItemStack item1 = inventory.getItem(i + 9);
					ItemStack item2 = inventory.getItem(i + 18);
					if (item1 != null && item1.getType() != Material.AIR) {
						cost1 = item1;
						if (item2 != null && item2.getType() != Material.AIR) {
							cost2 = item2;
						}
					} else if (item2 != null && item2.getType() != Material.AIR) {
						cost1 = item2;
					}
					if (cost1 != null) {
						Cost cost = new Cost();
						cost.amount = item.getAmount();
						cost.item1 = cost1;
						cost.item2 = cost2;
						ItemStack saleItem = item.clone();
						saleItem.setAmount(1);
						((TradingPlayerShopkeeper) this.shopkeeper).costs.put(saleItem, cost);
					} else {
						ItemStack saleItem = item.clone();
						saleItem.setAmount(1);
						((TradingPlayerShopkeeper) this.shopkeeper).costs.remove(saleItem);
					}
				}
			}
			((TradingPlayerShopkeeper) this.shopkeeper).clickedItem = null;
		}
	}

	protected class TradingPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected TradingPlayerShopTradingHandler(UIManager uiManager, TradingPlayerShopkeeper shopkeeper) {
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

			Cost cost = ((TradingPlayerShopkeeper) this.shopkeeper).costs.get(type);
			if (cost == null) {
				event.setCancelled(true);
				return;
			}

			if (cost.amount != item.getAmount()) {
				event.setCancelled(true);
				return;
			}

			// get chest
			Block chest = ((TradingPlayerShopkeeper) this.shopkeeper).getChest();
			if (!Utils.isChest(chest.getType())) {
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

			// add traded items to chest
			if (cost.item1 == null) {
				event.setCancelled(true);
				return;
			} else {
				ItemStack c = cost.item1.clone();
				c.setAmount(this.getAmountAfterTaxes(c.getAmount()));
				if (c.getAmount() > 0) {
					boolean added = this.addToInventory(c, contents);
					if (!added) {
						event.setCancelled(true);
						return;
					}
				}
			}
			if (cost.item2 != null) {
				ItemStack c = cost.item2.clone();
				c.setAmount(this.getAmountAfterTaxes(c.getAmount()));
				if (c.getAmount() > 0) {
					boolean added = this.addToInventory(c, contents);
					if (!added) {
						event.setCancelled(true);
						return;
					}
				}
			}

			// save chest contents
			inventory.setContents(contents);
		}
	}

	private Map<ItemStack, Cost> costs;
	private ItemStack clickedItem;

	public TradingPlayerShopkeeper(ConfigurationSection config) {
		super(config);
		this.onConstruction();
	}

	public TradingPlayerShopkeeper(ShopCreationData creationData) {
		super(creationData);
		this.costs = new HashMap<ItemStack, Cost>();
		this.onConstruction();
	}

	private final void onConstruction() {
		this.registerUIHandler(new TradingPlayerShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new TradingPlayerShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
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
				Cost cost = new Cost();
				cost.amount = itemSection.getInt("amount");
				cost.item1 = itemSection.getItemStack("item1");
				cost.item2 = itemSection.getItemStack("item2");
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
			itemSection.set("item1", cost.item1);
			itemSection.set("item2", cost.item2);
			count++;
		}
	}

	@Override
	public ShopType<TradingPlayerShopkeeper> getType() {
		return DefaultShopTypes.PLAYER_TRADE;
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
					if (cost.item1 != null && cost.item1.getType() != Material.AIR && cost.item1.getAmount() > 0) {
						recipe[0] = cost.item1;
					}
					if (cost.item2 != null && cost.item2.getType() != Material.AIR && cost.item2.getAmount() > 0) {
						recipe[1] = cost.item2;
					}
					ItemStack saleItem = item.clone();
					saleItem.setAmount(cost.amount);
					recipe[2] = saleItem;
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
		if (Utils.isChest(chest.getType())) {
			Inventory inv = ((Chest) chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (item != null && item.getType() != Material.AIR) {
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
		ItemStack item1;
		ItemStack item2;

		public int getAmount() {
			return amount;
		}

		public void setAmount(int amount) {
			this.amount = amount;
		}

		public ItemStack getItem1() {
			return this.item1;
		}

		public void setItem1(ItemStack item) {
			this.item1 = item;
		}

		public ItemStack getItem2() {
			return this.item2;
		}

		public void setItem2(ItemStack item) {
			this.item2 = item;
		}
	}
}