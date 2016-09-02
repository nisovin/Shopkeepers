package com.nisovin.shopkeepers;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * A test event that is called to check if a player can damage the given entity (ex. when the player tries to hire a
 * villager).
 */
public class TestEntityDamageByEntityEvent extends EntityDamageByEntityEvent {

	public TestEntityDamageByEntityEvent(Entity damager, Entity damagee) {
		super(damager, damagee, DamageCause.CUSTOM, 1.0D);
	}
}
