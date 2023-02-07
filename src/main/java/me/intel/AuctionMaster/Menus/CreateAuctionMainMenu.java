package me.intel.AuctionMaster.Menus;

import me.intel.AuctionMaster.API.Events.AuctionPreviewItemEvent;
import me.intel.AuctionMaster.AuctionMaster;
import me.intel.AuctionMaster.InputGUIs.StartingBidGUI.StartingBidGUI;
import me.intel.AuctionMaster.Utils.Utils;
import me.intel.AuctionMaster.database.MySQLDatabase;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.*;

public class CreateAuctionMainMenu {

    private Inventory inventory;
    private Player player;
    private final ClickListen listener = new ClickListen();
    private ItemStack previewItem;
    private int previewSlot;
    private ItemStack createAuctionItemNo;
    private double startingBid;
    private double startingFeeTime;
    private double startingBidFee;
    private String startingDuration;
    private boolean preventDoubleClick;
    private final boolean customtax = AuctionMaster.customTax;
    private double customTaxBin;
    private double customTaxBid;

    private double getCustomTaxBin() {  //BUY IT NOW!
        if (customtax) {
            ArrayList<Double> binTax = new ArrayList<>();
            for (PermissionAttachmentInfo effectivePermission : player.getEffectivePermissions()) {
                if (effectivePermission.getPermission().contains("auctionmaster.customtax.bin.") && effectivePermission.getValue()) {
                    binTax.add(Double.parseDouble(effectivePermission.getPermission().substring(28)));
                }
            }
            if (binTax.isEmpty()) {
                customTaxBin = 0;
            } else {
                Collections.sort(binTax);
                customTaxBin = binTax.get(0);
            }
        }
        return customTaxBin;
    }

    private double getCustomTaxBid() {  //AUCTION
        if (customtax) {
            ArrayList<Double> bidTax = new ArrayList<>();
            for (PermissionAttachmentInfo effectivePermission : player.getEffectivePermissions()) {
                if (effectivePermission.getPermission().contains("auctionmaster.customtax.bid.") && effectivePermission.getValue()) {
                    bidTax.add(Double.parseDouble(effectivePermission.getPermission().substring(28)));
                }
            }
            if (bidTax.isEmpty()) {
                customTaxBid = 0;
            } else {
                Collections.sort(bidTax);
                customTaxBid = bidTax.get(0);
            }
        }
        return customTaxBid;
    }

