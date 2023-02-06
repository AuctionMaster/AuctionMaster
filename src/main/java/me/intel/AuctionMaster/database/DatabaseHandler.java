package me.intel.AuctionMaster.database;

import me.intel.AuctionMaster.AuctionObjects.Auction;
import org.bukkit.entity.Player;

import java.util.HashMap;

public interface DatabaseHandler {
    void deletePreviewItems(String id);
    void registerPreviewItem(String player, String item);
    void removePreviewItem(String player);
    void updateWhenBuyerClaimed(String id);
    boolean checkIFIsInDatabase(String id);
    boolean checkDBifBuyerClaimed(String player);
    boolean checkDBPreviewItems(Player player);
    boolean checkDBIsClaimedItem(String id);
    void insertAuction(Auction auction);
    void updateAuctionField(String id, HashMap<String, String> toUpdate);
    boolean deleteAuction(String id);
    void addToOwnBids(String player, String toAdd);
    boolean removeFromOwnBids(String player, String toRemove);
    void resetOwnBids(String player);
    boolean removeFromOwnAuctions(String player, String toRemove);
    void resetOwnAuctions(String player);
    void addToOwnAuctions(String player, String toAdd);
}
