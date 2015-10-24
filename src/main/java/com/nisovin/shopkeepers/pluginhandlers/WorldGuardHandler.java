package com.nisovin.shopkeepers.pluginhandlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.DefaultFlag;

public class WorldGuardHandler {

	private static final String PLUGIN_NAME = "WorldGuard";

	public static Plugin getPlugin() {
		return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
	}

	public static boolean isShopAllowed(Player player, Location loc) {
		Plugin plugin = getPlugin();
		if (plugin != null) {
			WorldGuardPlugin wgPlugin = (WorldGuardPlugin) plugin;
			// allow shops in regions where the player can build or where the ENABLE_SHOP flag is set:
			return wgPlugin.canBuild(player, loc)
					|| wgPlugin.getRegionManager(loc.getWorld()).getApplicableRegions(loc).allows(DefaultFlag.ENABLE_SHOP);
		} else {
			return true;
		}
	}
}