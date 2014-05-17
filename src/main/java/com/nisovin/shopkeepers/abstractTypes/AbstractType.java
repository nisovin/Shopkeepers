package com.nisovin.shopkeepers.abstractTypes;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;

public abstract class AbstractType {

	/**
	 * An identifier for this type object.
	 * This could for example be used inside save/configuration files.
	 * So it should not contain any characters which could cause problems with that.
	 */
	protected final String identifier;
	/**
	 * The permission a player needs in order to use/access this type in some way.
	 * Can be null or empty to indicate that no permission is needed.
	 */
	protected final String permission;

	protected AbstractType(String identifier, String permission) {
		Validate.notEmpty(identifier);
		this.identifier = identifier;
		this.permission = permission;
	}

	public final String getIdentifier() {
		return this.identifier;
	}

	public final boolean hasPermission(Player player) {
		return this.permission == null || this.permission.isEmpty() || player.hasPermission(this.permission);
	}

	public boolean isEnabled() {
		return true;
	}

	/**
	 * Checks the given (possibly inaccurate) identifier matches to this type.
	 * 
	 * @param identifier
	 *            an (possible inaccurate) identifier
	 * @return true, if the given identifier is considered to represent this type
	 */
	public boolean matches(String identifier) {
		return this.identifier.equalsIgnoreCase(identifier);
	}
}