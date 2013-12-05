package com.nisovin.shopkeepers.pluginhandlers;

import org.bukkit.Location;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class TownyHandler {

	public static boolean isCommercialArea(Location loc) {
		TownBlock townBlock = TownyUniverse.getTownBlock(loc);
		return townBlock.getType() == TownBlockType.COMMERCIAL;
	}
	
}
