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

import com.nisovin.shopkeepers.EditorClickResult;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeeperType;
import com.nisovin.shopkeepers.shopobjects.ShopObject;

public class BuyingPlayerShopkeeper extends PlayerShopkeeper {

	private Map<ItemStack, Cost> costs;
	
	public BuyingPlayerShopkeeper(ConfigurationSection config) {
		super(config);
	}

	public BuyingPlayerShopkeeper(Player owner, Block chest, Location location, ShopObject shopObject) {
		super(owner, chest, location, shopObject);
		costs = new HashMap<ItemStack, Cost>();
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
					item = new ItemStack(itemSection.getInt("id"), 1, (short)itemSection.getInt("data"));
				}
				Cost cost = new Cost();
				cost.amount = itemSection.getInt("amount");
				cost.cost = itemSection.getInt("cost");
				costs.put(item, cost);
			}
		}
	}
	
	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("type", "buy");
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
		return ShopkeeperType.PLAYER_BUY;
	}
	
	@Override
	public List<ItemStack[]> getRecipes() {
		List<ItemStack[]> recipes = new ArrayList<ItemStack[]>();
		List<ItemStack> chestItems = getTypesFromChest();
		int chestTotal = getCurrencyInChest();
		for (ItemStack type : costs.keySet()) {
			if (chestItems.contains(type)) {
				Cost cost = costs.get(type);
				if (chestTotal >= cost.cost) {
					ItemStack[] recipe = new ItemStack[3];
					recipe[0] = type.clone();
					recipe[0].setAmount(cost.amount);
					recipe[2] = new ItemStack(Settings.currencyItem, cost.cost, Settings.currencyItemData);
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
	protected boolean onPlayerEdit(Player player) {
		Inventory inv = Bukkit.createInventory(player, 27, Settings.editorTitle);
		
		List<ItemStack> types = getTypesFromChest();
		for (int i = 0; i < types.size() && i < 8; i++) {
			ItemStack type = types.get(i);
			Cost cost = costs.get(type);
			
			if (cost != null) {
				if (cost.cost == 0) {
					inv.setItem(i, new ItemStack(Settings.zeroItem));
				} else {
					inv.setItem(i, new ItemStack(Settings.currencyItem, cost.cost, Settings.currencyItemData));
				}
				int amt = cost.amount;
				if (amt <= 0) amt = 1;
				type.setAmount(amt);
				inv.setItem(i + 18, type);
			} else {
				inv.setItem(i, new ItemStack(Settings.zeroItem));
				inv.setItem(i + 18, type);
			}
		}
		
		setActionButtons(inv);
		
		player.openInventory(inv);
		
		return true;
	}

	@Override
	public EditorClickResult onEditorClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getRawSlot() >= 0 && event.getRawSlot() <= 7) {
			// modifying cost
			ItemStack item = event.getCurrentItem();
			if (item != null) {
				if (item.getTypeId() == Settings.currencyItem) {
					int amount = item.getAmount();
					amount = getNewAmountAfterEditorClick(amount, event);
					if (amount > 64) amount = 64;
					if (amount <= 0) {
						item.setTypeId(Settings.zeroItem);
						item.setDurability((short)0);
						item.setAmount(1);
					} else {
						item.setAmount(amount);
					}
				} else if (item.getTypeId() == Settings.zeroItem) {
					item.setTypeId(Settings.currencyItem);
					item.setDurability(Settings.currencyItemData);
					item.setAmount(1);
				}
			}
			
		} else if (event.getRawSlot() >= 18 && event.getRawSlot() <= 25) {
			// modifying quantity
			ItemStack item = event.getCurrentItem();
			if (item != null && item.getTypeId() != 0) {
				int amt = item.getAmount();
				amt = getNewAmountAfterEditorClick(amt, event);
				if (amt <= 0) amt = 1;
				if (amt > item.getMaxStackSize()) amt = item.getMaxStackSize();
				item.setAmount(amt);
			}
			
		} else if (event.getRawSlot() >= 9 && event.getRawSlot() <= 16) {
		} else {
			return super.onEditorClick(event);
		}
		return EditorClickResult.NOTHING;
	}
	
	@Override
	protected void saveEditor(Inventory inv, Player player) {
		for (int i = 0; i < 8; i++) {
			ItemStack item = inv.getItem(i + 18);
			if (item != null) {
				ItemStack costItem = inv.getItem(i);
				ItemStack saleItem = item.clone();
				saleItem.setAmount(1);
				if (costItem != null && costItem.getTypeId() == Settings.currencyItem && costItem.getAmount() > 0) {
					costs.put(saleItem, new Cost(item.getAmount(), costItem.getAmount()));
				} else {
					costs.remove(saleItem);
				}
			}
		}
	}

	@Override
	public void onPlayerPurchaseClick(InventoryClickEvent event) {
		// get type and cost
		ItemStack item = event.getInventory().getItem(0);
		ItemStack type = item.clone();
		type.setAmount(1);
		if (!costs.containsKey(type)) {
			event.setCancelled(true);
			return;
		}
		Cost cost = costs.get(type);
		if (cost.amount > item.getAmount()) {
			event.setCancelled(true);
			return;
		}
		
		// get chest
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() != Material.CHEST) {
			event.setCancelled(true);
			return;
		}
		
		// remove currency from chest
		Inventory inv = ((Chest)chest.getState()).getInventory();
		ItemStack[] contents = inv.getContents();
		boolean removed = removeCurrencyFromChest(cost.cost, contents);
		if (!removed) {
			event.setCancelled(true);
			return;
		}
		
		// add items to chest
		int amount = getAmountAfterTaxes(cost.amount);
		if (amount > 0) {
			type.setAmount(amount);
			boolean added = addToInventory(type, contents);
			if (!added) {
				event.setCancelled(true);
				return;
			}
		}

		// save chest contents
		inv.setContents(contents);
	}
	
	private List<ItemStack> getTypesFromChest() {
		List<ItemStack> list = new ArrayList<ItemStack>();
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() == Material.CHEST) {
			Inventory inv = ((Chest)chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (item != null && item.getType() != Material.AIR && item.getTypeId() != Settings.currencyItem && item.getTypeId() != Settings.highCurrencyItem && item.getType() != Material.WRITTEN_BOOK && item.getEnchantments().size() == 0) {
					ItemStack saleItem = item.clone();
					saleItem.setAmount(1);
					if (!list.contains(saleItem)) {
						list.add(saleItem);
					}
				}
			}
		}
		return list;
	}
	
	private int getCurrencyInChest() {
		int total = 0;
		Block chest = Bukkit.getWorld(world).getBlockAt(chestx, chesty, chestz);
		if (chest.getType() == Material.CHEST) {
			Inventory inv = ((Chest)chest.getState()).getInventory();
			ItemStack[] contents = inv.getContents();
			for (ItemStack item : contents) {
				if (item != null && item.getTypeId() == Settings.currencyItem && item.getDurability() == Settings.currencyItemData) {
					total += item.getAmount();
				} else if (item != null && item.getTypeId() == Settings.highCurrencyItem && item.getDurability() == Settings.highCurrencyItemData) {
					total += item.getAmount() * Settings.highCurrencyValue;
				}
			}
		}
		return total;
	}
	
	private boolean removeCurrencyFromChest(int amount, ItemStack[] contents) {
		int remaining = amount;
		
		// first pass - remove currency
		int emptySlot = -1;
		for (int i = 0; i < contents.length; i++) {
			ItemStack item = contents[i];
			if (item != null) {
				if (Settings.highCurrencyItem > 0 && remaining >= Settings.highCurrencyValue && item.getTypeId() == Settings.highCurrencyItem && item.getDurability() == Settings.highCurrencyItemData) {
					int needed = remaining / Settings.highCurrencyValue;
					int amt = item.getAmount();
					if (amt > needed) {
						item.setAmount(amt - needed);
						remaining = remaining - (needed * Settings.highCurrencyValue);
					} else {
						contents[i] = null;
						remaining = remaining - (amt * Settings.highCurrencyValue);						
					}
				} else if (item.getTypeId() == Settings.currencyItem && item.getDurability() == Settings.currencyItemData) {
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
		if (remaining > 0 && remaining <= Settings.highCurrencyValue && Settings.highCurrencyItem > 0 && emptySlot >= 0) {
			for (int i = 0; i < contents.length; i++) {
				ItemStack item = contents[i];
				if (item != null && item.getTypeId() == Settings.highCurrencyItem && item.getDurability() == Settings.highCurrencyItemData) {
					if (item.getAmount() == 1) {
						contents[i] = null;
					} else {
						item.setAmount(item.getAmount() - 1);
					}
					int stackSize = Settings.highCurrencyValue - remaining;
					if (stackSize > 0) {
						contents[emptySlot] = new ItemStack(Settings.currencyItem, stackSize, Settings.currencyItemData);
					}
					return true;
				}
			}
		}
		
		return false;
	}
	
	public class Cost {
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
