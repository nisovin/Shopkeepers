package com.nisovin.shopkeepers.util;

import java.util.Map;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class ItemType {

	public int id;
	public short data;
	public String enchants;
	
	public ItemType() {
		
	}
	
	public ItemType(ItemStack item) {
		id = item.getTypeId();
		data = item.getDurability();
		Map<Enchantment, Integer> enchantments = item.getEnchantments();
		if (enchantments != null && enchantments.size() > 0) {
			enchants = "";
			for (Enchantment e : enchantments.keySet()) {
				enchants += e.getId() + ":" + enchantments.get(e) + " ";
			}
			enchants = enchants.trim();
		}
	}
	
	public ItemStack getItemStack(int amount) {
		ItemStack item = new ItemStack(id, amount, data);
		if (enchants != null) {
			String[] dataList = enchants.split(" ");
			for (String s : dataList) {
				String[] data = s.split(":");
				item.addUnsafeEnchantment(Enchantment.getById(Integer.parseInt(data[0])), Integer.parseInt(data[1]));
			}
		}
		return item;
	}
	
	@Override
	public int hashCode() {
		return (id + " " + data + (enchants != null ? " " + enchants : "")).hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ItemType)) return false;
		ItemType i = (ItemType)o;
		boolean test = (i.id == this.id && i.data == this.data);
		if (!test) return false;
		if (i.enchants == null && this.enchants == null) return true;
		if (i.enchants == null || this.enchants == null) return false;
		return i.enchants.equals(this.enchants);
	}
}
