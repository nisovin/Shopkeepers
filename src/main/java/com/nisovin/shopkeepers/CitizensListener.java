package com.nisovin.shopkeepers;

import net.citizensnpcs.api.event.CitizensDisableEvent;
import net.citizensnpcs.api.event.CitizensEnableEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

class CitizensListener implements Listener {

	final ShopkeepersPlugin plugin;

	public CitizensListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onCitizenDisable(CitizensDisableEvent event) {
		
	}

	@EventHandler(priority = EventPriority.MONITOR)
	void onCitizenEnable(CitizensEnableEvent event) {
		
	}
}