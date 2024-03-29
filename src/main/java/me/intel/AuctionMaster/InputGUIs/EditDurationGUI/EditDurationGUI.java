package me.intel.AuctionMaster.InputGUIs.EditDurationGUI;

import me.intel.AuctionMaster.AuctionObjects.Auction;
import me.intel.AuctionMaster.InputGUIs.ChatListener;
import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.Menus.AdminMenus.ViewAuctionAdminMenu;
import me.intel.AuctionMaster.Utils.Utils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static me.intel.AuctionMaster.AuctionMaster.utilsAPI;

public class EditDurationGUI {
    private ItemStack paper;

    public interface EditDuration{
        void openGUI(Player p, Auction auction, String goBackTo, boolean rightClick);
    }

    public static EditDuration editDuration;

    public EditDurationGUI(){
        switch (AuctionMaster.inputType) {
            case "chat":
                editDuration=this::chatTrigger;
                break;
            case "anvil":
                paper = new ItemStack(Material.PAPER);
                ArrayList<String> lore=new ArrayList<>();
                lore.add(Utils.chat("&fEnter minutes"));
                lore.add(Utils.chat("&fExamples: 20"));
                lore.add(Utils.chat("&for -20 to speed"));
                paper= AuctionMaster.itemConstructor.getItem(paper, " ", lore);
                editDuration =this::anvilTrigger;
                break;
            case "sign":
                editDuration=this::signTrigger;
                break;
        }
    }

    private void signTrigger(Player p, Auction auction, String goBackTo, boolean rightClick){
        new EditDurationSignGUI(p, auction, goBackTo, rightClick);
    }

    private void anvilTrigger(Player p, Auction auction, String goBackTo, boolean rightClick){
        new net.wesjd.anvilgui.AnvilGUI.Builder()
                .onClose(stateSnapshot -> {
                })
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    try{
                        int timeInput = Integer.parseInt(stateSnapshot.getText());
                        if(rightClick)
                            auction.addMinutesToAuction(timeInput);
                        else
                            auction.setEndingDate(ZonedDateTime.now().toInstant().toEpochMilli()+timeInput*60000);
                    }catch(Exception x){
                        p.sendMessage(utilsAPI.chat(p, "&cInvalid number."));
                    }
                    new ViewAuctionAdminMenu(p, auction, goBackTo);
                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .text("")
                .plugin(AuctionMaster.plugin)
                .open(p);
    }


    private void chatTrigger(Player p, Auction auction, String goBackTo, boolean rightClick){
        p.sendMessage(utilsAPI.chat(p, "Enter minutes"));
        p.sendMessage(utilsAPI.chat(p, "Examples: 20"));
        p.sendMessage(utilsAPI.chat(p, "or -20 to speed"));
        p.closeInventory();
        new ChatListener(p, (reply) -> {
            try{
                int timeInput = Integer.parseInt(reply);
                if(rightClick)
                    auction.addMinutesToAuction(timeInput);
                else
                    auction.setEndingDate(ZonedDateTime.now().toInstant().toEpochMilli()+timeInput*60000);
            }catch(Exception x){
                p.sendMessage(utilsAPI.chat(p, "&cInvalid number."));
            }
            new ViewAuctionAdminMenu(p, auction, goBackTo);
        });
    }
}
