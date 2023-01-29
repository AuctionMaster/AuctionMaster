package me.intel.AuctionMaster.API.Events;

import me.intel.AuctionMaster.AuctionObjects.Auction;
import me.intel.AuctionMaster.Utils.Utils;
import me.intel.AuctionMaster.AuctionMaster;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuctionDeleteEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Auction auction;

    public AuctionDeleteEvent(Player player, Auction auction) {
        this.player = player;
        this.auction = auction;
        ConfigurationSection auctionc = AuctionMaster.plugin.getConfig().getConfigurationSection("events").getConfigurationSection("AuctionDelete");
        if(auctionc.getBoolean("Enabled")) {
            String message = auctionc.getString("Message");
            Utils.sendWebhook(message.replace("%player%",player.getName()).replace("%item%", (auction.getItemStack().getItemMeta().hasDisplayName() ? ChatColor.stripColor(auction.getItemStack().getItemMeta().getDisplayName()) : auction.getItemStack().getType().name())));
        }
    }

    public Player getPlayer() {
        return this.player;
    }

    public Auction getAuction() {
        return this.auction;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
