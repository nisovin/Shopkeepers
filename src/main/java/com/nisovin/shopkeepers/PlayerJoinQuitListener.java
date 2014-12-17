package com.nisovin.shopkeepers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

class PlayerJoinQuitListener implements Listener {

	private final ShopkeepersPlugin plugin;

	PlayerJoinQuitListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		plugin.updateShopkeepersForPlayer(player.getUniqueId(), player.getName());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerQuit(PlayerQuitEvent event) {
		plugin.onPlayerQuit(event.getPlayer());
	}
}