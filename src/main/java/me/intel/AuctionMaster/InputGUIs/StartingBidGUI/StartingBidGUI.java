package me.intel.AuctionMaster.InputGUIs.StartingBidGUI;

import me.intel.AuctionMaster.InputGUIs.ChatListener;
import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.Menus.CreateAuctionMainMenu;
import me.intel.AuctionMaster.Utils.Utils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static me.intel.AuctionMaster.AuctionMaster.utilsAPI;

public class StartingBidGUI {
    private ItemStack paper;

    public interface StartingBid{
        void openGUI(Player p);
    }

    public static StartingBid selectStartingBid;

    public StartingBidGUI(){
        switch (AuctionMaster.inputType) {
            case "chat":
                selectStartingBid=this::chatTrigger;
                break;
            case "anvil":
                paper = new ItemStack(Material.PAPER);
                ArrayList<String> lore=new ArrayList<>();
                for(String line : AuctionMaster.auctionsManagerCfg.getStringList("starting-bid-sign-message"))
                    lore.add(Utils.chat(line));
                paper= AuctionMaster.itemConstructor.getItem(paper, " ", lore);
                selectStartingBid=this::anvilTrigger;
                break;
            case "sign":
                selectStartingBid=this::signTrigger;
                break;
        }
    }

    private void signTrigger(Player p){
        new StartingBidSignGUI(p);
    }

    private void anvilTrigger(Player p){
        new AnvilGUI.Builder()
                .onClose(stateSnapshot -> {
                })
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    try{
                        double timeInput = AuctionMaster.numberFormatHelper.useDecimals? Double.parseDouble(stateSnapshot.getText()):Math.floor(Double.parseDouble(stateSnapshot.getText()));
                        if(timeInput < 1){
                            p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("starting-bid-sign-deny")));
                        }
                        else
                            AuctionMaster.auctionsHandler.startingBid.put(p.getUniqueId().toString(), timeInput);
                    }catch(Exception x){
                        p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("starting-bid-sign-deny")));
                    }

                    new CreateAuctionMainMenu(p);
                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .text("")
                .plugin(AuctionMaster.plugin)
                .open(p);
    }


    private void chatTrigger(Player p){
        for(String line : AuctionMaster.auctionsManagerCfg.getStringList("starting-bid-sign-message"))
            p.sendMessage(utilsAPI.chat(p, line));
        p.closeInventory();
        new ChatListener(p, (reply) -> {
            try {
                double timeInput = AuctionMaster.numberFormatHelper.useDecimals ? Double.parseDouble(reply) : Math.floor(Double.parseDouble(reply));
                if (timeInput < 1) {
                    p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("starting-bid-sign-deny")));
                } else
                    AuctionMaster.auctionsHandler.startingBid.put(p.getUniqueId().toString(), timeInput);
            } catch (Exception x) {
                p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("starting-bid-sign-deny")));
            }

            new CreateAuctionMainMenu(p);
        });
    }
}
