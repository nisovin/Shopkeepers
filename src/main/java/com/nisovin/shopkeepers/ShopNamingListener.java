package com.nisovin.shopkeepers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.nisovin.shopkeepers.events.ShopkeeperEditedEvent;

class ShopNamingListener implements Listener {

	private final ShopkeepersPlugin plugin;

	ShopNamingListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	// SHOPKEEPER NAMING

	@EventHandler(priority = EventPriority.LOWEST)
	void onChat(AsyncPlayerChatEvent event) {
		final Player player = event.getPlayer();
		final Shopkeeper shopkeeper = plugin.endNaming(player);
		if (shopkeeper == null || !shopkeeper.isValid()) return;

		event.setCancelled(true);
		final String message = event.getMessage().trim();

		Bukkit.getScheduler().runTask(plugin, new Runnable() {
			public void run() {
				// update name:
				if (message.isEmpty() || message.equals("-")) {
					// remove name:
					shopkeeper.setName("");
				} else {
					// validate name:
					if (!message.matches("^" + Settings.nameRegex + "$")) {
						Utils.sendMessage(player, Settings.msgNameInvalid);
						return;
					}
					// set name:
					if (message.length() > 32) {
						shopkeeper.setName(message.substring(0, 32));
					} else {
						shopkeeper.setName(message);
					}
				}

				Utils.sendMessage(player, Settings.msgNameSet);
				shopkeeper.closeAllOpenWindows();

				// run event:
				Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(player, shopkeeper));

				// save:
				plugin.save();
			}
		});
	}
}
