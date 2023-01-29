package me.intel.AuctionMaster.Menus;

import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.Utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class DeliveryPlayerMenu {

    private Inventory inventory;
    private Player player;
    private final ClickListen listener = new ClickListen();
    private boolean fromMainMenu;
    private ArrayList<ItemStack> deliveryItems;
    private double deliveryCoins;

    public DeliveryPlayerMenu(Player player, boolean fromMainMenu){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            this.player = player;
            this.fromMainMenu = fromMainMenu;
            this.deliveryCoins = AuctionMaster.deliveries.getCoins(player.getUniqueId().toString());
            this.deliveryItems = AuctionMaster.deliveries.getDeliveryItems(player.getUniqueId().toString());
            inventory = Bukkit.createInventory(player, InventoryType.DISPENSER, AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.deliveryMenuName));

            if (AuctionMaster.configLoad.useBackgoundGlass)
                for (int i = 0; i < 9; i++)
                    inventory.setItem(i, AuctionMaster.configLoad.backgroundGlass.clone());

            ArrayList<String> lore = new ArrayList<>();
            if (deliveryItems.isEmpty() && deliveryCoins == 0) {
                for (String line : AuctionMaster.configLoad.deliveryItemNoLore)
                    lore.add(AuctionMaster.utilsAPI.chat(player, line));
                inventory.setItem(4, AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.mainMenuDeliveryItem, AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.deliveryItemNoName), lore));
            } else {
                for (String line : AuctionMaster.configLoad.deliveryItemYesLore)
                    lore.add(AuctionMaster.utilsAPI.chat(player, line
                            .replace("%coins%", AuctionMaster.numberFormatHelper.formatNumber(deliveryCoins))
                            .replace("%items-count%", String.valueOf(deliveryItems.size()))
                    ));
                inventory.setItem(4, AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.mainMenuDeliveryItem, AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.deliveryItemYesName), lore));
            }

            if (fromMainMenu) {
                lore = new ArrayList<>();
                for (String line : AuctionMaster.configLoad.goBackLore)
                    lore.add(AuctionMaster.utilsAPI.chat(player, line));
                inventory.setItem(7, AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.goBackMaterial, AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.goBackName), lore));
            }

            Bukkit.getScheduler().runTask(AuctionMaster.plugin, () -> {
                Bukkit.getPluginManager().registerEvents(listener, AuctionMaster.plugin);
                player.openInventory(inventory);
            });
        });
    }

    public class ClickListen implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent e){
            if(e.getInventory().equals(inventory)){
                e.setCancelled(true);
                if(e.getCurrentItem()==null || e.getCurrentItem().getType().equals(Material.AIR)) {
                    return;
                }
                if(e.getClickedInventory().equals(inventory)) {
                    if (e.getSlot() == 7) {
                        if (fromMainMenu)
                            new MainAuctionMenu(player);
                    }
                    else if(e.getSlot()==4){
                        if(deliveryItems.isEmpty() && deliveryCoins==0){
                            Utils.playSound(player, "ah-no-delivery");
                            player.sendMessage(AuctionMaster.utilsAPI.chat(player, AuctionMaster.plugin.getConfig().getString("no-delivery-to-claim")));
                            return;
                        }
                        try {
                            AuctionMaster.economy.addMoney(player, deliveryCoins);
                            ArrayList<ItemStack> itemStacks = new ArrayList<>();
                            for (ItemStack itemStack : deliveryItems) {
                                if (Utils.getEmptySlots(player) == 0) {
                                    itemStacks.add(itemStack);
                                } else
                                    player.getInventory().addItem(itemStack);
                            }
                            if (!itemStacks.isEmpty()) {
                                player.sendMessage(AuctionMaster.utilsAPI.chat(player, AuctionMaster.plugin.getConfig().getString("not-enough-space-delivery")));
                                AuctionMaster.deliveries.setCoinsAndItems(player.getUniqueId().toString(), itemStacks, 0);
                            } else
                                AuctionMaster.deliveries.removeDelivery(player.getUniqueId().toString());
                            Utils.playSound(player, "ah-delivery-claimed");
                            player.sendMessage(AuctionMaster.utilsAPI.chat(player, AuctionMaster.plugin.getConfig().getString("delivery-claimed-message")));
                        }catch(Exception x){
                            x.printStackTrace();
                            AuctionMaster.deliveries.removeDelivery(player.getUniqueId().toString());
                        }
                        player.closeInventory();
                    }
                }
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent e){
            if(inventory.equals(e.getInventory())) {
                HandlerList.unregisterAll(this);
                inventory = null;
                player = null;
            }
        }
    }

}
