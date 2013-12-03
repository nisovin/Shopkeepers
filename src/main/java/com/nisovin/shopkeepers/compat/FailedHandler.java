package com.nisovin.shopkeepers.compat;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;

import org.bukkit.ChatColor;

import java.util.List;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Shopkeeper;

public final class FailedHandler implements NMSCallProvider {
    
    @Override
    public boolean openTradeWindow(String name, List<ItemStack[]> recipes, Player player) {
        player.sendMessage(ChatColor.AQUA + "Shopkeepers needs an update.");
        player.sendMessage(ChatColor.AQUA + "Please log out then back in to fix your user");
        return false;
    }
    
    @Override
    public boolean openTradeWindow(Shopkeeper shopkeeper, Player player) {
        player.sendMessage(ChatColor.AQUA + "Shopkeepers needs an update.");
        player.sendMessage(ChatColor.AQUA + "Please log out then back in to fix your user");
        return false;
    }
    
    @Override
    public void overwriteLivingEntityAI(LivingEntity entity) {
        
    }
    
    @Override
    public void overwriteVillagerAI(LivingEntity villager) {
        
    }
    
    @Override
    public void setVillagerProfession(Villager villager, int profession) {
        
    }
    
    @Override
    public ItemStack loadItemAttributesFromString(ItemStack item, String data) {
        return null;
    }
    
    @Override
    public String saveItemAttributesToString(ItemStack item) {
        return null;        
    }


}