package agent;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * An object that handles all the sockets for an agent.
 * auctionConnections - a hashmap, format auctionName, auctionConnection object
 */
public class Server{
    private static BankConnection bankConnection;
    private static int clientId;
    private static final HashMap<String,AuctionConnection> auctionConnections = new HashMap<>();


    public Server(String hostName, int portNumber) throws Exception{
        bankConnection = new BankConnection(hostName,portNumber);
        Thread t = new Thread(bankConnection);
        t.start();
    }

    /**
     * Runs when a user selects an auction, establishes a new auctionConnection if it doesn't already exist
     * @param auction the selected auction
     */
    protected static void addAuctionConnection(Auction auction) {
        if(auctionConnections.get(auction.getName()) != null) return;

        try{
            AuctionConnection auctionConnection = new AuctionConnection(auction.getHostName(), auction.getPort(),auction);
            Thread t = new Thread(auctionConnection);
            t.start();
            auctionConnections.put(auction.getName(),auctionConnection);
        } catch(Exception e){
            System.err.println("auction not Found");
        }
    }

    protected static void requestAuctionItems(Auction auction){
        auctionConnections.get(auction.getName()).sendMessage("ReqItems");
    }

    protected static void submitBid(String auctionName, String itemName, int bid){
        auctionConnections.get(auctionName).sendMessage("newBid " + itemName + " " + bid);
    }

    public static BankConnection getBankConnection() {
        return bankConnection;
    }

    public static void setClientId(int clientId) {
        Server.clientId = clientId;
    }

    public static int getClientId() {
        return clientId;
    }
}

