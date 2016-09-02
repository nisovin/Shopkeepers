package com.nisovin.shopkeepers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

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
		if (shopkeeper == null) return;

		event.setCancelled(true);
		final String newName = event.getMessage().trim();

		Bukkit.getScheduler().runTask(plugin, new Runnable() {
			public void run() {
				shopkeeper.requestNameChange(player, newName);
			}
		});
	}
}
