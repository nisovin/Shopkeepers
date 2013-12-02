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
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopobjects.ShopObject;
import com.nisovin.shopkeepers.util.ItemType;

public class TradingPlayerShopkeeper extends PlayerShopkeeper {

	private Map<ItemStack, Cost> costs;
	
	private ItemStack clickedItem;
	
	public TradingPlayerShopkeeper(ConfigurationSection config) {
		super(config);
	}

	public TradingPlayerShopkeeper(Player owner, Block chest, Location location, ShopObject shopObject) {
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
				Cost cost = new Cost();
				cost.amount = itemSection.getInt("amount");
				if (itemSection.contains("item1") || itemSection.contains("item2")) {
					cost.item1 = itemSection.getItemStack("item1");
					cost.item2 = itemSection.getItemStack("item2");
				} else {
					cost.item1 = new ItemStack(itemSection.getInt("item1type"), itemSection.getInt("item1amount"), (short)itemSection.getInt("item1data"));
					cost.item2 = new ItemStack(itemSection.getInt("item2type"), itemSection.getInt("item2amount"), (short)itemSection.getInt("item2data"));
				}
				costs.put(item, cost);
			}
		}
	}
	
	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("type", "trade");
		ConfigurationSection costsSection = config.createSection("costs");
		int count = 0;
		for (ItemStack item : costs.keySet()) {
			Cost cost = costs.get(item);
			ConfigurationSection itemSection = costsSection.createSection(count + "");
			itemSection.set("item", item);
			itemSection.set("amount", cost.amount);
			itemSection.set("item1", cost.item1);
			itemSection.set("item2", cost.item2);
			count++;
		}
	}

	@Override
	public ShopkeeperType getType() {
		return ShopkeeperType.PLAYER_TRADE;
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
		return costs;
	}

	@Override
	protected boolean onPlayerEdit(Player player) {
		Inventory inv = Bukkit.createInventory(player, 27, Settings.editorTitle);
		
		// add the sale types
		Map<ItemStack, Integer> typesFromChest = getItemsFromChest();
		int i = 0;
		for (ItemStack item : typesFromChest.keySet()) {
			Cost cost = costs.get(item);
			if (cost != null) {
				item.setAmount(cost.amount);
				inv.setItem(i, item);
				if (cost.item1 != null && cost.item1.getType() != Material.AIR && cost.item1.getAmount() > 0) {
					inv.setItem(i + 9, cost.item1);
				}
				if (cost.item2 != null && cost.item2.getType() != Material.AIR && cost.item2.getAmount() > 0) {
					inv.setItem(i + 18, cost.item2);
				}
			} else {
				inv.setItem(i, item);
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
	public EditorClickResult onEditorClick(final InventoryClickEvent event) {
		event.setCancelled(true);
		final int slot = event.getRawSlot();
		if (slot >= 0 && slot <= 7) {
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
		} else if ((slot >= 9 && slot <= 16) || (slot >= 18 && slot <= 25)) {
			if (clickedItem != null) {
				// placing item
				Bukkit.getScheduler().scheduleSyncDelayedTask(ShopkeepersPlugin.getInstance(), new Runnable() {
					public void run() {
						event.getInventory().setItem(slot, clickedItem);
						clickedItem = null;
					}
				}, 1);
			} else {
				// changing stack size
				ItemStack item = event.getCurrentItem();
				if (item != null && item.getType() != Material.AIR) {
					int amt = item.getAmount();
					amt = getNewAmountAfterEditorClick(amt, event);
					if (amt <= 0) {
						event.getInventory().setItem(slot, null);
					} else {
						if (amt > item.getMaxStackSize()) amt = item.getMaxStackSize();
						item.setAmount(amt);
					}
				}
			}
			return EditorClickResult.NOTHING;
		} else if (slot > 27) {
			// clicking in player inventory
			if (event.isShiftClick() || !event.isLeftClick()) {
				return EditorClickResult.NOTHING;
			}
			ItemStack cursor = event.getCursor();
			if (cursor != null && cursor.getType() != Material.AIR) {
				return EditorClickResult.NOTHING;
			}
			ItemStack current = event.getCurrentItem();
			if (current != null && current.getType() != Material.AIR) {
				clickedItem = current.clone();
				clickedItem.setAmount(1);
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
				ItemStack cost1 = null, cost2 = null;
				ItemStack item1 = inv.getItem(i + 9);
				ItemStack item2 = inv.getItem(i + 18);
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
					costs.put(saleItem, cost);
				} else {
					ItemStack saleItem = item.clone();
					saleItem.setAmount(1);
					costs.remove(saleItem);
				}
			}
		}
		clickedItem = null;
	}

	@Override
	public void onPlayerPurchaseClick(InventoryClickEvent event) {
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
		
		// add traded items to chest
		if (cost.item1 == null) {
			event.setCancelled(true);
			return;
		} else {
			ItemStack c = cost.item1.clone();
			c.setAmount(getAmountAfterTaxes(c.getAmount()));
			if (c.getAmount() > 0) {
				boolean added = addToInventory(c, contents);
				if (!added) {
					event.setCancelled(true);
					return;
				}
			}
		}
		if (cost.item2 != null) {
			ItemStack c = cost.item2.clone();
			c.setAmount(getAmountAfterTaxes(c.getAmount()));
			if (c.getAmount() > 0) {
				boolean added = addToInventory(c, contents);
				if (!added) {
					event.setCancelled(true);
					return;
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
	
	public class Cost {
		
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
			return item1;
		}
		
		public void setItem1(ItemStack item) {
			this.item1 = item;
		}
		
		public ItemStack getItem2() {
			return item2;
		}
		
		public void setItem2(ItemStack item) {
			this.item2 = item;
		}
		
	}

}
