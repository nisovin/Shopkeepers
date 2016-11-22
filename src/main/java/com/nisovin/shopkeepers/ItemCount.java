package com.nisovin.shopkeepers;

import java.util.Collection;

import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;

public class ItemCount {

	private final ItemStack item;
	private int amount;

	public ItemCount(ItemStack item, int initialAmount) {
		Validate.notNull(item, "Item cannot be null!");
		this.item = item.clone();
		this.item.setAmount(1);
		this.setAmount(initialAmount);
	}

	public ItemStack getItem() {
		return item;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		Validate.isTrue(amount >= 0, "Amount cannot be negative!");
		this.amount = amount;
	}

	public void addAmount(int amount) {
		Validate.isTrue(amount >= 0, "Amount cannot be negative!");
		this.amount += amount;
	}

	/**
	 * Utility method for finding an {@link ItemCount} matching the given item.
	 * 
	 * @param itemCounts
	 *            the item counts
	 * @param item
	 *            the item to search for
	 * @return the matching item count, or <code>null</code> if none was found
	 */
	public static ItemCount findSimilar(Collection<ItemCount> itemCounts, ItemStack item) {
		if (itemCounts != null && item != null) {
			for (ItemCount entry : itemCounts) {
				if (Utils.isSimilar(entry.getItem(), item)) {
					return entry;
				}
			}
		}
		return null;
	}
}
