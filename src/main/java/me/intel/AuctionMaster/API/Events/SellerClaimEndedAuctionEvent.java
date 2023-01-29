package me.intel.AuctionMaster.API.Events;

import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.AuctionObjects.Auction;
import me.intel.AuctionMaster.Utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SellerClaimEndedAuctionEvent extends Event implements Cancellable  {
    private static final HandlerList handlers = new HandlerList();
    private Boolean cancelled = false;
    private final Player player;
    private final Auction auction;
    private final double toClaim;

    public SellerClaimEndedAuctionEvent(Player player, Auction auction, double toClaim) {
        this.player = player;
        this.auction=auction;
        this.toClaim=toClaim;
        ConfigurationSection auctionc = AuctionMaster.plugin.getConfig().getConfigurationSection("events").getConfigurationSection("ClaimEnded");
        if(auctionc.getBoolean("Enabled")) {
            String message = auctionc.getString("Message");
            Utils.sendWebhook(message.replace("%player%",player.getName()).replace("%item%", (auction.getItemStack().getItemMeta().hasDisplayName() ? ChatColor.stripColor(auction.getItemStack().getItemMeta().getDisplayName()) : auction.getItemStack().getType().name())).replace("%price%", String.valueOf(auction.getCoins())));
        }
    }

    public Player getPlayer() {
        return this.player;
    }

    public Auction getAuction() {
        return this.auction;
    }

    public double getToClaim() {
        return this.toClaim;
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
