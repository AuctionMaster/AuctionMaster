package me.intel.AuctionMaster.InputGUIs.SearchGUI;

import me.intel.AuctionMaster.Utils.Utils;
import me.intel.AuctionMaster.InputGUIs.ChatListener;
import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.Menus.BrowsingAuctionsMenu;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class SearchGUI {
    private ItemStack paper;

    public interface SearchFor{
        void openGUI(Player p, String category);
    }

    public static SearchFor searchFor;

    public SearchGUI(){
        switch (AuctionMaster.inputType) {
            case "chat":
                searchFor=this::chatTrigger;
                break;
            case "anvil":
                paper = new ItemStack(Material.PAPER);
                ArrayList<String> lore=new ArrayList<>();
                for(String line : AuctionMaster.auctionsManagerCfg.getStringList("search-sign-message"))
                    lore.add(Utils.chat(line));
                paper= AuctionMaster.itemConstructor.getItem(paper, " ", lore);
                searchFor=this::anvilTrigger;
                break;
            case "sign":
                searchFor=this::signTrigger;
                break;
        }
    }

    private void signTrigger(Player p, String category){
        new SearchSignGUI(p, category);
    }

    private void anvilTrigger(Player p, String category){
        new net.wesjd.anvilgui.AnvilGUI.Builder()
                .onClose(stateSnapshot -> {
                })
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    if (stateSnapshot.getText().equals("")) {
                        new BrowsingAuctionsMenu(p, category, 0, null);
                    } else {
                        new BrowsingAuctionsMenu(p, category, 0, stateSnapshot.getText());
                    }
                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .text("")
                .plugin(AuctionMaster.plugin)
                .open(p);
    }


    private void chatTrigger(Player p, String category){
        for(String line : AuctionMaster.auctionsManagerCfg.getStringList("search-sign-message"))
            p.sendMessage(Utils.chat(line));
        p.closeInventory();
        new ChatListener(p, (reply) -> {
            new BrowsingAuctionsMenu(p, category, 0, reply.equals("")?null:reply);
        });
    }

}
