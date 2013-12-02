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

import com.nisovin.shopkeepers.EditorClickResult;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeeperType;
import com.nisovin.shopkeepers.shopobjects.ShopObject;
import com.nisovin.shopkeepers.util.ItemType;

public class NormalPlayerShopkeeper extends PlayerShopkeeper {

	//private Map<ItemType, Cost> costs;
	private Map<ItemStack, Cost> costs;
	
	public NormalPlayerShopkeeper(ConfigurationSection config) {
		super(config);
	}

	public NormalPlayerShopkeeper(Player owner, Block chest, Location location, ShopObject shopObject) {
		super(owner, chest, location, shopObject);
		this.costs = new HashMap<ItemStack, Cost>();
	}
	
	@Override
	public void load(ConfigurationSection config) {
		super.load(config);		
		costs = new HashMap<ItemStack, Cost>();
		ConfigurationSection costsSection = config.getConfigurationSection("costs");
		if (costsSection != null) {
			for (String key : costsSection.getKeys(false)) {
				ConfigurationSection itemSection = costsSection.getConfigurationSection(key);
				ItemStack item;
				if (itemSection.contains("item")) {
					item = itemSection.getItemStack("item");
				} else {
					ItemType type = new ItemType();
					type.id = itemSection.getInt("id");
					type.data = (short)itemSection.getInt("data");
					if (itemSection.contains("enchants")) {
						type.enchants = itemSection.getString("enchants");
					}
					item = type.getItemStack(1);
				}
				Cost cost = new Cost(itemSection.getInt("amount"), itemSection.getInt("cost"));
				costs.put(item, cost);
			}
		}
	}
	
	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		ConfigurationSection costsSection = config.createSection("costs");
		int count = 0;
		for (ItemStack item : costs.keySet()) {
			Cost cost = costs.get(item);
			ConfigurationSection itemSection = costsSection.createSection(count + "");
			itemSection.set("item", item);
			itemSection.set("amount", cost.amount);
			itemSection.set("cost", cost.cost);
			count++;
		}
	}
	
	@Override
	public ShopkeeperType getType() {
		return ShopkeeperType.PLAYER_NORMAL;
	}
	
	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		Map<ItemStack, Integer> chestItems = getItemsFromChest();
		for (ItemStack item : costs.keySet()) {
			if (chestItems.containsKey(item)) {
				Cost cost = costs.get(item);
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
		return costs;
	}
	
	@Override
	public boolean onPlayerEdit(Player player) {
		Inventory inv = Bukkit.createInventory(player, 27, Settings.editorTitle);
		
		// add the sale types
		Map<ItemStack, Integer> typesFromChest = getItemsFromChest();
		int i = 0;
		for (ItemStack item : typesFromChest.keySet()) {
			Cost cost = costs.get(item);
			if (cost != null) {
				ItemStack saleItem = item.clone();
				saleItem.setAmount(cost.amount);
				inv.setItem(i, saleItem);
				setEditColumnCost(inv, i, cost.cost);
			} else {
				inv.setItem(i, item);
				setEditColumnCost(inv, i, 0);
			}
			i++;
			if (i > 8) break;
		}
		
		// add the special buttons
		setActionButtons(inv);
		
		player.openInventory(inv);
		
		return true;
	}

	@Override
	public EditorClickResult onEditorClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getRawSlot() >= 0 && event.getRawSlot() <= 7) {
			// handle changing sell stack size
			ItemStack item = event.getCurrentItem();
			if (item != null && item.getType() != Material.AIR) {
				int amt = item.getAmount();
				amt = getNewAmountAfterEditorClick(amt, event);
				if (amt <= 0) amt = 1;
				if (amt > item.getMaxStackSize()) amt = item.getMaxStackSize();
				item.setAmount(amt);
			}
			return EditorClickResult.NOTHING;
		} else {
			return super.onEditorClick(event);
		}
	}
	
	@Override
	protected void saveEditor(Inventory inv, Player player) {
		for (int i = 0; i < 8; i++) {
			ItemStack item = inv.getItem(i);
			if (item != null && item.getType() != Material.AIR) {
				int cost = getCostFromColumn(inv, i);
				if (cost > 0) {
					ItemStack saleItem = item.clone();
					saleItem.setAmount(1);
					costs.put(saleItem, new Cost(item.getAmount(), cost));
				} else {
					ItemStack saleItem = item.clone();
					saleItem.setAmount(1);
					costs.remove(saleItem);
				}
			}
		}
	}
	
	@Override
	public void onPlayerPurchaseClick(final InventoryClickEvent event) {
		// get type and cost
		ItemStack item = event.getCurrentItem();
		ItemStack type = item.clone();
		type.setAmount(1);
		if (!costs.containsKey(type)) {
			event.setCancelled(true);
			return;
		}
		Cost cost = costs.get(type);
		if (cost.amount != item.getAmount()) {
			event.setCancelled(true);
			return;
		}
		
		// get chest
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() != Material.CHEST) {
			event.setCancelled(true);
			return;
		}
		
		// remove item from chest
		Inventory inv = ((Chest)chest.getState()).getInventory();
		ItemStack[] contents = inv.getContents();
		boolean removed = removeFromInventory(item, contents);
		if (!removed) {
			event.setCancelled(true);
			return;
		}
		
		// add earnings to chest
		int amount = getAmountAfterTaxes(cost.cost);
		if (amount > 0) {
			if (Settings.highCurrencyItem == Material.AIR || cost.cost <= Settings.highCurrencyMinCost) {
				boolean added = addToInventory(new ItemStack(Settings.currencyItem, amount, Settings.currencyItemData), contents);
				if (!added) {
					event.setCancelled(true);
					return;
				}
			} else {
				int highCost = amount / Settings.highCurrencyValue;
				int lowCost = amount % Settings.highCurrencyValue;
				boolean added = false;
				if (highCost > 0) {
					added = addToInventory(new ItemStack(Settings.highCurrencyItem, highCost, Settings.highCurrencyItemData), contents);
					if (!added) {
						event.setCancelled(true);
						return;
					}
				}
				if (lowCost > 0) {
					added = addToInventory(new ItemStack(Settings.currencyItem, lowCost, Settings.currencyItemData), contents);
					if (!added) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}

		// save chest contents
		inv.setContents(contents);
	}
	
	private Map<ItemStack, Integer> getItemsFromChest() {
		Map<ItemStack, Integer> map = new LinkedHashMap<ItemStack, Integer>();
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() == Material.CHEST) {
			Inventory inv = ((Chest)chest.getState()).getInventory();
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
	
	public class Cost {
		int amount;
		int cost;
		
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
