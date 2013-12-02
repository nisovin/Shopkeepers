package com.nisovin.shopkeepers.volatilecode;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.server.v1_6_R3.*;

import org.bukkit.craftbukkit.v1_6_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftVillager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Shopkeeper;

public class VolatileCode_1_6_R3 implements VolatileCodeHandle {

	@SuppressWarnings("unchecked")
	@Override
	public boolean openTradeWindow(String name, List<ItemStack[]> recipes, Player player) {
		try {
			EntityVillager villager = new EntityVillager(((CraftPlayer)player).getHandle().world, 0);
			if (name != null && !name.isEmpty()) {
				villager.setCustomName(name);
			}
			
			Field recipeListField = EntityVillager.class.getDeclaredField("bu");
			recipeListField.setAccessible(true);
			MerchantRecipeList recipeList = (MerchantRecipeList)recipeListField.get(villager);
			if (recipeList == null) {
				recipeList = new MerchantRecipeList();
				recipeListField.set(villager, recipeList);
			}
			recipeList.clear();
			for (ItemStack[] recipe : recipes) {
				recipeList.add(createMerchantRecipe(recipe[0], recipe[1], recipe[2]));
			}
			
			villager.a(((CraftPlayer)player).getHandle());
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean openTradeWindow(Shopkeeper shopkeeper, Player player) {
		return openTradeWindow(shopkeeper.getName(), shopkeeper.getRecipes(), player);
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void overwriteLivingEntityAI(LivingEntity entity) {
		try {
			EntityLiving ev = ((CraftLivingEntity)entity).getHandle();
			
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			List list = (List)listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat((EntityInsentient) ev));
			goals.a(1, new PathfinderGoalLookAtPlayer((EntityInsentient) ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void overwriteVillagerAI(LivingEntity villager) {
		try {
			EntityVillager ev = ((CraftVillager)villager).getHandle();
			
			Field goalsField = EntityInsentient.class.getDeclaredField("goalSelector");
			goalsField.setAccessible(true);
			PathfinderGoalSelector goals = (PathfinderGoalSelector) goalsField.get(ev);
			
			Field listField = PathfinderGoalSelector.class.getDeclaredField("a");
			listField.setAccessible(true);
			List list = (List)listField.get(goals);
			list.clear();
			listField = PathfinderGoalSelector.class.getDeclaredField("b");
			listField.setAccessible(true);
			list = (List)listField.get(goals);
			list.clear();

			goals.a(0, new PathfinderGoalFloat(ev));
			goals.a(1, new PathfinderGoalTradeWithPlayer(ev));
			goals.a(1, new PathfinderGoalLookAtTradingPlayer(ev));
			goals.a(2, new PathfinderGoalLookAtPlayer(ev, EntityHuman.class, 12.0F, 1.0F));
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}

	@Override
	public void setVillagerProfession(Villager villager, int profession) {
		((CraftVillager)villager).getHandle().setProfession(profession);
	}
	
	private MerchantRecipe createMerchantRecipe(ItemStack item1, ItemStack item2, ItemStack item3) {
		MerchantRecipe recipe = new MerchantRecipe(convertItemStack(item1), convertItemStack(item2), convertItemStack(item3));
		try {
			Field maxUsesField = MerchantRecipe.class.getDeclaredField("maxUses");
			maxUsesField.setAccessible(true);
			maxUsesField.set(recipe, 10000);
		} catch (Exception e) {}
		return recipe;
	}
	
	private net.minecraft.server.v1_6_R3.ItemStack convertItemStack(org.bukkit.inventory.ItemStack item) {
		if (item == null) return null;
		return org.bukkit.craftbukkit.v1_6_R3.inventory.CraftItemStack.asNMSCopy(item);
	}

	
}
