package com.nisovin.shopkeepers.abstractTypes;

import org.bukkit.entity.Player;

public abstract class SelectableType extends AbstractType {

	// gets set and used by the SelectableTypeRegistry:
	SelectableType next = null;
	
	protected SelectableType(String identifier, String permission) {
		super(identifier, permission);
	}

	public abstract void onSelect(Player player);
}