package com.nisovin.shopkeepers;

import java.util.logging.Logger;

public class Log {

	public static Logger getLogger() {
		return ShopkeepersPlugin.getInstance().getLogger();
	}

	public static void info(String message) {
		if (message == null || message.isEmpty()) return;
		getLogger().info(message);
	}

	public static void debug(String message) {
		if (Settings.debug) {
			info(message);
		}
	}

	public static void warning(String message) {
		getLogger().warning(message);
	}

	public static void severe(String message) {
		getLogger().severe(message);
	}
}
