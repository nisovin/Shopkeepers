package com.nisovin.shopkeepers;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * A test event that is called to check that villagers can be removed/hired
 */
public class TestEntityDamageByEntityEvent extends EntityDamageByEntityEvent {

	public TestEntityDamageByEntityEvent(Entity damager, Entity damagee) {
		super(damager, damagee, DamageCause.CUSTOM, 1);
	}
}
