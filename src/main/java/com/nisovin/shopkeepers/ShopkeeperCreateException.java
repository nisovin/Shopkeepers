package com.nisovin.shopkeepers;

/**
 * This exception gets used during shopkeeper creation and loading, if the shopkeeper cannot be created due to invalid
 * or missing data.
 */
public class ShopkeeperCreateException extends Exception {

	private static final long serialVersionUID = -2026963951805397944L;

	public ShopkeeperCreateException(String message) {
		super(message);
	}
}
