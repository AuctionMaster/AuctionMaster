package me.intel.AuctionMaster.PlaceholderAPISupport;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlaceholderAPISupportNo implements PlaceholderAPISupport {

    public String chat (Player p, String msg){
        if(msg==null)
            return ChatColor.translateAlternateColorCodes('&', "&cConfig Missing Text");
        else if (!Pattern.compile("\\{#[0-9A-Fa-f]{6}}").matcher(msg).find()) {
            return ChatColor.translateAlternateColorCodes('&', msg);
        } else {
            Matcher m = Pattern.compile("\\{#[0-9A-Fa-f]{6}}").matcher(msg);
            String s;
            String sNew;
            while (m.find()) {
                s = m.group();
                sNew = "§x" + Arrays.stream(s.split("")).map((s2) -> "§" + s2).collect(Collectors.joining()).replace("§#", "");
                msg = msg.replace(s, sNew.replace("§{", "").replace("§}", ""));
            }
            return ChatColor.translateAlternateColorCodes('&', msg);
        }
    }

}
