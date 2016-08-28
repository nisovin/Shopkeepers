package com.nisovin.shopkeepers;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * A test event that is called when we check if something can be interacted with (Chests)
 */
public class TestEntityDamageByEntityEvent extends EntityDamageByEntityEvent {
	public TestEntityDamageByEntityEvent(Entity damager, Entity damagee) {
		super(damager, damagee, DamageCause.CUSTOM, 1);
	}
}
