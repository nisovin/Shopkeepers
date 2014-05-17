package com.nisovin.shopkeepers.abstractTypes;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.map.LinkedMap;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

public abstract class SelectableTypeRegistry<T extends SelectableType> extends TypeRegistry<T> {

	// registered types:
	private final LinkedMap<String, T> registeredTypes = new LinkedMap<String, T>();

	@Override
	protected Map<String, T> getTypesMap() {
		return this.registeredTypes;
	}

	protected T getFirst() {
		return this.registeredTypes.getValue(0);
	}

	protected T getNext(T current) {
		if (current == null) return this.getFirst();
		String identifier = current.getIdentifier();
		String nextId = this.registeredTypes.nextKey(identifier);
		if (nextId == null) {
			return this.getFirst();
		} else {
			return this.registeredTypes.get(nextId);
		}
	}

	protected boolean canBeSelected(Player player, T type) {
		assert player != null && type != null;
		return type.isEnabled() && type.hasPermission(player);
	}

	protected T getNext(Player player, T current) {
		assert player != null;
		T next = current;

		int count = this.numberOfRegisteredTypes();
		while (count > 0) {
			next = this.getNext(next); // automatically selects the first type, if next is null or if next is the last type
			if (this.canBeSelected(player, next)) {
				break;
			}
			count--;
		}

		// use the currently selected type (can be null) after it went through all types and didn't find one the player can use:
		if (count == 0) {
			// check if the currently selected type can still be used by this player:
			if (current != null && !this.canBeSelected(player, current)) current = null;
			next = current;
		}
		return next;
	}

	// SELECTION MANAGEMENT

	protected final Map<String, T> selections = new HashMap<String, T>();

	/**
	 * Gets the first select-able type for this player.
	 * 
	 * @param player
	 *            a player
	 * @return the first select-able type for this player, or null if this player can't select/use any type at all
	 */
	public T getDefaultSelection(Player player) {
		return this.getNext(player, null);
	}

	public T getSelection(Player player) {
		Validate.notNull(player);
		String playerName = player.getName();
		T current = this.selections.get(playerName);
		// if none is currently selected, let's search for the first type this player can use:
		if (current == null) current = this.getNext(player, current);
		return current; // returns null if the player can use no type at all
	}

	public T selectNext(Player player) {
		Validate.notNull(player);
		String playerName = player.getName();
		T current = this.selections.get(playerName);
		T next = this.getNext(player, current);
		if (next != null) {
			this.selections.put(playerName, next);
			next.onSelect(player);
		}
		return next;
	}

	public void clearSelection(Player player) {
		assert player != null;
		String playerName = player.getName();
		this.selections.remove(playerName);
	}

	public void clearAllSelections() {
		this.selections.clear();
	}
}