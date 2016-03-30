package com.nisovin.shopkeepers.pluginhandlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyHandler {

	public static boolean isCommercialArea(Location loc) {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("Towny");
		if (plugin != null) {
			TownBlock townBlock = TownyUniverse.getTownBlock(loc);
			return townBlock.getType() == TownBlockType.COMMERCIAL;
		} else {
			return false;
		}
	}
}
