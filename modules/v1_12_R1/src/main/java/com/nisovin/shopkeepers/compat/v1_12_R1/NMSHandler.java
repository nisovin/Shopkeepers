package com.nisovin.shopkeepers.compat.v1_12_R1;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;

import org.bukkit.craftbukkit.v1_12_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.SpawnEggMeta;

import net.minecraft.server.v1_12_R1.*;

import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.compat.api.NMSCallProvider;

public final class NMSHandler implements NMSCallProvider {

	@Override
	public String getVersionId() {
		return "1_12_R1";
	}

	// TODO this can be moved out of nms handler once we only support 1.11 upwards
	@Override
	public boolean openTradeWindow(String title, List<ItemStack[]> recipes, Player player) {
		// create empty merchant:
		Merchant merchant = Bukkit.createMerchant(title);

		// create list of merchant recipes:
		List<MerchantRecipe> merchantRecipes = new ArrayList<MerchantRecipe>();
		for (ItemStack[] recipe : recipes) {
			// skip invalid recipes:
			if (recipe == null || recipe.length != 3 || Utils.isEmpty(recipe[0]) || Utils.isEmpty(recipe[2])) {
				continue;
			}

			// create and add merchant recipe:
			merchantRecipes.add(this.createMerchantRecipe(recipe[0], recipe[1], recipe[2]));
		}

		// set merchant's recipes:
		merchant.setRecipes(merchantRecipes);

		// increase 'talked-to-villager' statistic:
		player.incrementStatistic(Statistic.TALKED_TO_VILLAGER);

		// open merchant:
		return player.openMerchant(merchant, true) != null;
	}

	private MerchantRecipe createMerchantRecipe(ItemStack buyItem1, ItemStack buyItem2, ItemStack sellingItem) {
		assert !Utils.isEmpty(sellingItem) && !Utils.isEmpty(buyItem1);
		MerchantRecipe recipe = new MerchantRecipe(sellingItem, 10000); // no max-uses limit
		recipe.setExperienceReward(false); // no experience rewards
		recipe.addIngredient(buyItem1);
		if (!Utils.isEmpty(buyItem2)) {
			recipe.addIngredient(buyItem2);
		}
		return recipe;
	}

	// TODO this can be moved out of nms handler once we only support 1.11 upwards
	@Override
	public ItemStack[] getUsedTradingRecipe(MerchantInventory merchantInventory) {
		MerchantRecipe merchantRecipe = merchantInventory.getSelectedRecipe();
		List<ItemStack> ingredients = merchantRecipe.getIngredients();
		ItemStack[] recipe = new ItemStack[3];
		recipe[0] = ingredients.get(0);
		recipe[1] = null;
		if (ingredients.size() > 1) {
			ItemStack buyItem2 = ingredients.get(1);
			if (!Utils.isEmpty(buyItem2)) {
				recipe[1] = buyItem2;
			}
		}
		recipe[2] = merchantRecipe.getResult();
		return recipe;
	}

	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving mcLivingEntity = ((CraftLivingEntity) entity).getHandle();
			// example: armor stands are living, but not insentient
			if (!(mcLivingEntity instanceof EntityInsentient)) return;

			// make goal selector items accessible:
			Field bField = PathfinderGoalSelector.class.getDeclaredField("b");
			bField.setAccessible(true);
			Field cField = PathfinderGoalSelector.class.getDeclaredField("c");
			cField.setAccessible(true);

			// overwrite goal selector:
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(mcLivingEntity);

			// clear old goals:
			Set<?> goals_b = (Set<?>) bField.get(goals);
			goals_b.clear();
			Set<?> goals_c = (Set<?>) cField.get(goals);
			goals_c.clear();

			// add new goals:
			goals.a(0, new PathfinderGoalFloat((EntityInsentient) mcLivingEntity));
			goals.a(1, new PathfinderGoalLookAtPlayer((EntityInsentient) mcLivingEntity, EntityHuman.class, 12.0F, 1.0F));

			// overwrite target selector:
			Field targetsField = EntityInsentient.class.getDeclaredField("targetSelector");
			targetsField.setAccessible(true);
			PathfinderGoalSelector targets = (PathfinderGoalSelector) targetsField.get(mcLivingEntity);

