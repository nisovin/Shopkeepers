package com.nisovin.shopkeepers.volatilecode;

import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Shopkeeper;

public interface VolatileCodeHandle {

	public boolean openTradeWindow(String name, List<ItemStack[]> recipes, Player player);
	
	public boolean openTradeWindow(Shopkeeper shopkeeper, Player player);
	
	public void overwriteLivingEntityAI(LivingEntity entity);
	
	public void overwriteVillagerAI(LivingEntity villager);
	
	public void setVillagerProfession(Villager villager, int profession);
	
	public ItemStack loadItemAttributesFromString(ItemStack item, String data);
	
	public String saveItemAttributesToString(ItemStack item);
	
}
