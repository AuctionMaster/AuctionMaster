package me.intel.AuctionMaster.API.Events;

import me.intel.AuctionMaster.AuctionObjects.Auction;
import me.intel.AuctionMaster.Utils.Utils;
import me.intel.AuctionMaster.AuctionMaster;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BINPurchaseEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private Boolean cancelled = false;
    private final Player player;
    private final Auction auction;
    private final double pricePaid;

    public BINPurchaseEvent(Player player, Auction auction, double pricePaid) {
        this.player = player;
        this.auction=auction;
        this.pricePaid=pricePaid;
        ConfigurationSection auctionc = AuctionMaster.plugin.getConfig().getConfigurationSection("events").getConfigurationSection("Purchase");
        if(auctionc.getBoolean("Enabled")) {
            String message = auctionc.getString("Message");
            Utils.sendWebhook(message.replace("%player%",player.getName()).replace("%item%", (auction.getItemStack().getItemMeta().hasDisplayName() ? ChatColor.stripColor(auction.getItemStack().getItemMeta().getDisplayName()) : auction.getItemStack().getType().name())).replace("%price%", String.valueOf(pricePaid)));
        }
    }

    public Player getPlayer() {
        return this.player;
    }

    public Auction getAuction() {
        return this.auction;
    }

    public double getPricePaid() {
        return this.pricePaid;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
