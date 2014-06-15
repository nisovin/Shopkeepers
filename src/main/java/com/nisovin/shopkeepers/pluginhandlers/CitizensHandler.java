package com.nisovin.shopkeepers.pluginhandlers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class CitizensHandler {

	public static final String PLUGIN_NAME = "Citizens";

	public static boolean isEnabled() {
		Plugin plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
		return plugin != null && plugin.isEnabled();
	}

	// returns null if this entity is no citizens npc
	public static Integer getNPCId(Entity entity) {
		if (isEnabled()) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
			return npc != null ? npc.getId() : null;
		} else {
			return null;
		}
	}
}