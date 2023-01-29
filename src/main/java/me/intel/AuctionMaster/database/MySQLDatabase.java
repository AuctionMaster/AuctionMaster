package me.intel.AuctionMaster.database;

import me.intel.AuctionMaster.AuctionObjects.Auction;
import me.intel.AuctionMaster.AuctionObjects.AuctionBIN;
import me.intel.AuctionMaster.AuctionObjects.AuctionClassic;
import me.intel.AuctionMaster.Utils.Utils;
import me.intel.AuctionMaster.AuctionMaster;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import java.sql.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLDatabase implements DatabaseHandler {
    private HikariDataSource hikari;
    private String user;
    private String password;
    private String link;
    private String url;

    public MySQLDatabase() {
        ConfigurationSection section = AuctionMaster.plugin.getConfig().getConfigurationSection("database.mysql");
        if (section == null) {
            AuctionMaster.plugin.getLogger().warning("There is a problem in MySQL config settings!");
            return;
        }

        this.url = section.getString("url");
        this.link = section.getString("link");
        this.user = section.getString("user");
        this.password = section.getString("password");

        Database database = new Database()
                .setJdbcUrl(url)
                .setPassword(password)
                .setUsername(user)
                .setup();

        hikari = database.getHikari();


        loadAuctionsFile();

        /*
        String statements = "ALTER TABLE Auctions RENAME TO AuctionsOld;";
        statements+="CREATE TABLE Auctions " +
                "(id VARCHAR(36), " +
                "coins DOUBLE(25, 0), " +
                "ending BIGINT(15), " +
                "sellerUUID VARCHAR(36), " +
                "item MEDIUMTEXT, " +
                "bids MEDIUMTEXT, " +
                "sellerClaimed BOOL, "+
                "PRIMARY KEY (id));";
        statements+="INSERT INTO Auctions SELECT id, coins, ending, sellerUUID, item, bids, sellerClaimed FROM AuctionsOld;";
        statements+="DROP TABLE AuctionsOld";

        Arrays.stream(statements.split(";")).forEach(statement -> {
            try (
                    Connection conn = getConnection();
                    PreparedStatement statement1 = conn.prepareStatement(statement)
            ) {
                statement1.execute();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        */

        loadAuctionsDataFromFile();

        if (section.getBoolean("refresh.setting")) {
            long seconds = section.getInt("refresh.time", 5) * 20L;
            Bukkit.getScheduler().runTaskTimerAsynchronously(AuctionMaster.plugin, () -> {
                //Bukkit.getLogger().info("Refreshing auctions...");  //DEBUG
                this.refreshAuctions();
                addAllToBrowse();
                loadPreviewItems();
            }, seconds, seconds);
        }
    }

/*    public Connection getConnection() {
        try {
            return hikari.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }*/



    //public Connection getConnection() {
    //    try {
    //        if (this.connection != null)
    //            this.connection.close();
//
    //        Class.forName(this.link != null && !this.link.equals("") ? this.link : "com.mysql.jdbc.Driver");
    //        return this.connection = DriverManager.getConnection(this.url, this.user, this.password);
    //    } catch (SQLException | ClassNotFoundException throwable) {
    //        throwable.printStackTrace();
    //        AuctionMaster.plugin.getLogger().warning("There is a problem in MySQL database!");
    //    }
//
    //    return null;
    //}

    private void loadAuctionsFile() {
        try (
                Connection conn = hikari.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS Auctions " +
                                "(id VARCHAR(36) not NULL, " +
                                " coins DOUBLE(25, 0), " +
                                " ending BIGINT(15), " +
                                " sellerDisplayName VARCHAR(50), " +
                                " sellerName VARCHAR(16), " +
                                " sellerUUID VARCHAR(36), " +
                                " item MEDIUMTEXT, " +
                                " displayName VARCHAR(40), " +
                                " bids MEDIUMTEXT, " +
                                " sellerClaimed BOOL, "+
                                " PRIMARY KEY ( id ))"
                )
        ) {
            stmt.execute();
            AuctionMaster.plugin.getLogger().warning("Auctions database is ready!");
        } catch (Exception x) {
            AuctionMaster.plugin.getLogger().warning("There is a problem in Auctions database!");
            x.printStackTrace();
        }

        try (
                Connection conn = hikari.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS AuctionLists " +
                                "(id VARCHAR(36) not NULL, " +
                                " ownAuctions MEDIUMTEXT, " +
                                " ownBids MEDIUMTEXT, " +
                                " PRIMARY KEY ( id ))")
        ) {
            stmt.execute();
            AuctionMaster.plugin.getLogger().warning("AuctionLists database is ready!");
        } catch (Exception x) {
            AuctionMaster.plugin.getLogger().warning("There is a problem in AuctionLists database!");
            x.printStackTrace();
        }

        try (
                Connection conn = hikari.getConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS PreviewData " +
                                "(id VARCHAR(36) not NULL, " +
                                " item MEDIUMTEXT, " +
                                " PRIMARY KEY ( id ))")
        ) {
            stmt.execute();
            AuctionMaster.plugin.getLogger().warning("PreviewData database is ready!");
        } catch (Exception x) {
            AuctionMaster.plugin.getLogger().warning("There is a problem in PreviewData database!");
            x.printStackTrace();
        }
    }

    public void loadPreviewItems(){
        try (
                Connection Auctions = hikari.getConnection();
                PreparedStatement select = Auctions.prepareStatement("SELECT * FROM PreviewData")
        ) {
            ResultSet resultSet = select.executeQuery();

            while (resultSet.next()) {
                String uuid = resultSet.getString(1);
                if (!uuid.equalsIgnoreCase("serverCloseDate"))
                    try {
                        AuctionMaster.auctionsHandler.previewItems.put(uuid, Utils.itemFromBase64(resultSet.getString(2)));
                    } catch (Exception x) {
                        AuctionMaster.plugin.getLogger().warning("Failed to load player's preview item! UUID: " + uuid);
                    }
            }
        } catch (Exception x) {
            AuctionMaster.plugin.getLogger().warning("There is a problem in PreviewData database!");
            x.printStackTrace();
        }
    }

    public void deletePreviewItems(String id){
        try {
            Connection Auctions = hikari.getConnection();
            PreparedStatement stmt1 = Auctions.prepareStatement("DELETE FROM PreviewData WHERE id = ?;");

            stmt1.setString(1, id);
            stmt1.executeUpdate();

        } catch (Exception x) {
            if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                try {
                    System.out.println("Bu olmamalıydı! PreviewData meşgul.");
                    Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> deletePreviewItems(id), 7L);
                } catch (Exception exception) {
                    System.out.println("Bu olmamalıydı! MySQLDatabase.java/L:186 Exception: " + exception);
                    Bukkit.getServer().getScheduler().runTaskLater(AuctionMaster.plugin, () -> deletePreviewItems(id), 20L);
                }
            else
                x.printStackTrace();
        }
    }

    public void registerPreviewItem(String player, String item) {
        try (
                Connection Auctions = hikari.getConnection();
                PreparedStatement stmt1 = Auctions.prepareStatement("UPDATE PreviewData SET item = ? WHERE id = ?");
                PreparedStatement stmt2 = Auctions.prepareStatement("INSERT INTO PreviewData VALUES(?, ?)")
        ) {
            stmt1.setString(1, item);
            stmt1.setString(2, player);

            int updated = stmt1.executeUpdate();
            if (updated == 0) {
                stmt2.setString(1, player);
                stmt2.setString(2, item);
                stmt2.executeUpdate();
            }
        } catch (Exception x) {
            if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> registerPreviewItem(player, item), 7);
            else
                x.printStackTrace();
        }
    }

    public void removePreviewItem(String player) {
        try (
                Connection Auctions = hikari.getConnection();
                PreparedStatement stmt = Auctions.prepareStatement("DELETE FROM PreviewData WHERE id = ?")
        ) {
            stmt.setString(1, player);
            stmt.executeUpdate();
        } catch (Exception x) {
            if (x.getMessage().startsWith("[SQLITE_BUSY]")) {
                Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> removePreviewItem(player), 7);
            } else
                x.printStackTrace();
        }
    }

    public void insertAuction(Auction auction){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            try(
                    Connection Auctions = hikari.getConnection();
                    PreparedStatement stmt = Auctions.prepareStatement("INSERT INTO Auctions VALUES (?, ?, ?, ?, ?, ?, ?, ?, '"+(auction.isBIN()?"BIN":"")+" 0,,, ', 0)")
            ){
                stmt.setString(1, auction.getId());
                stmt.setDouble(2, auction.getCoins());
                stmt.setLong(3, auction.getEndingDate());
                stmt.setString(4, auction.getSellerDisplayName());
                stmt.setString(5, auction.getSellerName());
                stmt.setString(6, auction.getSellerUUID());
                stmt.setString(7, Utils.itemToBase64(auction.getItemStack()));
                stmt.setString(8, auction.getDisplayName());
                stmt.executeUpdate();
            }catch(Exception x){
                if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                    Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> insertAuction(auction), 7);
                else
                    x.printStackTrace();
            }
        });
    }

    public void updateAuctionField(String id, HashMap<String, String> toUpdate){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            String toSet = "";
            for (Map.Entry<String, String> entry : toUpdate.entrySet())
                toSet = toSet.concat("," + entry.getKey() + "=" + entry.getValue());
            toSet = toSet.substring(1);

            try (
                    Connection Auctions = hikari.getConnection();
                    PreparedStatement stmt = Auctions.prepareStatement("UPDATE Auctions SET " + toSet + " WHERE id = ?")
            ) {
                stmt.setString(1, id);
                stmt.executeUpdate();
            } catch (Exception x) {
                if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                    Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> updateAuctionField(id, toUpdate), 7);
                else
                    x.printStackTrace();
            }
        });
    }

    public boolean deleteAuction(String id){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            try {
                Connection Auctions = hikari.getConnection();
                PreparedStatement stmt = Auctions.prepareStatement("DELETE FROM Auctions WHERE id = ?");

                stmt.setString(1, id);
                stmt.executeUpdate();
            } catch (Exception x) {
                if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                    Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> deleteAuction(id), 7);
                else
                    x.printStackTrace();
            }
        });

        return true;
    }

    public void addToOwnBids(String player, String toAdd){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            try (
                    Connection Auctions = hikari.getConnection();
                    PreparedStatement stmt1 = Auctions.prepareStatement("UPDATE AuctionLists SET ownBids = CONCAT(ownBids, ?) WHERE id = ?");
                    PreparedStatement stmt2 = Auctions.prepareStatement("INSERT INTO AuctionLists VALUES(?, '', ?)")
            ) {
                stmt1.setString(1, "." + toAdd);
                stmt1.setString(2, player);

                int updated = stmt1.executeUpdate();
                if (updated == 0) {
                    stmt2.setString(1, player);
                    stmt2.setString(2, toAdd);
                    stmt2.executeUpdate();
                }
            } catch (Exception x) {
                if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                    Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> addToOwnBids(player, toAdd), 7);
                else
                    x.printStackTrace();
            }
        });
    }

    public boolean removeFromOwnBids(String player, String toRemove){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            try (
                    Connection Auctions = hikari.getConnection();
                    PreparedStatement stmt = Auctions.prepareStatement("UPDATE AuctionLists SET ownBids = REPLACE(REPLACE(REPLACE(ownBids, '" + toRemove + ".', ''), '." + toRemove + "', ''), '" + toRemove + "', '') WHERE id = ?")
            ) {
                stmt.setString(1, player);
                stmt.executeUpdate();
            } catch (Exception x) {
                if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                    Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> removeFromOwnBids(player, toRemove), 7);
                else
                    x.printStackTrace();
            }
        });

        return true;
    }

    public void resetOwnBids(String player){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            try (
                    Connection Auctions = hikari.getConnection();
                    PreparedStatement stmt = Auctions.prepareStatement("UPDATE AuctionLists SET ownBids = '' WHERE id = ?")
            ) {
                stmt.setString(1, player);
                stmt.executeUpdate();
            } catch (Exception x) {
                if (x.getMessage().startsWith("[SQLITE_BUSY]")) {
                    Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> resetOwnBids(player), 7);
                } else
                    x.printStackTrace();
            }
        });
    }

    public boolean removeFromOwnAuctions(String player, String toRemove){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            try (
                    Connection Auctions = hikari.getConnection();
                    PreparedStatement stmt = Auctions.prepareStatement("UPDATE AuctionLists SET ownAuctions = REPLACE(REPLACE(REPLACE(ownAuctions, '" + toRemove + ".', ''), '." + toRemove + "', ''), '" + toRemove + "', '') WHERE id = ?")
            ) {
                stmt.setString(1, player);
                stmt.executeUpdate();
            } catch (Exception e) {
                if (e.getMessage().startsWith("[SQLITE_BUSY]"))
                    Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> removeFromOwnAuctions(player, toRemove));
                else
                    e.printStackTrace();
            }
        });

        return true;
    }

    public void resetOwnAuctions(String player){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            try (
                    Connection Auctions = hikari.getConnection();
                    PreparedStatement stmt = Auctions.prepareStatement("UPDATE AuctionLists SET ownAuctions = '' WHERE id = ?")
            ) {
                stmt.setString(1, player);
                stmt.executeUpdate();
            } catch (Exception x) {
                if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                    Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> resetOwnAuctions(player), 7);
                else
                    x.printStackTrace();
            }
        });
    }

    public void addToOwnAuctions(String player, String toAdd){
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            try (
                    Connection Auctions = hikari.getConnection();
                    PreparedStatement stmt1 = Auctions.prepareStatement("UPDATE AuctionLists SET ownAuctions = CONCAT(ownAuctions, ?) WHERE id = ?");
                    PreparedStatement stmt2 = Auctions.prepareStatement("INSERT INTO AuctionLists VALUES(?, ?, '')")
            ) {
                stmt1.setString(1, "." + toAdd);
                stmt1.setString(2, player);

                int updated = stmt1.executeUpdate();
                if (updated == 0) {
                    stmt2.setString(1, player);
                    stmt2.setString(2, toAdd);
                    stmt2.executeUpdate();
                }
            } catch (Exception x) {
                if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                    Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> addToOwnAuctions(player, toAdd), 7);
                else
                    x.printStackTrace();
            }
        });
    }

    public void adjustAuctionTimers(long toAdd) {
        try (
                Connection Auctions = hikari.getConnection();
                PreparedStatement stmt = Auctions.prepareStatement("UPDATE Auctions SET ending=ending+?")
        ) {
            stmt.setLong(1, toAdd);
            stmt.executeUpdate();
        } catch (Exception x) {
            if (x.getMessage().startsWith("[SQLITE_BUSY]"))
                Bukkit.getScheduler().runTaskLaterAsynchronously(AuctionMaster.plugin, () -> adjustAuctionTimers(toAdd), 7);
            else
                x.printStackTrace();
        }
    }

    public void addAllToBrowse() {
        for (Auction auction : AuctionMaster.auctionsHandler.auctions.values()) {
            if (!auction.isEnded()) {

                AuctionMaster.auctionsHandler.addToBrowse(auction);
            }
        }
    }

    private void refreshAuctions() {
        Bukkit.getScheduler().runTaskAsynchronously(AuctionMaster.plugin, () -> {
            try (
                    Connection connection = hikari.getConnection();
                    PreparedStatement statement = connection.prepareStatement("SELECT * FROM Auctions")
            ) {
                ResultSet set = statement.executeQuery();

                while (set.next()) {
                    String id = set.getString(1);
                    if (AuctionMaster.auctionsHandler.auctions.containsKey(id))
                        continue;

                    Auction auction = set.getString(9).startsWith("BIN") ?
                            new AuctionBIN(set.getString(1), set.getDouble(2), set.getLong(3), set.getString(4), set.getString(5), set.getString(6), set.getString(7), set.getString(8), set.getString(9))
                            : new AuctionClassic(set.getString(1), set.getDouble(2), set.getLong(3), set.getString(4), set.getString(5), set.getString(6), set.getString(7), set.getString(8), set.getString(9), set.getBoolean(10));

                    AuctionMaster.auctionsHandler.auctions.put(id, auction);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try (
                    Connection connection = hikari.getConnection();
                    PreparedStatement statement = connection.prepareStatement("DELETE FROM AuctionLists WHERE ownAuctions='' and ownBids=''")
            ) {
                statement.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try (
                    Connection connection = hikari.getConnection();
                    PreparedStatement statement = connection.prepareStatement("SELECT * FROM AuctionLists")
            ) {
                ResultSet set = statement.executeQuery();

                while (set.next()) {
                    ArrayList<Auction> list = new ArrayList<>();
                    String id = set.getString(1);
                    String ownAuctions = set.getString(2);
                    String ownBids = set.getString(3);

                    for (String auctionID : ownAuctions.split("\\.")) {
                        if (auctionID.equals(""))
                            continue;

                        Auction auction = AuctionMaster.auctionsHandler.auctions.get(auctionID);
                        if (auction == null)
                            continue;

                        list.add(auction);
                    }

                    if (!list.isEmpty()) {
                        AuctionMaster.auctionsHandler.ownAuctions.put(id, list);
                    }
                    list = new ArrayList<>();

                    for (String bidID : ownBids.split("\\.")) {
                        if (bidID.equals(""))
                            continue;

                        Auction auction = AuctionMaster.auctionsHandler.auctions.get(bidID);
                        if (auction == null)
                            continue;

                        list.add(auction);
                    }

                    if (!list.isEmpty()) {
                        AuctionMaster.auctionsHandler.bidAuctions.put(id, list);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadAuctionsDataFromFile(){
        long toAdd = AuctionMaster.serverCloseDate;
        if (toAdd != 0L)
            adjustAuctionTimers(ZonedDateTime.now().toInstant().toEpochMilli() - toAdd);

        refreshAuctions();

        addAllToBrowse();
        loadPreviewItems();
    }
}