			// clear old target goals:
			Set<?> targets_b = (Set<?>) bField.get(targets);
			targets_b.clear();
			Set<?> targets_c = (Set<?>) cField.get(targets);
			targets_c.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setEntitySilent(org.bukkit.entity.Entity entity, boolean silent) {
		entity.setSilent(silent);
	}

	@Override
	public void setNoAI(LivingEntity bukkitEntity) {
		bukkitEntity.setAI(false);
	}

	// TODO no longer needed once attribute saving and loading has been removed
	private NBTTagCompound getItemTag(net.minecraft.server.v1_12_R1.ItemStack itemStack) {
		if (itemStack == null) return null;
		try {
			Field tag = itemStack.getClass().getDeclaredField("tag");
			tag.setAccessible(true);
			return (NBTTagCompound) tag.get(itemStack);
		} catch (NoSuchFieldException e) {
			return null;
		} catch (IllegalAccessException e2) {
			return null;
		}
	}

	// TODO no longer needed once attribute saving and loading has been removed
	private void setItemTag(net.minecraft.server.v1_12_R1.ItemStack itemStack, NBTTagCompound newTag) {
		if (itemStack == null) return;
		try {
			Field tag = itemStack.getClass().getDeclaredField("tag");
			tag.setAccessible(true);
			tag.set(itemStack, newTag);
		} catch (NoSuchFieldException e) {
		} catch (IllegalAccessException e2) {
		}
	}

	@Override
	public ItemStack loadItemAttributesFromString(ItemStack item, String data) {
		// since somewhere in late bukkit 1.8, bukkit saves item attributes on its own (inside the internal data)
		// this is currently kept in, in case some old shopkeeper data gets imported, for which attributes weren't yet
		// serialized to the internal data by bukkit
		// TODO remove this in the future
		NBTTagList list = new NBTTagList();
		String[] attrs = data.split(";");
		for (String s : attrs) {
			if (!s.isEmpty()) {
				String[] attrData = s.split(",");
				NBTTagCompound attr = new NBTTagCompound();
				attr.setString("Name", attrData[0]);
				attr.setString("AttributeName", attrData[1]);
				attr.setDouble("Amount", Double.parseDouble(attrData[2]));
				attr.setInt("Operation", Integer.parseInt(attrData[3]));
				attr.setLong("UUIDLeast", Long.parseLong(attrData[4]));
				attr.setLong("UUIDMost", Long.parseLong(attrData[5]));
				// MC 1.9 addition: not needed, as Slot-serialization wasn't ever published
				/*if (attrData.length >= 7) {
					attr.setString("Slot", attrData[6]);
				}*/
				list.add(attr);
			}
		}
		net.minecraft.server.v1_12_R1.ItemStack i = CraftItemStack.asNMSCopy(item);
		NBTTagCompound tag = this.getItemTag(i);
		if (tag == null) {
			tag = new NBTTagCompound();
			this.setItemTag(i, tag);
		}
		tag.set("AttributeModifiers", list);
		return CraftItemStack.asBukkitCopy(i);
	}

	@Override
	public String saveItemAttributesToString(ItemStack item) {
		// since somewhere in late bukkit 1.8, bukkit saves item attributes on its own (inside the internal data)
		return null;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEvent event) {
		return event.getHand() == EquipmentSlot.HAND;
	}

	@Override
	public boolean isMainHandInteraction(PlayerInteractEntityEvent event) {
		return event.getHand() == EquipmentSlot.HAND;
	}

	@Override
	public void setSpawnEggEntityType(ItemStack spawnEggItem, EntityType entityType) {
		assert spawnEggItem != null && spawnEggItem.getType() == org.bukkit.Material.MONSTER_EGG;
		if (entityType == null && !spawnEggItem.hasItemMeta()) return;
		SpawnEggMeta itemMeta = (SpawnEggMeta) spawnEggItem.getItemMeta();
		itemMeta.setSpawnedType(entityType);
		spawnEggItem.setItemMeta(itemMeta);
	}

	@Override
	public EntityType getSpawnEggEntityType(ItemStack spawnEggItem) {
		assert spawnEggItem != null && spawnEggItem.getType() == org.bukkit.Material.MONSTER_EGG;
		if (!spawnEggItem.hasItemMeta()) return null;
		SpawnEggMeta itemMeta = (SpawnEggMeta) spawnEggItem.getItemMeta();
		return itemMeta.getSpawnedType();
	}
}
