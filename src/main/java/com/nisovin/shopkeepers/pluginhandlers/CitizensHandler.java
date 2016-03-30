package com.nisovin.shopkeepers.pluginhandlers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRemoveEvent;
import net.citizensnpcs.api.event.NPCRemoveTraitEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.trait.LookClose;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopobjects.CitizensShop;
import com.nisovin.shopkeepers.shopobjects.CitizensShopkeeperTrait;

public class CitizensHandler {

	public static final String PLUGIN_NAME = "Citizens";
	private static boolean enabled = false;
	private static CitizensListener citizensListener = null;

	public static Plugin getPlugin() {
		return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void enable() {
		if (enabled) {
			// disable first, to perform cleanup if needed:
			disable();
		}

		if (Settings.enableCitizenShops) {
			Plugin citizensPlugin = getPlugin();
			if (citizensPlugin != null && citizensPlugin.isEnabled()) {
				Log.info("Citizens found, enabling NPC shopkeepers.");
				// register shopkeeper trait:
				try {
					CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(CitizensShopkeeperTrait.class).withName(CitizensShopkeeperTrait.TRAIT_NAME));
				} catch (Throwable ex) {
					// throws an exception if the trait is already registered, for ex. after reloads (is no problem)
				}

				// register citizens listener:
				assert citizensListener == null;
				citizensListener = new CitizensListener();
				Bukkit.getPluginManager().registerEvents(citizensListener, ShopkeepersPlugin.getInstance());

				// enabled:
				enabled = true;
			} else {
				Log.warning("Citizens Shops enabled, but Citizens plugin not found or disabled.");
			}
		}
	}

	public static void disable() {
		if (!enabled) {
			// already disabled
			return;
		}

		// unregister citizens listener:
		if (citizensListener != null) {
			HandlerList.unregisterAll(citizensListener);
			citizensListener = null;
		}

		// disabled:
		enabled = false;
	}

	// returns null if this entity is no citizens npc (or citizens or citizens shops are disabled)
	public static Integer getNPCId(Entity entity) {
		if (enabled) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
			return npc != null ? npc.getId() : null;
		} else {
			return null;
		}
	}

	// returns the id of the created npc, or null
	public static Integer createNPC(Location location, EntityType entityType, String name) {
		if (!enabled) return null;
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
		if (!enabled) {
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
	public static void removeShopkeeperTraits() {
		if (!enabled) return;
		Iterator<NPC> npcs = CitizensAPI.getNPCRegistry().iterator();
		while (npcs.hasNext()) {
			NPC npc = npcs.next();
			if (npc.hasTrait(CitizensShopkeeperTrait.class)) {
				npc.removeTrait(CitizensShopkeeperTrait.class);
			}
		}
	}

	private static class CitizensListener implements Listener {

		CitizensListener() {
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		void onNPCRemoved(NPCRemoveEvent event) {
			NPC npc = event.getNPC();
			if (npc.hasTrait(CitizensShopkeeperTrait.class)) {
				CitizensShopkeeperTrait shopkeeperTrait = npc.getTrait(CitizensShopkeeperTrait.class);
				shopkeeperTrait.onTraitDeletion();
			}
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		void onTraitRemoved(NPCRemoveTraitEvent event) {
			if (event.getTrait() instanceof CitizensShopkeeperTrait) {
				CitizensShopkeeperTrait shopkeeperTrait = (CitizensShopkeeperTrait) event.getTrait();
				shopkeeperTrait.onTraitDeletion();
			}
		}
	}
}
