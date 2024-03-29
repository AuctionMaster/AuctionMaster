package me.intel.AuctionMaster.InputGUIs.BidSelectGUI;

import me.intel.AuctionMaster.AuctionObjects.Auction;
import me.intel.AuctionMaster.InputGUIs.ChatListener;
import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.Menus.ViewAuctionMenu;
import me.intel.AuctionMaster.Utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.wesjd.anvilgui.AnvilGUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static me.intel.AuctionMaster.AuctionMaster.utilsAPI;

public class BidSelectGUI {
    private ItemStack paper;

    public interface UpdatedBid{
        void openGUI(Player p, Auction auction, String goBackTo, double minimumBid);
    }

    public static UpdatedBid selectUpdateBid;

    public BidSelectGUI(){
        switch (AuctionMaster.inputType) {
            case "chat":
                selectUpdateBid=this::chatTrigger;
                break;
            case "anvil":
                paper = new ItemStack(Material.PAPER);
                ArrayList<String> lore=new ArrayList<>();
                for(String line : AuctionMaster.auctionsManagerCfg.getStringList("starting-bid-sign-message"))
                    lore.add(Utils.chat(line));
                paper= AuctionMaster.itemConstructor.getItem(paper, " ", lore);
                selectUpdateBid=this::anvilTrigger;
                break;
            case "sign":
                selectUpdateBid=this::signTrigger;
                break;
        }
    }

    private void signTrigger(Player p, Auction auction, String goBackTo, double minimumBid){
        new BidSelectSignGUI(p, auction, goBackTo, minimumBid);
    }

    private void anvilTrigger(Player p, Auction auction, String goBackTo, double minimumBid){
        new AnvilGUI.Builder()
                .onClose(stateSnapshot -> {
                })
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    try{
                        double bidSelect = AuctionMaster.numberFormatHelper.useDecimals ? Double.parseDouble(stateSnapshot.getText()) : Math.floor(Double.parseDouble(stateSnapshot.getText()));
                        if(bidSelect >= minimumBid)
                            new ViewAuctionMenu(stateSnapshot.getPlayer(), auction, goBackTo, bidSelect);
                        else
                            new ViewAuctionMenu(stateSnapshot.getPlayer(), auction, goBackTo, 0);
                    }catch(Exception x){
                        stateSnapshot.getPlayer().sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("edit-bid-deny-message")));
                        new ViewAuctionMenu(stateSnapshot.getPlayer(), auction, goBackTo, 0);
                    }
                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .text("")
                .plugin(AuctionMaster.plugin)
                .open(p);
    }


    private void chatTrigger(Player p, Auction auction, String goBackTo, double minimumBid){
        for(String line : AuctionMaster.auctionsManagerCfg.getStringList("starting-bid-sign-message"))
            p.sendMessage(utilsAPI.chat(p, line));
        p.closeInventory();

        new ChatListener(p, (reply) -> {
            try{
                double bidSelect = AuctionMaster.numberFormatHelper.useDecimals? Double.parseDouble(reply):Math.floor(Double.parseDouble(reply));
                if(bidSelect>=minimumBid)
                    new ViewAuctionMenu(p, auction, goBackTo, bidSelect);
                else
                    new ViewAuctionMenu(p, auction, goBackTo, 0);
            }catch(Exception x){
                p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("edit-bid-deny-message")));
                new ViewAuctionMenu(p, auction, goBackTo, 0);
            }

        });
    }
}
