package me.intel.AuctionMaster.API.Events;

import me.intel.AuctionMaster.AuctionObjects.Auction;
import me.intel.AuctionMaster.Utils.Utils;
import me.intel.AuctionMaster.AuctionMaster;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class TopBidClaim extends Event{
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Auction auction;
    private final ItemStack itemWon;

    public TopBidClaim(Player player, Auction auction, ItemStack itemWon) {
        this.player = player;
        this.auction=auction;
        this.itemWon=itemWon;
        ConfigurationSection auctionc = AuctionMaster.plugin.getConfig().getConfigurationSection("events").getConfigurationSection("ItemClaim");
        if(auctionc.getBoolean("Enabled")) {
            String message = auctionc.getString("Message");
            Utils.sendWebhook(message.replace("%player%",player.getName()).replace("%item%", (itemWon.getItemMeta().hasDisplayName() ? ChatColor.stripColor(itemWon.getItemMeta().getDisplayName()) : auction.getItemStack().getType().name())));
        }
    }

    public Player getPlayer() {
        return this.player;
    }

    public Auction getAuction() {
        return this.auction;
    }

    public ItemStack getItemWon() {
        return this.itemWon;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
