package com.nisovin.shopkeepers.ui;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import com.nisovin.shopkeepers.Log;
import com.nisovin.shopkeepers.Utils;
import com.nisovin.shopkeepers.ui.UIManager.UISession;

class UIListener implements Listener {

	private final UITypeRegistry uiRegistry;

	UIListener(UITypeRegistry uiRegistry) {
		this.uiRegistry = uiRegistry;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	void onInventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer().getType() != EntityType.PLAYER) return;
		Player player = (Player) event.getPlayer();
		UISession session = this.uiRegistry.getSession(player);
		if (session != null) {
			Log.debug("Player " + player.getName() + " closed " + session.uiType.getIdentifier());
			// inform uiManager so that it can cleanup player data:
			session.uiType.onClose(player);
			// inform uiHandler so that it can react to it:
			if (session.handler.isInterface(event.getInventory())) {
				session.handler.onInventoryClose(event, player);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked().getType() != EntityType.PLAYER) return;
		Player player = (Player) event.getWhoClicked();
		UISession session = this.uiRegistry.getSession(player);
		if (session != null) {
			// inform uiHandler so that it can react to it:
			if (session.handler.isInterface(event.getInventory())) {
				session.handler.onInventoryClick(event, player);
			} else {
				// the player has some other inventory open but an active session.. let's close it
				event.setCancelled(true);
				session.uiType.onClose(player); // cleanup
				Utils.closeInventoryLater(player);
			}
		}
	}
}