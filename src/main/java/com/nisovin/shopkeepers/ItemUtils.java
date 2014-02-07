package com.nisovin.shopkeepers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemUtils {

	// itemstack utilities:

	public static ItemStack createItemStack(Material type, short data, String name, List<String> lore) {
		ItemStack item = new ItemStack(type, 1, data);
		return setItemStackNameAndLore(item, name, lore);
	}

	public static ItemStack setItemStackNameAndLore(ItemStack item, String name, List<String> lore) {
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
		List<String> loreColored = new ArrayList<String>(lore.size());
		for (String loreString : lore) {
			loreColored.add(ChatColor.translateAlternateColorCodes('&', loreString));
		}
		meta.setLore(loreColored);
		item.setItemMeta(meta);
		return item;
	}
	
	// inventory utilities:
	
	public static boolean hasInventoryItemsAtLeast(Inventory inv, Material type, short data, int amount) {
		for (ItemStack is : inv.getContents()) {
			if (is != null && is.getType() == type && is.getDurability() == data) {
				int currentAmount = is.getAmount() - amount;
				if (currentAmount >= 0) {
					return true;
				} else {
					amount = -currentAmount;
				}
			}
		}
		return false;
	}

	public static void removeItemsFromInventory(Inventory inv, Material type, short data, int amount) {
		for (ItemStack is : inv.getContents()) {
			if (is != null && is.getType() == type && is.getDurability() == data) {
				int newamount = is.getAmount() - amount;
				if (newamount > 0) {
					is.setAmount(newamount);
					break;
				} else {
					inv.remove(is);
					amount = -newamount;
					if (amount == 0) break;
				}
			}
		}
	}
}