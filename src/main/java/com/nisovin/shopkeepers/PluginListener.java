package com.nisovin.shopkeepers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;

class PluginListener implements Listener {

	PluginListener() {
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	void onPluginEnable(PluginEnableEvent event) {
		String pluginName = event.getPlugin().getName();
		if (pluginName.equals(CitizensHandler.PLUGIN_NAME)) {
			CitizensHandler.enable();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	void onPluginDisable(PluginDisableEvent event) {
		String pluginName = event.getPlugin().getName();
		if (pluginName.equals(CitizensHandler.PLUGIN_NAME)) {
			CitizensHandler.disable();
		}
	}
}
