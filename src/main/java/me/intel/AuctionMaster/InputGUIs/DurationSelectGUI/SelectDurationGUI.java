package me.intel.AuctionMaster.InputGUIs.DurationSelectGUI;

import me.intel.AuctionMaster.InputGUIs.ChatListener;
import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.Menus.CreateAuctionMainMenu;
import me.intel.AuctionMaster.Utils.Utils;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static me.intel.AuctionMaster.AuctionMaster.utilsAPI;

public class SelectDurationGUI {
    private ItemStack paper;

    public interface SelectDuration {
        void openGUI(Player p, int maximum_hours, boolean minutes);
    }

    public static SelectDuration selectDuration;

    public SelectDurationGUI() {
        switch (AuctionMaster.inputType) {
            case "chat":
                selectDuration=this::chatTrigger;
                break;
            case "anvil":
                paper = new ItemStack(Material.PAPER);
                ArrayList<String> lore=new ArrayList<>();
                for(String line : AuctionMaster.auctionsManagerCfg.getStringList("duration-sign-message"))
                    lore.add(Utils.chat(line));
                paper= AuctionMaster.itemConstructor.getItem(paper, " ", lore);
                selectDuration=this::anvilTrigger;
                break;
            case "sign":
                selectDuration=this::signTrigger;
                break;
        }
    }

    private void signTrigger(Player p, int maximum_hours, boolean minutes){
        new SelectDurationSignGUI(p, maximum_hours, minutes);
    }

    private void anvilTrigger(Player p, int maximum_hours, boolean minutes){
        ItemStack paperClone = paper.clone();
        ItemMeta meta = paperClone.getItemMeta();
        ArrayList<String> lore = new ArrayList<>();
        for(String line : meta.getLore())
            lore.add(line.replace("%time-format%", minutes ? AuctionMaster.configLoad.minutes : AuctionMaster.configLoad.hours));
        meta.setLore(lore);
        paperClone.setItemMeta(meta);

        new AnvilGUI.Builder()
                .onClose(stateSnapshot -> {
                })
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    try{
                        int timeInput = Integer.parseInt(stateSnapshot.getText());
                        if(minutes){
                            if(timeInput > 59 || timeInput < 1){
                                p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("duration-sign-deny")));
                            }
                            else {
                                AuctionMaster.auctionsHandler.startingDuration.put(p.getUniqueId().toString(), timeInput * 60000);
                            }
                        }
                        else{
                            if(timeInput > 168 || timeInput < 1){
                                p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("duration-sign-deny")));
                            }
                            else{
                                if(maximum_hours != -1 && maximum_hours < timeInput)
                                    p.sendMessage(utilsAPI.chat(p, AuctionMaster.plugin.getConfig().getString("duration-limit-reached-message")));
                                else
                                    AuctionMaster.auctionsHandler.startingDuration.put(p.getUniqueId().toString(), timeInput * 3600000);
                            }
                        }
                    }catch(Exception x){
                        p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("duration-sign-deny")));
                    }

                    new CreateAuctionMainMenu(p);
                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                })
                .text("")
                .plugin(AuctionMaster.plugin)
                .open(p);
    }


    private void chatTrigger(Player p, int maximum_hours, boolean minutes){
        for(String line : AuctionMaster.auctionsManagerCfg.getStringList("duration-sign-message"))
            p.sendMessage(Utils.chat(line.replace("%time-format%", minutes? AuctionMaster.configLoad.minutes: AuctionMaster.configLoad.hours)));
        p.closeInventory();
        new ChatListener(p, (reply) ->{
            try{
                int timeInput = Integer.parseInt(reply);
                if(minutes){
                    if(timeInput>59 || timeInput<1){
                        p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("duration-sign-deny")));
                    }
                    else {
                        AuctionMaster.auctionsHandler.startingDuration.put(p.getUniqueId().toString(), timeInput*60000);
                    }
                }
                else{
                    if(timeInput>168 || timeInput<1){
                        p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("duration-sign-deny")));
                    }
                    else{
                        if(maximum_hours!=-1 && maximum_hours<timeInput)
                            p.sendMessage(utilsAPI.chat(p, AuctionMaster.plugin.getConfig().getString("duration-limit-reached-message")));
                        else
                            AuctionMaster.auctionsHandler.startingDuration.put(p.getUniqueId().toString(), timeInput*3600000);
                    }
                }
            }catch(Exception x){
                p.sendMessage(utilsAPI.chat(p, AuctionMaster.auctionsManagerCfg.getString("duration-sign-deny")));
            }

            new CreateAuctionMainMenu(p);
        });
    }

}
