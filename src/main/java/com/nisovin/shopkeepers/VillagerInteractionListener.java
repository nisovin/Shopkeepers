package com.nisovin.shopkeepers;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

class VillagerInteractionListener implements Listener {

	private final ShopkeepersPlugin plugin;

	VillagerInteractionListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	void onEntityInteract(PlayerInteractEntityEvent event) {
		if (!(event.getRightClicked() instanceof Villager)) return;
		Villager villager = (Villager) event.getRightClicked();

		if (plugin.isShopkeeper(villager)) return; // shopkeeper interaction is handled elsewhere
		Log.debug("Interaction with Non-shopkeeper villager ..");

		if (villager.hasMetadata("NPC")) {
			// ignore any interaction with citizens2 NPCs
			Log.debug("  ignoring (probably citizens2) NPC");
			return;
		}

		Player player = event.getPlayer();
		if (Settings.disableOtherVillagers) {
			// don't allow trading with other villagers
			event.setCancelled(true);
			if (Settings.hireOtherVillagers) {
				// allow hiring of other villagers
				Log.debug("  trade prevented, but possible hire ..");
				if (this.handleHireOtherVillager(player, villager)) {
					// hiring was successful -> prevent normal trading
					Log.debug("    ..success");
				} else {

				}
			} else {
				Log.debug("  trade prevented");
			}
		} else if (Settings.hireOtherVillagers) {
			// allow hiring of other villagers
			Log.debug("  possible hire ..");
			if (this.handleHireOtherVillager(player, villager)) {
				// hiring was successful -> prevent normal trading
				Log.debug("    ..success (normal trading prevented)");
				event.setCancelled(true);
			} else {
				// hiring was not successful -> no preventing of normal villager trading
				Log.debug("    ..failed");
			}
		}
	}

	// returns false, if the player wasn't able to hire this villager
	@SuppressWarnings("deprecation")
	// because of player.updateInventory()
	private boolean handleHireOtherVillager(Player player, Villager villager) {
		// hire him if holding his hiring item
		ItemStack inHand = player.getItemInHand();
		if (Settings.isHireItem(inHand)) {
			Inventory inventory = player.getInventory();
			// check if the player has enough of those hiring items
			int costs = Settings.hireOtherVillagersCosts;
			if (costs > 0) {
				if (Utils.hasInventoryItemsAtLeast(inventory, Settings.hireItem, (short) Settings.hireItemData, Settings.hireItemName, Settings.hireItemLore, costs)) {
					Log.debug("  Villager hiring: the player has the needed amount of hiring items");
					int inHandAmount = inHand.getAmount();
					int remaining = inHandAmount - costs;
					Log.debug("  Villager hiring: in hand=" + inHandAmount + " costs=" + costs + " remaining=" + remaining);
					if (remaining > 0) {
						inHand.setAmount(remaining);
					} else { // remaining <= 0
						player.setItemInHand(null); // remove item in hand
						if (remaining < 0) {
							// remove remaining costs from inventory
							Utils.removeItemsFromInventory(inventory, Settings.hireItem, (short) Settings.hireItemData, Settings.hireItemName, Settings.hireItemLore, -remaining);
						}
					}
				} else {
					Utils.sendMessage(player, Settings.msgCantHire);
					return false;
				}
			}

			// give player the creation item
			ItemStack creationItem = Settings.createCreationItem();
			Map<Integer, ItemStack> remaining = inventory.addItem(creationItem);
			if (!remaining.isEmpty()) {
				villager.getWorld().dropItem(villager.getLocation(), creationItem);
			}

			// remove the entity:
			villager.remove();

			// update client's inventory
			player.updateInventory();

			Utils.sendMessage(player, Settings.msgHired);
			return true;
		} else {
			Utils.sendMessage(player, Settings.msgVillagerForHire, "{costs}", String.valueOf(Settings.hireOtherVillagersCosts), "{hire-item}", Settings.hireItem.toString());
		}
		return false;
	}
}