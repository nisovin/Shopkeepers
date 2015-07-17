package com.nisovin.shopkeepers.pluginhandlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.LookClose;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopobjects.CitizensShop;
import com.nisovin.shopkeepers.shopobjects.CitizensShopkeeperTrait;

public class CitizensHandler {

	public static final String PLUGIN_NAME = "Citizens";

	public static Plugin getPlugin() {
		return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
	}

	public static boolean isEnabled() {
		Plugin citizensPlugin = getPlugin();
		return (citizensPlugin != null && citizensPlugin.isEnabled());
	}

	// called once when Shopkeepers is enabled
	public static void onEnable() {
		if (Settings.enableCitizenShops) {
			if (isEnabled()) {
				Log.info("Citizens found, enabling NPC shopkeepers.");
				try {
					// register shopkeeper trait:
					CitizensShopkeeperTrait.registerTrait();
				} catch (Throwable ex) {
					// throws an exception if the trait is already registered, for ex. after reloads
				}
			} else {
				Log.warning("Citizens Shops enabled, but Citizens plugin not found or disabled.");
			}
		}
	}

	// returns null if this entity is no citizens npc
	public static Integer getNPCId(Entity entity) {
		if (isEnabled()) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
			return npc != null ? npc.getId() : null;
		} else {
			return null;
		}
	}

	// returns the id of the created npc, or null
	public static Integer createNPC(Location location, EntityType entityType, String name) {
		if (!isEnabled()) return null;
		NPC npc = CitizensAPI.getNPCRegistry().createNPC(entityType, name);
		if (npc == null) return null;
		// look towards near players:
		npc.getTrait(LookClose.class).lookClose(true);
		// this will log a debug message from citizens if it currently cannot spawn this npc,
		// but will then later attempt to spawn it when the chunk is loaded:
		npc.spawn(location);
		// npc.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
		return npc.getId();
	}

	public static void removeInvalidCitizensShopkeepers() {
		if (!isEnabled()) {
			// cannot determine which shopkeepers have a backing npc if citizens isn't running:
			return;
		}
		List<Shopkeeper> forRemoval = new ArrayList<Shopkeeper>();
		for (Shopkeeper shopkeeper : ShopkeepersPlugin.getInstance().getAllShopkeepers()) {
			if (shopkeeper.getShopObject() instanceof CitizensShop) {
				CitizensShop citizensShop = (CitizensShop) shopkeeper.getShopObject();
				Integer npcId = citizensShop.getNpcId();
				if (npcId == null) {
					// npc wasn't created yet, which is only the case if a shopkeeper got somehow created without
					// citizens being enabled:
					forRemoval.add(shopkeeper);
					Log.warning("Removing citizens shopkeeper at " + shopkeeper.getPositionString()
							+ ": NPC has not be created.");
				} else if (CitizensAPI.getNPCRegistry().getById(npcId.intValue()) == null) {
					// there is no npc with the stored id:
					forRemoval.add(shopkeeper);
					Log.warning("Removing citizens shopkeeper at " + shopkeeper.getPositionString()
							+ ": no NPC existing with id '" + npcId + "'.");
				} else if (ShopkeepersPlugin.getInstance().getActiveShopkeeper(shopkeeper.getObjectId()) != shopkeeper) {
					// there is already another citizens shopkeeper using this npc id:
					forRemoval.add(shopkeeper);
					Log.warning("Removing citizens shopkeeper at " + shopkeeper.getPositionString()
							+ ": there exists another shopkeeper using the same NPC with id '" + npcId + "'.");
				}
			}
		}

		// remove those shopkeepers:
		if (!forRemoval.isEmpty()) {
			for (Shopkeeper shopkeeper : forRemoval) {
				shopkeeper.delete();
			}

			// save:
			ShopkeepersPlugin.getInstance().save();
		}
	}

	// unused
	public static void cleanupUnusedShopkeeperTraits() {
		if (!isEnabled()) return;
		Iterator<NPC> npcs = CitizensAPI.getNPCRegistry().iterator();
		while (npcs.hasNext()) {
			NPC npc = npcs.next();
			if (npc.hasTrait(CitizensShopkeeperTrait.class)) {
				npc.removeTrait(CitizensShopkeeperTrait.class);
			}
		}
	}
}