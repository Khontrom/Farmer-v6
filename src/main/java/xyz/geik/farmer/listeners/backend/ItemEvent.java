package xyz.geik.farmer.listeners.backend;

import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.geik.farmer.Main;
import xyz.geik.farmer.api.handlers.FarmerItemCollectEvent;
import xyz.geik.farmer.api.handlers.FarmerStorageFullEvent;
import xyz.geik.farmer.helpers.Settings;
import xyz.geik.farmer.model.Farmer;
import xyz.geik.farmer.model.inventory.FarmerInv;

/**
 * Main event of farmer which collect items to storage
 */
public class ItemEvent implements Listener {

    /**
     * Has Item in farmer
     * Item don't have meta
     * Checks player drop
     * Checks World
     * Checks has farmer on location
     * Checks if farmer closed
     */
    @EventHandler(priority= EventPriority.HIGHEST)
    public void itemSpawnEvent(@NotNull ItemSpawnEvent e) {
        // Checks world suitable for farmer
        if (!Settings.allowedWorlds.contains(e.getLocation().getWorld().getName()))
            return;
        // Checks if player dropped or naturally dropped
        // if settings contain Cancel player drop then it cancel collecting it.
        if (Settings.ignorePlayerDrop && e.getEntity().getPickupDelay() >= 39)
            return;

        // Cancel if item has meta because there can be unique items
        // which used for something else, and it would turn to basic item if farmer collects.
        ItemStack item = new ItemStack(e.getEntity().getItemStack());
        if (item.hasItemMeta())
            return;

        // Checks farmer contain that item also supports old version and newer versions.
        if (!FarmerInv.checkMaterial(item))
            return;

        // Checks item dropped in region of a player
        // And checks region owner has a farmer
        String regionID = Main.getIntegration().getRegionID(e.getLocation());
        if (regionID == null || !Main.getFarmers().containsKey(regionID))
            return;

        // Checks farmer in collection state
        Farmer farmer = Main.getFarmers().get(regionID);
        if (farmer.getState() == 0)
            return;

        long left = -1;

        // Calls FarmerItemCollectEvent
        FarmerItemCollectEvent collectEvent = new FarmerItemCollectEvent(farmer, item);
        Bukkit.getPluginManager().callEvent(collectEvent);
        // Checks if event is cancelled
        if (!collectEvent.isCancelled()) {
            // Summing item amount to the farmer if stock is not full
            // And catch the left amount
            left = farmer.getInv().sumItemAmount(XMaterial.matchXMaterial(item), item.getAmount());
            // If left amount is not 0 then it means stock is full
            if (left != 0) {
                // Calls FarmerStorageFullEvent
                FarmerStorageFullEvent storageFullEvent = new FarmerStorageFullEvent(farmer, item, (int) left);
                Bukkit.getPluginManager().callEvent(storageFullEvent);
                // Checks if FarmerStorageFullEvent is not cancelled
                // And if drop item is false then it will force sum the item
                // To the stock
                if (!storageFullEvent.isCancelled() && !storageFullEvent.isDropItem()) {
                    farmer.getInv().forceSumItem(XMaterial.matchXMaterial(item), left);
                    return;
                }
                // Execute only on drop item is true
                e.getEntity().getItemStack().setAmount((int) left);
            }
            else e.setCancelled(true);
        }
    }
}