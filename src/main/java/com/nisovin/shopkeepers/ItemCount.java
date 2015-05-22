package com.nisovin.shopkeepers;

import java.util.Collection;

import org.bukkit.inventory.ItemStack;

public class ItemCount {

	private final ItemStack item;
	private int amount;

	public ItemCount(ItemStack item, int initialAmount) {
		assert item != null;
		this.item = item;
		this.setAmount(initialAmount);
	}

	public ItemStack getItem() {
		return item;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		assert amount >= 0;
		this.amount = amount;
	}

	public void addAmount(int amount) {
		assert amount >= 0;
		this.amount += amount;
	}

	/**
	 * Utility method for searching through a collection of item count entries for a similar item,
	 * taking {@link Settings#ignoreNameAndLoreOfTradedItems} into account.
	 * 
	 * @param entries
	 * @param item
	 * @return
	 */
	public static ItemCount findSimilar(Collection<ItemCount> entries, ItemStack item) {
		if (entries != null && item != null) {
			for (ItemCount entry : entries) {
				if (Utils.areSimilar(entry.getItem(), item, Settings.ignoreNameAndLoreOfTradedItems)) {
					return entry;
				}
			}
		}
		return null;
	}
}