    void loadTime() {
        long startingTime;
        if (buyItNow && !AuctionMaster.configLoad.BinTimer)
            startingTime = 0;
        else {
            if (AuctionMaster.auctionsHandler.startingDuration.containsKey(player.getUniqueId().toString()))
                startingTime = AuctionMaster.auctionsHandler.startingDuration.get(player.getUniqueId().toString());
            else
                startingTime = AuctionMaster.configLoad.defaultDuration;
        }
        String startingDuration;
        if (startingTime != 0) {
            this.startingDuration = Utils.fromMiliseconds((int) startingTime);
            startingDuration = AuctionMaster.numberFormatHelper.useDecimals ?
                    AuctionMaster.numberFormatHelper.formatNumber(startingFeeTime = AuctionMaster.configLoad.durationFeeCalculator((int) (startingTime / 3600000)))
                    :
                    AuctionMaster.numberFormatHelper.formatNumber(startingFeeTime = Math.floor(AuctionMaster.configLoad.durationFeeCalculator((int) (startingTime / 3600000))));
        } else {
            this.startingDuration = "Never Expire";
            startingFeeTime = 0;
            startingDuration = "0";
        }
        ArrayList<String> lore = new ArrayList<>();
        for (String line : AuctionMaster.configLoad.durationItemLore)
            lore.add(AuctionMaster.utilsAPI.chat(player, line
                    .replace("%auction-time%", this.startingDuration)
                    .replace("%auction-fee%", startingDuration)
            ));

        if (AuctionMaster.menusCfg.getInt("create-auction-menu.duration-slot") >= 0)
            inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.duration-slot"),
                    AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.durationItemMaterial,
                            AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.durationItemName.replace("%auction-time%",
                                    this.startingDuration).replace("%auction-fee%", startingDuration)), lore));
    }

    private int getMaximumAuctions() {
        if (AuctionMaster.plugin.getConfig().getBoolean("use-auction-limit")) {
            for (int start = AuctionMaster.configLoad.maxAuctionPerPlayer; start >= 0; start--)
                if (player.hasPermission("auctionmaster.limit.auctions." + start))
                    return start;
        }
        return AuctionMaster.configLoad.maxAuctionPerPlayer;
    }

    private ItemStack getCreateAuctionItemYes(String displayName) {
        ItemStack toReturn = AuctionMaster.configLoad.createAuctionConfirmYesMaterial.clone();
        ItemMeta meta = toReturn.getItemMeta();
        meta.setDisplayName(AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.createAuctionConfirmYesName));
        ArrayList<String> lore = new ArrayList<>();
        for (String line : AuctionMaster.configLoad.createAuctionConfirmYesLore)
            lore.add(AuctionMaster.utilsAPI.chat(player, line
                    .replace("%item-name%", displayName)
                    .replace("%duration%", startingDuration)
                    .replace("%starting-bid%", AuctionMaster.numberFormatHelper.formatNumber(startingBid))
                    .replace("%fee%", AuctionMaster.numberFormatHelper.formatNumber(startingBidFee + startingFeeTime))
            ));
        meta.setLore(lore);
        toReturn.setItemMeta(meta);
        return toReturn;
    }

    private void generateCreateAuctionItemNo() {
        createAuctionItemNo = AuctionMaster.configLoad.createAuctionConfirmNoMaterial.clone();
        ItemMeta meta = createAuctionItemNo.getItemMeta();
        meta.setDisplayName(AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.createAuctionConfirmNoName));
        ArrayList<String> lore = new ArrayList<>();
        for (String line : AuctionMaster.configLoad.createAuctionConfirmNoLore)
            lore.add(AuctionMaster.utilsAPI.chat(player, line));
        meta.setLore(lore);
        createAuctionItemNo.setItemMeta(meta);
    }

    private ItemStack transformToPreview(ItemStack toTransform) {
        String name = Utils.getDisplayName(toTransform);
        ItemStack toReturn = toTransform.clone();
        ItemMeta meta = toReturn.getItemMeta();
        meta.setDisplayName(AuctionMaster.utilsAPI.chat(player, AuctionMaster.auctionsManagerCfg.getString("preview-selected-item-name")));
        ArrayList<String> lore = new ArrayList<>();
        lore.add(name);
        lore.add(" ");
        if (meta.getLore() != null) {
            for (String line : meta.getLore())
                lore.add(AuctionMaster.utilsAPI.chat(player, line));
            lore.add(" ");
        }
        lore.add(AuctionMaster.utilsAPI.chat(player, AuctionMaster.auctionsManagerCfg.getString("preview-selected-item-take-back")));
        meta.setLore(lore);
        toReturn.setItemMeta(meta);
        return toReturn;
    }

    private void setupStartingBidItem() {
        ArrayList<String> lore = new ArrayList<>();
        if (buyItNow) {
            if (AuctionMaster.auctionsHandler.buyItNowSelected != null && !AuctionMaster.configLoad.onlyBuyItNow) {
                for (String line : AuctionMaster.configLoad.switchToAuctionLore)
                    lore.add(AuctionMaster.utilsAPI.chat(player, line));

                if (AuctionMaster.menusCfg.getInt("create-auction-menu.switch-type-slot") >= 0)
                    inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.switch-type-slot"),
                            AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.switchToAuctionMaterial,
                                    AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.switchToAuctionName), lore));
            }
            lore = new ArrayList<>();
            for (String line : AuctionMaster.configLoad.editBINPriceLore)
                lore.add(AuctionMaster.utilsAPI.chat(player, line
                        .replace("%price%", AuctionMaster.numberFormatHelper.formatNumber(startingBid))
                        .replace("%fee%", AuctionMaster.numberFormatHelper.formatNumber(startingBidFee))
                ));

            if (AuctionMaster.menusCfg.getInt("create-auction-menu.starting-bid-slot") >= 0)
                inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.starting-bid-slot"),
                        AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.editBINPriceMaterial,
                                AuctionMaster.utilsAPI.chat(player,
                                        AuctionMaster.configLoad.editBINPriceName.replace("%price%",
                                                AuctionMaster.numberFormatHelper.formatNumber(startingBid)).replace("%fee%",
                                                AuctionMaster.numberFormatHelper.formatNumber(startingBidFee))), lore));
        } else {
            if (AuctionMaster.auctionsHandler.buyItNowSelected != null) {
                for (String line : AuctionMaster.configLoad.switchToBinLore)
                    lore.add(AuctionMaster.utilsAPI.chat(player, line));

                if (AuctionMaster.menusCfg.getInt("create-auction-menu.switch-type-slot") >= 0)
                    inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.switch-type-slot"),
                            AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.switchToBinMaterial,
                                    AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.switchToBinName), lore));
            }
            lore = new ArrayList<>();
            for (String line : AuctionMaster.configLoad.startingBidItemLore)
                lore.add(AuctionMaster.utilsAPI.chat(player, line
                        .replace("%starting-bid%", AuctionMaster.numberFormatHelper.formatNumber(startingBid))
                        .replace("%starting-fee%", AuctionMaster.numberFormatHelper.formatNumber(startingBidFee))
                ));

            if (AuctionMaster.menusCfg.getInt("create-auction-menu.starting-bid-slot") >= 0)
                inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.starting-bid-slot"),
                        AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.startingBidItemMaterial,
                                AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.startingBidItemName.replace("%starting-bid%",
                                        AuctionMaster.numberFormatHelper.formatNumber(startingBid)).replace("%starting-fee%",
                                        AuctionMaster.numberFormatHelper.formatNumber(startingBidFee))), lore));
        }
    }

    boolean buyItNow;
    public CreateAuctionMainMenu(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            this.player = player;
            if (!AuctionMaster.auctionsDatabase.checkDBPreviewItems(player)){
                AuctionMaster.auctionsHandler.previewItems.remove(player.getUniqueId().toString());
            }
            inventory = Bukkit.createInventory(player, AuctionMaster.configLoad.createAuctionMenuSize,
                    AuctionMaster.utilsAPI.chat(player,
                            AuctionMaster.configLoad.createAuctionMenuName));

            generateCreateAuctionItemNo();
            buyItNow = AuctionMaster.auctionsHandler.buyItNowSelected != null
                    &&
                    (AuctionMaster.configLoad.onlyBuyItNow
                            ||
                            ((AuctionMaster.configLoad.defaultBuyItNow
                                    &&
                                    !AuctionMaster.auctionsHandler.buyItNowSelected.contains(player.getUniqueId().toString()))
                                    ||
                                    (!AuctionMaster.configLoad.defaultBuyItNow && AuctionMaster.auctionsHandler.buyItNowSelected.contains(player.getUniqueId().toString()))));
            if (AuctionMaster.configLoad.useBackgoundGlass)
                for (int i = 0; i < AuctionMaster.configLoad.createAuctionMenuSize; i++)
                    inventory.setItem(i, AuctionMaster.configLoad.backgroundGlass.clone());

            ArrayList<String> lore = new ArrayList<>();
            for (String line : AuctionMaster.configLoad.createAuctionPreviewLoreNoItem)
                lore.add(AuctionMaster.utilsAPI.chat(player, line));
            previewItem = AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.createAuctionPreviewMaterial, AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.createAuctionPreviewNameNoItem), lore);

            if (AuctionMaster.auctionsHandler.previewItems.containsKey(player.getUniqueId().toString())) {
                inventory.setItem(previewSlot = AuctionMaster.menusCfg.getInt("create-auction-menu.preview-item-slot"), transformToPreview(AuctionMaster.auctionsHandler.previewItems.get(player.getUniqueId().toString())));
            } else {
                inventory.setItem(previewSlot = AuctionMaster.menusCfg.getInt("create-auction-menu.preview-item-slot"), previewItem);
            }

            if (AuctionMaster.auctionsHandler.startingBid.containsKey(player.getUniqueId().toString()))
                startingBid = AuctionMaster.auctionsHandler.startingBid.get(player.getUniqueId().toString());
            else
                startingBid = AuctionMaster.configLoad.defaultStartingBid;

            double feeBID = AuctionMaster.configLoad.startingBidFee;
            double feeBIN = AuctionMaster.configLoad.startingBidBINFee;


            if (customtax) {
                if (getCustomTaxBin() > 0) {
                    feeBIN = getCustomTaxBin();
                }
                if (getCustomTaxBid() > 0) {
                    feeBID = getCustomTaxBid();
                }
            }
            startingBidFee = startingBid * (buyItNow ? feeBIN : feeBID) / 100;


            if (!AuctionMaster.numberFormatHelper.useDecimals) {
                startingBid = Math.floor(startingBid);
                startingBidFee = Math.floor(startingBidFee);
            }

            setupStartingBidItem();
            loadTime();

            if (AuctionMaster.auctionsHandler.previewItems.containsKey(player.getUniqueId().toString()))
                inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.create-auction-button-slot"), getCreateAuctionItemYes(Utils.getDisplayName(AuctionMaster.auctionsHandler.previewItems.get(player.getUniqueId().toString()))));
            else
                inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.create-auction-button-slot"), createAuctionItemNo);

            lore = new ArrayList<>();
            for (String line : AuctionMaster.configLoad.goBackLore)
                lore.add(AuctionMaster.utilsAPI.chat(player, line));
            inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.go-back-slot"), AuctionMaster.itemConstructor.getItem(AuctionMaster.configLoad.goBackMaterial, AuctionMaster.utilsAPI.chat(player, AuctionMaster.configLoad.goBackName), lore));

            Bukkit.getScheduler().runTask(AuctionMaster.plugin, () -> {
                Bukkit.getPluginManager().registerEvents(listener, AuctionMaster.plugin);
                player.openInventory(inventory);
            });
        });
    }

    public class ClickListen implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent e) {
            if (e.getInventory().equals(inventory)) {
                e.setCancelled(true);
                if (preventDoubleClick)
                    return;
                if (e.getCurrentItem() == null || e.getCurrentItem().getType().equals(Material.AIR)) {
                    return;
                }
                if (e.getClickedInventory().equals(player.getInventory())) {
                    if (AuctionMaster.configLoad.isBlacklisted(player, e.getCurrentItem())) {
                        player.sendMessage(AuctionMaster.utilsAPI.chat(player, AuctionMaster.plugin.getConfig().getString("blacklist-item-message")));
                        return;
                    }
                    AuctionPreviewItemEvent event = new AuctionPreviewItemEvent(player, e.getCurrentItem());
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled())
                        return;

                    Utils.playSound(player, "inventory-item-click");
                    ItemStack saveCurrentItem = e.getCurrentItem().clone();
                    ItemStack toSet = transformToPreview(e.getCurrentItem());

                    preventDoubleClick = true;

                    Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
                        if (AuctionMaster.pluginDisable)
                            return;

                        if (AuctionMaster.auctionsHandler.previewItems.containsKey(player.getUniqueId().toString())) {
                            ItemStack lastRecordedItem = AuctionMaster.auctionsHandler.previewItems.get(player.getUniqueId().toString());
                            try {
                                AuctionMaster.auctionsDatabase.registerPreviewItem(player.getUniqueId().toString(), Utils.itemToBase64(saveCurrentItem));
                            } catch (Exception exception) {
                                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(AuctionMaster.plugin, () -> AuctionMaster.auctionsDatabase.registerPreviewItem(player.getUniqueId().toString(), Utils.itemToBase64(saveCurrentItem)), 1L);
                                exception.printStackTrace();
                            }
                            player.getInventory().setItem(e.getSlot(), lastRecordedItem);
                        } else {
                            player.getInventory().setItem(e.getSlot(), new ItemStack(Material.AIR));
                            // sry for boilerplate
                            try {
                                AuctionMaster.auctionsDatabase.registerPreviewItem(player.getUniqueId().toString(), Utils.itemToBase64(saveCurrentItem));
                            } catch (Exception exception) {
                                Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(AuctionMaster.plugin, () -> AuctionMaster.auctionsDatabase.registerPreviewItem(player.getUniqueId().toString(), Utils.itemToBase64(saveCurrentItem)), 1L);
                                exception.printStackTrace();
                            }
                        }

                        AuctionMaster.auctionsHandler.previewItems.put(player.getUniqueId().toString(), saveCurrentItem);

                        String compareInventory = AuctionMaster.auctionsManagerCfg.getString("create-menu-name");
                        compareInventory = ChatColor.translateAlternateColorCodes('&', compareInventory);

                        if (player.getOpenInventory().getTitle().equals(compareInventory)) {
                            inventory.setItem(previewSlot, toSet);
                            inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.create-auction-button-slot"), getCreateAuctionItemYes(Utils.getDisplayName(saveCurrentItem)));
                        }
                    });

                    Bukkit.getScheduler().runTaskLater(AuctionMaster.plugin, () -> preventDoubleClick = false, 10L);
                } else {
                    if (e.getSlot() == AuctionMaster.menusCfg.getInt("create-auction-menu.duration-slot")) {
                        if (buyItNow && !AuctionMaster.configLoad.BinTimer)
                            return;
                        Utils.playSound(player, "duration-item-click");
                        new DurationSelectMenu(player);
                    } else if (e.getSlot() == previewSlot) {

                        AuctionPreviewItemEvent event = new AuctionPreviewItemEvent(player, e.getCurrentItem());
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled())
                            return;

                        preventDoubleClick = true;

                        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
                            if (AuctionMaster.pluginDisable)
                                return;

                            if (AuctionMaster.auctionsHandler.previewItems.containsKey(player.getUniqueId().toString()) && Utils.getEmptySlots(player) != 0) {
                                try {
                                    AuctionMaster.auctionsDatabase.deletePreviewItems(player.getUniqueId().toString());
                                } catch (Exception exception) {
                                    Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(AuctionMaster.plugin, () -> AuctionMaster.auctionsDatabase.deletePreviewItems(player.getUniqueId().toString()), 1L);
                                    exception.printStackTrace();
                                }

                                Bukkit.getScheduler().runTaskLater(AuctionMaster.plugin, () -> {
                                    player.getInventory().addItem(AuctionMaster.auctionsHandler.previewItems.get(player.getUniqueId().toString()));
                                    AuctionMaster.auctionsHandler.previewItems.remove(player.getUniqueId().toString());

                                    String compareInventory = AuctionMaster.auctionsManagerCfg.getString("create-menu-name");
                                    compareInventory = ChatColor.translateAlternateColorCodes('&', compareInventory);

                                    if (player.getOpenInventory().getTitle().equals(compareInventory)) {
                                        inventory.setItem(previewSlot, previewItem.clone());
                                        inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.create-auction-button-slot"), createAuctionItemNo);
                                    }
                                }, 1L);
                            }
                        });

                        Bukkit.getScheduler().runTaskLater(AuctionMaster.plugin, () -> preventDoubleClick = false, 10L);
                    } else if (e.getSlot() == AuctionMaster.menusCfg.getInt("create-auction-menu.switch-type-slot")) {
                        if (AuctionMaster.auctionsHandler.buyItNowSelected != null && !AuctionMaster.configLoad.onlyBuyItNow) {
                            if (buyItNow) {
                                if (AuctionMaster.configLoad.defaultBuyItNow)
                                    AuctionMaster.auctionsHandler.buyItNowSelected.add(player.getUniqueId().toString());
                                else
                                    AuctionMaster.auctionsHandler.buyItNowSelected.remove(player.getUniqueId().toString());
                                buyItNow = false;
                            } else {
                                if (AuctionMaster.configLoad.defaultBuyItNow)
                                    AuctionMaster.auctionsHandler.buyItNowSelected.remove(player.getUniqueId().toString());
                                else
                                    AuctionMaster.auctionsHandler.buyItNowSelected.add(player.getUniqueId().toString());
                                buyItNow = true;
                            }
                            double feeBID = AuctionMaster.configLoad.startingBidFee;
                            double feeBIN = AuctionMaster.configLoad.startingBidBINFee;

                            if (customtax) {
                                if (getCustomTaxBin() > 0) {
                                    feeBIN = getCustomTaxBin();
                                }
                                if (getCustomTaxBid() > 0) {
                                    feeBID = getCustomTaxBid();
                                }
                            }
                            startingBidFee = startingBid * (buyItNow ? feeBIN : feeBID) / 100;
                            if (!AuctionMaster.numberFormatHelper.useDecimals) {
                                startingBid = Math.floor(startingBid);
                                startingBidFee = Math.floor(startingBidFee);
                            }
                            loadTime();
                            setupStartingBidItem();
                            if (AuctionMaster.auctionsHandler.previewItems.containsKey(player.getUniqueId().toString())) {
                                inventory.setItem(AuctionMaster.menusCfg.getInt("create-auction-menu.create-auction-button-slot"), getCreateAuctionItemYes(Utils.getDisplayName(AuctionMaster.auctionsHandler.previewItems.get(player.getUniqueId().toString()))));
                            }
                        }
                    } else if (e.getSlot() == AuctionMaster.menusCfg.getInt("create-auction-menu.starting-bid-slot")) {
                        Utils.playSound(player, "starting-bid-item-click");
                        StartingBidGUI.selectStartingBid.openGUI(player);
                    } else if (e.getSlot() == AuctionMaster.menusCfg.getInt("create-auction-menu.create-auction-button-slot")) {
                        if (AuctionMaster.auctionsHandler.previewItems.containsKey(player.getUniqueId().toString())) {
                            if (AuctionMaster.auctionsHandler.ownAuctions.getOrDefault(player.getUniqueId().toString(), new ArrayList<>()).size() < getMaximumAuctions()) {
                                if (AuctionMaster.economy.hasMoney(player, startingBidFee + startingFeeTime)) {
                                    Utils.playSound(player, "create-auction-item-click");
                                    new CreateAuctionConfirmMenu(player, startingBidFee + startingFeeTime);
                                } else {
                                    player.sendMessage(AuctionMaster.utilsAPI.chat(player, AuctionMaster.auctionsManagerCfg.getString("not-enough-money-auction")));
                                }
                            } else
                                player.sendMessage(AuctionMaster.utilsAPI.chat(player, AuctionMaster.plugin.getConfig().getString("auction-limit-reached-message")));
                        }
                    } else if (e.getSlot() == AuctionMaster.menusCfg.getInt("create-auction-menu.go-back-slot")) {
                        Utils.playSound(player, "go-back-click");
                        if (AuctionMaster.auctionsHandler.ownAuctions.containsKey(player.getUniqueId().toString())) {
                            new ManageOwnAuctionsMenu(player, 1);
                        } else
                            new MainAuctionMenu(player);
                    }
                }
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent e) {
            if (inventory.equals(e.getInventory())) {
                HandlerList.unregisterAll(this);
                inventory = null;
            }
        }

    }
}