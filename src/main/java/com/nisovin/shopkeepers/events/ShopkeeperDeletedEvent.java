package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.Shopkeeper;

public class ShopkeeperDeletedEvent extends Event {

	private Player player;
	private Shopkeeper shopkeeper;

	public ShopkeeperDeletedEvent(Player player, Shopkeeper shopkeeper) {
		this.player = player;
		this.shopkeeper = shopkeeper;
	}

	public Player getPlayer() {
		return player;
	}

	public Shopkeeper getShopkeeper() {
		return shopkeeper;
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}