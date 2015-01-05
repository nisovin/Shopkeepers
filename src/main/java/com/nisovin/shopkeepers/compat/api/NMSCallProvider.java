package com.nisovin.shopkeepers.compat.api;

import java.util.List;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.Shopkeeper;

public interface NMSCallProvider {

	public String getVersionId();

	public boolean openTradeWindow(String name, List<ItemStack[]> recipes, Player player);

	public boolean openTradeWindow(Shopkeeper shopkeeper, Player player);

	public int getCurrentRecipePage(Inventory merchantInventory);

	public void overwriteLivingEntityAI(LivingEntity entity);

	public void overwriteVillagerAI(LivingEntity villager);

	public int getMaxVillagerProfession();

	public void setVillagerProfession(Villager villager, int profession);

	public void setEntitySilent(Entity entity, boolean silent);

	public ItemStack loadItemAttributesFromString(ItemStack item, String data);

	public String saveItemAttributesToString(ItemStack item);

	/**
	 * Compares the attribute types and modifiers of the given items.
	 * Does ignore the specific attribute uuids.
	 * 
	 * @param item1
	 *            A non-null itemstack.
	 * @param item2
	 *            A non-null itemstack.
	 * @return
	 */
	public boolean areAttributesSimilar(ItemStack item1, ItemStack item2);

	/**
	 * Performs additional version specific general item comparison.
	 * Item meta comparison is handled separately.
	 * It can be asserted that all other general item comparison was run before
	 * and that the given items are both not null.
	 * 
	 * @param item1
	 * @param item2
	 * @return A reason why the given items are not similar, otherwise null.
	 */
	public String areSimilarReasoned(ItemStack item1, ItemStack item2);

	/**
	 * Performs additional version specific item meta comparison.
	 * It can be asserted that all other general item comparison was run before
	 * and that the given item meta are both of the same type and not null.
	 * 
	 * @param itemMeta1
	 * @param itemMeta2
	 * @return A reason why the given item meta are not similar, otherwise null.
	 */
	public String areSimilarReasoned(ItemMeta itemMeta1, ItemMeta itemMeta2);

	public boolean supportsPlayerUUIDs();

	public UUID getUUID(OfflinePlayer player);

	public OfflinePlayer getOfflinePlayer(UUID uuid);
}