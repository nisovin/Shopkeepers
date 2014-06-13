package com.nisovin.shopkeepers.abstractTypes;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.Log;

public abstract class TypeRegistry<T extends AbstractType> {

	protected final Map<String, T> registeredTypes = new HashMap<String, T>();

	// true on success:
	public boolean register(T type) {
		Validate.notNull(type);
		String identifier = type.getIdentifier();
		assert identifier != null && !identifier.isEmpty();
		if (this.registeredTypes.containsKey(identifier)) {
			// this shouldn't happen, as currently we are the only one registering types, and the registry gets cleared on reloads
			Log.warning("Cannot replace previously registered " + this.getTypeName() + " '" + identifier + "' with new one!");
			return false;
		}
		this.registeredTypes.put(identifier, type);
		return true;
	}

	public void registerAll(Collection<T> all) {
		if (all == null) return;
		for (T type : all) {
			if (type != null) this.register(type);
		}
	}

	/**
	 * A name for the type this TypeRegistry is managing.
	 * Used to print slightly more informative debug messages.
	 * Examples: 'shop type', 'shop object type', 'trading window', 'hiring window', etc.
	 * 
	 * @return a name for the type this class is handling
	 */
	protected abstract String getTypeName();

	public Collection<T> getRegisteredTypes() {
		return Collections.unmodifiableCollection(this.registeredTypes.values());
	}

	/**
	 * Gets the number of the different types registered in this class.
	 * 
	 * @return the number of registered types
	 */
	public int numberOfRegisteredTypes() {
		return this.registeredTypes.size();
	}

	public T get(String identifier) {
		return this.registeredTypes.get(identifier);
	}

	public T match(String identifier) {
		if (identifier == null || identifier.isEmpty()) return null;
		// might slightly improve performance of this loop: java /might/ skip 'toLowerCase' calls if the string already is in lower case:
		identifier = identifier.toLowerCase();
		for (T type : this.registeredTypes.values()) {
			if (type.matches(identifier)) {
				return type;
			}
		}
		return null;
	}

	public void clearAll() {
		this.registeredTypes.clear();
	}
}