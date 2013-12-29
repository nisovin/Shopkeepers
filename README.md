Shopkeepers
===========

Shopkeepers is a Bukkit plugin which allows you to set up custom villager shopkeepers that sell exactly what you want them to sell and for what price. 
You can set up admin shops, which have infinite supply, and you can also set up player shops, which pull supply from a chest.

BukkitDev Page: http://dev.bukkit.org/bukkit-plugins/shopkeepers/  
Forums: http://nisovin.com/forums/index.php?forums/shopkeepers/  
Dev builds: http://ci.cube-nation.de/job/Shopkeepers/

Guidelines
----------

All "volatile" code (any code that relies on CraftBukkit and will break with version updates) should be in the volatilecode package.
Please keep this code to a minimum wherever possible, as adding more volatile code makes the updating process more difficult.
If it is possible to create a non-volatile fallback method, please do so and put it in the FailedHandler class.

For any major feature changes, please make a forum thread about it and/or create an extra branch so it can be discussed before you commit the code to the main repository.

Build with Maven
----------------

This is the recommended and easy way to compile the plugin yourself and/or help to contribute to it.
Just check out the project to your machine and import it in Eclipse with **Import > Maven > Existing Maven Project**.
Then just right click the imported project and select **Run As > Maven install**.

Build without Maven
-------------------

If you really want to do it the old school way, you're free to import the project in Eclipse with **Import > General > Existing Project into Workspace**. You'll find that the project will instantly show some errors because it's missing its' dependencies. You also need to make sure that you'll include the provided modules (NMSHandlers).

**Here's how you do that:**
* after importing the project right click on it and select **Properties**
* under **Java Build Path > Source** click on **Add Folder...** and add all provided modules:
  * modules/v1_6_R3/src/main/java
  * modules/v1_7_R1/src/main/java
  * ...
* under **Java Build Path > Library** click on **Add External JARs...** and add the needed CraftBukkit.jar files for the modules above<br>
  (you can download them from http://dl.bukkit.org/downloads/craftbukkit)
* after that you can create the plugin for example by using Rightclick and selecting **Export > Java > JAR file** or another recommened way


Todo
----
* Consider replacing the trade interface with a custom one made from a regular inventory. It wouldn't look as nice, but it would potentially allow better functionality. It would be a config option.
* Look into whether there's a way to fix the InventoryClickEvent cancellation bug that was introduced in 1.7.2.
* Improve chest protection (the anti-hopper code is inefficient).
* Possibly change the way the different "shop object" types are handled to make adding new ones easier.
* Add MySQL support (maybe someday, certainly not urgent).
* Move schema.txt into resources folder (?).
* Fix (Admin) sign shopkeepers not checking during creation, if there actually is a sign targeted.

Ideas (comments please)
----
* Per-Trade/Shopkeeper settings, maybe via written books:
  -> by adding another row to the shopkeeper-editor inventory window each trade option and shopkeeper could have a slot for a written-book
  -> which could contain additional meta-data, per-trade/shopkeeper settings, like for example:
  -> "ignore damage/durability" (useful for repair-shops)
  -> "ignore name/lore/etc" (useful when players want to buy a certain item with their shop without being interested in concrete name or other metadata
  -> There should maybe be improved feedback messages then (this should be there probably anyways), to inform players why their trade failed, instead of only cancle it
* Maybe ignore cancellation of the PlayerInteractEntityEvent when clicking shopkeeper entities to open the shops?
  -> So that shops can also be traded with, even if they are located in some sort of "protected" region of another plugin.
  -> Maybe optional via a setting, so that server owners can activate this, if they have a land protection plugin which gives them no option to allow entity/villager interaction for everyone, not only "region members/owners"
