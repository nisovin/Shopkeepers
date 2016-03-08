package com.nisovin.shopkeepers.compat.api;

import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Shopkeeper;

public interface NMSCallProvider {

	public String getVersionId();

	public boolean openTradeWindow(String name, List<ItemStack[]> recipes, Player player);

	public boolean openTradeWindow(Shopkeeper shopkeeper, Player player);

	public ItemStack[] getUsedTradingRecipe(Inventory merchantInventory);

	public void overwriteLivingEntityAI(LivingEntity entity);

	public void overwriteVillagerAI(LivingEntity villager);

	public int getMaxVillagerProfession();

	public void setVillagerProfession(Villager villager, int profession);

	public void setEntitySilent(Entity entity, boolean silent);

	public void setNoAI(LivingEntity bukkitEntity);

	public ItemStack loadItemAttributesFromString(ItemStack item, String data);

	public String saveItemAttributesToString(ItemStack item);

	public boolean supportsPlayerUUIDs();

	public UUID getUUID(OfflinePlayer player);

	public OfflinePlayer getOfflinePlayer(UUID uuid);

	public boolean isMainHandInteraction(PlayerInteractEvent event);

	public boolean isMainHandInteraction(PlayerInteractEntityEvent event);
}