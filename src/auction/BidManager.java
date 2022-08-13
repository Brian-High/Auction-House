package auction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Handles all the bidding activity on items for sale at the auction house.
 * Receives bid requests from agents, sends hold requests to the bank,
 * accepts the bids if the bank places the hold, or rejects the bid if the
 * hold could not be placed. When time is up on an item, the
 */
public class BidManager implements Runnable {
    private static final Map<String, Double> currBids = new HashMap<>();
    private static final Map<String, Integer> highestBidders = new HashMap<>();
    private static final Map<String, Integer> bidTimers = new HashMap<>();
    private static final Map<String, Integer> itemWinners = new HashMap<>();
    private static final BlockingQueue<String> inbox =
            new ArrayBlockingQueue<>(20);

    /**
     * Active object used to update the times remaining on current items
     * that are up for auction and have bidding activity
     */
    private final class BidTimer implements Runnable {
        private final int maxTime;
        private final List<String> itemsCopy;

        /**
         * Creates a new bid timer with the given parameters
         * @param maxTime Time before an auction ends
         * @param items List of auction items
         */
        public BidTimer(int maxTime, List<String> items) {
            this.maxTime = maxTime;
            itemsCopy = new ArrayList<>();
            itemsCopy.addAll(items);
        }

        /**
         * Waits for one second, then updates all times. If a time exceeds
         * the max time, the item is won by the highest bidder
         */
        @Override
        public void run() {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                for (String item: itemsCopy) {
                    try {
                        int time = bidTimers.get(item);

                        if (time + 1 >= maxTime) {
                            timeUp(item);
                            bidTimers.remove(item);
                        }
                        else bidTimers.put(item, time + 1);
                    } catch (NullPointerException exc) {}
                }
            }
        }
    }

    /**
     * Creates a new bid manager object
     */
    public BidManager() {
        BidTimer timer = new BidTimer(30, Auction.getItems());
        Thread t = new Thread(timer);
        t.start();
    }

    /**
     * Thread loop checks for new messages to the bid manager
     */
    @Override
    public void run() {
        while (true) {
            if (!inbox.isEmpty()) processInbox();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * @return True if bidding is currently taking place, otherwise false
     */
    protected static boolean hasUnresolvedBids() {
        return !currBids.isEmpty();
    }

    /**
     * Fetches the highest bid on an item, if any
     * @param itemName Name of the item
     * @return Current highest bid as a double value
     * @throws NullPointerException If there is no highest bid for this item
     */
    protected static double getHighestBid(String itemName)
            throws NullPointerException {
        Double value = currBids.get(itemName);

        if (value != null) return value;
        else throw new NullPointerException();
    }

    /**
     * Call this method when time is up for an item with bidding activity
     * @param itemName Name of the item that has been won
     */
    private synchronized void timeUp(String itemName) {
        System.out.println("time up " + itemName);
        int winnerID = highestBidders.get(itemName);
        Auction.getItems().remove(itemName);
        currBids.remove(itemName);
        itemWinners.put(itemName, winnerID);
        ClientConnection cc = Server.getClient(winnerID);
        cc.sendMessage("itemWon " + itemName.substring(itemName.indexOf('/') + 1));
        BankConnection.sendMessage("itemWon " + itemName.substring(itemName.indexOf('/') + 1) +  " " + winnerID);
        Server.sendItemsUpdate(printItemsToLine());
    }

    /**
     * Sends a message to the bid manager
     * @param msg Message to the bid manager
     */
    protected static void sendMessage(String msg) {
        inbox.add(msg);
    }

    /**
     * Handles every message currently in the inbox
     */
    private synchronized void processInbox() {
        while (!inbox.isEmpty()) {
            String[] args = inbox.poll().split(" ");

            //determine type of message
            switch (args[0]) {
                case "Bid" -> {
                    String[] result = args[2].split("/");
                    BankConnection.sendMessage("reqHold " + args[1] + " " +
                            args[3] + " " + result[1]);
                }
                case "ReqItems" -> {
                    int clientID = Integer.parseInt(args[1]);
                    ClientConnection cc = Server.getClient(clientID);
                    cc.sendMessage(printItemsToLine());
                }
                case "holdSuccessful" -> {
                    System.out.println(args[1]);
                    int bidderID = Integer.parseInt(args[1]);
                    double bidAmt = Double.parseDouble(args[2]);
                    placeBid(bidderID, args[3], bidAmt);
                }
                case "holdFailed" -> {
                    int rejectedID = Integer.parseInt(args[1]);
                    ClientConnection cc = Server.getClient(rejectedID);
                    cc.sendMessage("invalidBid " + args[2] + " holdFailed");
                }
                case "fundsTransferred" -> {
                    String item = "";

                    for (String string : Auction.getItems()) {
                        String[] arr = string.split("/");

                        if (arr[1].equalsIgnoreCase(args[1])) {
                            item = string;
                            break;
                        }
                    }

                    int winnerID = itemWinners.remove(item);
                    ClientConnection winner = Server.getClient(winnerID);
                    winner.sendMessage("itemDelivered " + args[1]);
                }
            }
        }
    }

    /**
     * @return List of auction items currently for sale in a single string
     */
    protected static String printItemsToLine() {
        String itemsList = "auctionItems";

        for (int index = 0; index < Auction.getItems().size() &&
                index < Auction.getMaxConcurrentSales(); index++) {
            String itemName = Auction.getItems().get(index);
            itemsList += " " + itemName;
            double currPrice;

            try {
                currPrice = BidManager.getHighestBid(itemName);
            } catch (NullPointerException exc) {
                currPrice = Auction.getInitPrice(itemName);
            }

            itemsList += " " + currPrice;
        }

        return itemsList;
    }

    /**
     * Places a new highest bid on an item for sale
     * @param clientID ID number of the bid placer
     * @param itemID ID number of the item
     * @param amount Bid amount as a double value
     */
    private synchronized void placeBid(int clientID, String itemID,
                                       double amount) {
        ClientConnection cc = Server.getClient(clientID);
        String item = "";

        for (String string : Auction.getItems()) {
            String[] arr = string.split("/");

            if (arr[1].equalsIgnoreCase(itemID)) {
                item = string;
                break;
            }
        }

        double currPrice;

        try {
            currPrice = BidManager.getHighestBid(item);
        } catch (NullPointerException exc) {
            currPrice = Auction.getInitPrice(item);
        }

        if (amount > currPrice) {
            System.out.println("bidPlaced " + item + " " + amount + " " + clientID);
            cc.sendMessage("bidPlaced " + item.split("/")[1]);
            bidTimers.put(item, -1);
            Double lastBid = currBids.put(item, amount);
            Integer outbidID = highestBidders.get(item);
            highestBidders.remove(item);
            highestBidders.put(item, clientID);
            synchronized (this){
            if (lastBid != null && outbidID != null && outbidID != clientID) {
                ClientConnection outbidClient = Server.getClient(outbidID);
                outbidClient.sendMessage("OutBid " + item.split("/")[1]);
                outbidClient.sendMessage(printItemsToLine());
                BankConnection.sendMessage("removeHold " + outbidID + " " +
                        lastBid + " " + item.split("/")[1]);
            }

                Server.sendItemsUpdate(printItemsToLine());
            }



        } else {
            cc.sendMessage("invalidBid " + item.split("/")[1] + " amt2low");
            BankConnection.sendMessage("removeHold " + clientID + " " +
                    amount + " " + item.substring(item.indexOf('/')));
        }
    }
}
