package me.intel.AuctionMaster.API;

import me.intel.AuctionMaster.AuctionObjects.Auction;
import me.intel.AuctionMaster.AuctionObjects.Categories.Category;
import me.intel.AuctionMaster.AuctionMaster;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class API {

    public Auction getAuction(String id){
        return AuctionMaster.auctionsHandler.auctions.getOrDefault(id, null);
    }

    public Category getCategory(String category){
        return AuctionMaster.auctionsHandler.getCategory(category);
    }

    public  ArrayList<Auction> getBiddedAuctions(String playerUUID){
        return AuctionMaster.auctionsHandler.bidAuctions.getOrDefault(playerUUID, new ArrayList<>());
    }

    public  ArrayList<Auction> getOwnAuctions(String playerUUID){
        return AuctionMaster.auctionsHandler.ownAuctions.getOrDefault(playerUUID, new ArrayList<>());
    }

    public  double getDeliveryCoins(String playerUUID){
        return AuctionMaster.deliveries.getCoins(playerUUID);
    }

    public  ArrayList<ItemStack> getDeliveryItems(String playerUUID){
        return AuctionMaster.deliveries.getDeliveryItems(playerUUID);
    }

    public  void sendDeliveryCoins(String playerUUID, double coins){
        AuctionMaster.deliveries.addCoins(playerUUID, coins);
    }

    public  void sendDeliveryItem(String playerUUID, ItemStack item){
        AuctionMaster.deliveries.addItem(playerUUID, item);
    }

    public  void setDeliveryCoins(String playerUUID, double coins){
        AuctionMaster.deliveries.setCoins(playerUUID, coins);
    }

    public  void setDeliveryItems(String playerUUID, ArrayList<ItemStack> items){
        AuctionMaster.deliveries.setItems(playerUUID, items);
    }

    public  void removeDeliveries(String playerUUID){
        AuctionMaster.deliveries.removeDelivery(playerUUID);
    }

    public  void setDeliveryItemsAndCoins(String playerUUID, ArrayList<ItemStack> items, double coins){
        AuctionMaster.deliveries.setCoinsAndItems(playerUUID, items, coins);
    }
}
