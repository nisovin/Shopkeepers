package com.nisovin.shopkeepers.abstractTypes;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.Log;

public abstract class TypeRegistry<T extends AbstractType> {

	protected final Map<String, T> registeredTypes = new LinkedHashMap<String, T>();

	public void register(T type) {
		Validate.notNull(type);
		String identifier = type.getIdentifier();
		assert identifier != null && !identifier.isEmpty();
		if (this.registeredTypes.containsKey(identifier) && this.isDefaultTypeIdentifier(identifier)) {
			// no replacing of default types:
			Log.debug("Failed to register " + this.getTypeName() + " '" + identifier + "': this identifier is already registered and represents a default " + this.getTypeName() + "!");
			return;
		}
		T oldType = this.registeredTypes.put(identifier, type);
		if (oldType != null) {
			Log.debug("Replaced previously registered " + this.getTypeName() + " '" + identifier + "' with new one.");
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

	/**
	 * Checks whether or not the given identifier represents a default type.
	 * Default types are blocked from being overwritten.
	 * 
	 * @param identifier
	 *            a type identifier
	 * @return true, if the given identifier represents a default type
	 */
	protected abstract boolean isDefaultTypeIdentifier(String identifier);

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
}