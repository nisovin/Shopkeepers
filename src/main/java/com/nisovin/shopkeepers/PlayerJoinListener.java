package com.nisovin.shopkeepers;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.nisovin.shopkeepers.shoptypes.PlayerShopkeeper;

public class PlayerJoinListener implements Listener {

	public PlayerJoinListener() {
		// let's update the shopkeepers for all online players:
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.updateShopkeepersForPlayer(player);;
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		this.updateShopkeepersForPlayer(event.getPlayer());
	}
	
	// checks for missing owner uuids and updates owner names:
	private void updateShopkeepersForPlayer(Player player) {
		for (List<Shopkeeper> shopkeepers : ShopkeepersPlugin.getInstance().allShopkeepersByChunk.values()) {
			for (Shopkeeper shopkeeper : shopkeepers) {
				if (shopkeeper instanceof PlayerShopkeeper) {
					PlayerShopkeeper playerShop = (PlayerShopkeeper) shopkeeper;
					playerShop.updateOnPlayerJoin(player);
				}
			}
		}
	}
}