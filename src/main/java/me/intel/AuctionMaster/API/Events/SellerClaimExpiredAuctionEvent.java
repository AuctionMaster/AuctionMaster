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
import org.bukkit.inventory.ItemStack;

public class SellerClaimExpiredAuctionEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private Boolean cancelled = false;
    private final Player player;
    private final Auction auction;
    private final ItemStack item;

    public SellerClaimExpiredAuctionEvent(Player player, Auction auction, ItemStack item) {
        this.player = player;
        this.auction=auction;
        this.item=item;
        ConfigurationSection auctionc = AuctionMaster.plugin.getConfig().getConfigurationSection("events").getConfigurationSection("ClaimExpired");
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

    public ItemStack getItem() {
        return this.item;
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
