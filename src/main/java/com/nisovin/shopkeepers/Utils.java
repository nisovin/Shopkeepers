package com.nisovin.shopkeepers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Utils {

	public static final BlockFace[] chestProtectFaces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
	public static final BlockFace[] hopperProtectFaces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN };

	// messages:

	public static void sendMessage(Player player, String message, String... args) {
		// skip if player is null or message is "empty":
		if (player == null || message == null || message.isEmpty()) return;
		if (args != null && args.length >= 2) {
			// replace arguments (key-value replacement):
			String key = args[0];
			for (int i = 1; i < args.length; i++) {
				String value = args[i];
				if (key == null || value == null) continue; // skip invalid arguments
				message = message.replace(key, value);
			}
		}

		message = ChatColor.translateAlternateColorCodes('&', message);
		String[] msgs = message.split("\n");
		for (String msg : msgs) {
			player.sendMessage(msg);
		}
	}

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

	public static void closeInventoryLater(final HumanEntity player) {
		Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {
			public void run() {
				player.closeInventory();
			}
		}, 1);
	}

	@SuppressWarnings("deprecation")
	public static void updateInventoryLater(final Player player) {
		Bukkit.getScheduler().runTaskLater(ShopkeepersPlugin.getInstance(), new Runnable() {

			@Override
			public void run() {
				player.updateInventory();
			}
		}, 3L);
	}
}