package me.intel.AuctionMaster.InputGUIs.DeliveryGUI;

import me.intel.AuctionMaster.Menus.AdminMenus.DeliveryAdminMenu;
import me.intel.AuctionMaster.Utils.Utils;
import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.InputGUIs.ChatListener;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class DeliveryGUI {
    private ItemStack paper;

    public interface DeliveryInstance {
        void openGUI(Player p);
    }

    public static DeliveryInstance deliveryInstance;

    public DeliveryGUI(){
        if(AuctionMaster.plugin.getConfig().getBoolean("use-chat-instead-sign")){
            deliveryInstance =this::chatTrigger;
        }
        else if(AuctionMaster.plugin.getConfig().getBoolean("use-anvil-instead-sign") || !AuctionMaster.hasProtocolLib){
            paper = new ItemStack(Material.PAPER);
            ArrayList<String> lore=new ArrayList<>();
            lore.add(Utils.chat("&7^^^^^^^^^^^^^^^"));
            lore.add(Utils.chat("&fPlease enter the player's"));
            lore.add(Utils.chat("&fname whose deliveries you"));
            lore.add(Utils.chat("&fwant to manage."));
            paper= AuctionMaster.itemConstructor.getItem(paper, " ", lore);
            deliveryInstance =this::anvilTrigger;
        }
        else{
            deliveryInstance =this::signTrigger;
        }
    }

    private void signTrigger(Player p){
        new DeliverySignGUI(p);
    }

    private void anvilTrigger(Player p){
        new AnvilGUI.Builder()
                .onClose(stateSnapshot -> {
                })
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    try {
                        new DeliveryAdminMenu(p, stateSnapshot.getText().replace(" ", "").equals("") ? null : stateSnapshot.getText());
                    } catch (Exception ignored) {}

                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .text("")
                .plugin(AuctionMaster.plugin)
                .open(p);
    }


    private void chatTrigger(Player p){
        p.sendMessage(Utils.chat("&7&m----------------"));
        p.sendMessage(Utils.chat("&fPlease enter the player's"));
        p.sendMessage(Utils.chat("&fname whose deliveries you"));
        p.sendMessage(Utils.chat("&fwant to manage."));
        p.closeInventory();
        new ChatListener(p, (reply) -> new DeliveryAdminMenu(p, reply.replace(" ", "").equals("")?null:reply));
    }

}
