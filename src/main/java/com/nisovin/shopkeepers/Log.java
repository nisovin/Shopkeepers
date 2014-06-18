package com.nisovin.shopkeepers;

import java.util.logging.Logger;

public class Log {

	private static boolean debug = false;

	public static void setDebug(boolean debug) {
		Log.debug = debug;
	}

	public static boolean isDebug() {
		return debug;
	}

	public static Logger getLogger() {
		return ShopkeepersPlugin.getInstance().getLogger();
	}

	public static void info(String message) {
		if (message == null || message.isEmpty()) return;
		Log.getLogger().info(message);
	}

	public static void debug(String message) {
		if (debug) {
			info(message);
		}
	}

	public static void warning(String message) {
		Log.getLogger().warning(message);
	}

	public static void severe(String message) {
		Log.getLogger().severe(message);
	}
}