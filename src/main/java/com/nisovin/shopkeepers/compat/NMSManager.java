package com.nisovin.shopkeepers.compat;

import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;

public final class NMSManager {
    private static NMSCallProvider provider;

    public static NMSCallProvider getProvider() {
        return NMSManager.provider;
    }

    public static void load(Plugin plugin) {
        final String packageName = plugin.getServer().getClass().getPackage().getName();
        String cbversion = packageName.substring(packageName.lastIndexOf('.') + 1);
        try {
            final Class<?> clazz = Class.forName("com.nisovin.shopkeepers.compat." + cbversion + ".NMSHandler");
            if (NMSCallProvider.class.isAssignableFrom(clazz)) {
                NMSManager.provider = (NMSCallProvider) clazz.getConstructor().newInstance();
            } else {
                throw new Exception("Nope");
            }
        } catch (final Exception e) {
            plugin.getLogger().severe("Potentially incompatible server version: Shopkeepers is running in 'compatibility mode'.");
            plugin.getLogger().info("Check for updates at http://dev.bukkit.org/bukkit-plugins/shopkeepers/");
            
            try {
                NMSManager.provider = new FailedHandler();
            } catch (Exception e_u) {}
        }
    }
}