package me.intel.AuctionMaster.API.Events;
import me.intel.AuctionMaster.AuctionObjects.Auction;
import me.intel.AuctionMaster.Utils.Utils;
import org.bukkit.ChatColor;
import me.intel.AuctionMaster.AuctionMaster;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AuctionCreateEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private Boolean cancelled = false;
    private final Player player;
    private final Auction auction;

    public AuctionCreateEvent(Player player, Auction auction) {
        this.player = player;
        this.auction = auction;
        ConfigurationSection location = AuctionMaster.plugin.getConfig().getConfigurationSection("events");
        ConfigurationSection auctionc = location.getConfigurationSection("AuctionCreate");
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
