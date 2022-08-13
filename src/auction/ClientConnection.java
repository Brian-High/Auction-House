package auction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Encapsulates socket connection between an agent program and the auction
 * house server. Handles communication between an agent and the auction house.
 */
public class ClientConnection implements Runnable {
    private int clientID = -1;
    private final Socket socket;
    private final PrintWriter toClient;
    private final BufferedReader fromClient;

    /**
     * Creates a new client connection object with the given parameters
     * @param clientSocket Socket connection activated by the client
     * @throws IOException If an error occurs fetching the I/O streams from
     * the client's socket connection
     */
    public ClientConnection(Socket clientSocket)
            throws IOException {
        this.socket = clientSocket;
        toClient = new PrintWriter(socket.getOutputStream(),true);
        InputStreamReader isr =
                new InputStreamReader(socket.getInputStream());
        fromClient = new BufferedReader(isr);
        toClient.println(BidManager.printItemsToLine());
    }

    /**
     * Thread loop checks for new input from the client, or new messages
     * which need to be sent to the client
     */
    @Override
    public void run() {
        String input, output;

        while (!socket.isClosed() && !Auction.getItems().isEmpty()) {
            try {
                Thread.sleep(100);
            }catch (InterruptedException exc){
                Thread.currentThread().interrupt();
            }

            try {
                input = fromClient.readLine();
            } catch (IOException exc) {
                input = null;
            }

            if (input != null) {
                output = processClientRequest(input);

                if (output != null) {
                    toClient.println(output);
                    output = null;
                }
            }
        }

        if (!socket.isClosed() && Auction.getItems().isEmpty()) {
            toClient.println("auctionClosing soldOut");
        } else Server.removeClient(clientID);
    }

    /**
     * Processes new input from the client
     * @param str Client input
     * @return Output message to be returned to the client (null if there
     * is no message to be returned)
     */
    private synchronized String processClientRequest(String str) {
        String status = "";

        //do bid processing here
        String[] args = str.split(" ");
        System.out.println("Message recieved from client" + str);
        switch (args[0]) {
            case "newBid" -> {
                double bidAmount = Double
                        .parseDouble(args[args.length - 1]);

                String item = "";

                for (String string : Auction.getItems()) {
                    String[] arr1 = string.split("/");

                    if (arr1[0].equalsIgnoreCase(args[1])) {
                        item = string;
                        break;
                    }
                }
                System.out.println("item " + item);
                double currPrice;

                try {
                    currPrice = BidManager.getHighestBid(item);
                } catch (NullPointerException exc) {
                    currPrice = Auction.getInitPrice(item);
                }

                if (bidAmount > currPrice) {
                    BidManager.sendMessage("Bid " + clientID + " " +
                            item + " " + bidAmount);
                    status = null;
                } else status = "invalidBid " + item.split("/")[1] +
                        " amt2low";
            }
            case "ReqItems" -> {
                BidManager.sendMessage(args[0] + " " + clientID);
                status = null;
            }
            case "agentID" -> {
                clientID = Integer.parseInt(args[1]);
                Server.addClient(clientID,this);
                status = null;
            }
        }

        return status;
    }

    /**
     * Sends a message to the client as soon as possible
     * @param msg Message to the client
     */
    protected synchronized void sendMessage(String msg) {
        toClient.println(msg);
    }

    /**
     * Closes the client's connection with the auction house server
     * @param reason Reason for disconnection from the auction house
     * @throws IOException If an error occurs while closing the socket
     */
    protected synchronized void close(String reason) throws IOException {
        toClient.println("auctionClosing " + reason);
        toClient.close();
        fromClient.close();
        socket.close();
    }
}
