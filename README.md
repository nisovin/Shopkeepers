Shopkeepers
===========

Shopkeepers is a Bukkit plugin which allows you to set up custom villager shopkeepers that sell exactly what you want them to sell and for what price. 
You can set up admin shops, which have infinite supply, and you can also set up player shops, which pull supply from a chest.

BukkitDev Page: http://dev.bukkit.org/bukkit-plugins/shopkeepers/  
Forums: http://nisovin.com/forums/index.php?forums/shopkeepers/

Guidelines
----------

All "volatile" code (any code that relies on CraftBukkit and will break with version updates) should be in the volatilecode package.
Please keep this code to a minimum wherever possible, as adding more volatile code makes the updating process more difficult.
If it is possible to create a non-volatile fallback method, please do so and put it in the FailedHandler class.

For any major feature changes, please make a forum thread about it so it can be discussed before you commit the code to the main repository.

Todo
----
* Remove dependence on item ids and other "magic numbers"
* Add support for item attributes (I'd prefer to wait for a Bukkit API for this, but who knows when that will happen)
* Improve chest protection (the anti-hopper code is inefficient)
* Possibly change the way the different "shop object" types are handled to make adding new ones easier
* Add MySQL support (maybe someday, certainly not urgent)
