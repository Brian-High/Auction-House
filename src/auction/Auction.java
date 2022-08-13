package auction;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static java.lang.Math.random;

/**
 * Main class of the auction program. Constructs item list from user file
 * (if given), fetches bank host and port info from STDIN, and initializes
 * bank connection, auction house server, and bid manager used to run the
 * auction house.
 */
public class Auction {
    private static int port = -1;
    private static String  auctionName = "";
    private static final Map<String, Double> initPrices = new HashMap<>();
    private static final List<String> items = new ArrayList<>();
    private static int maxItems = 3;

    /**
     * Entry point to the program
     * @param args Command line arguments. args[0] = local port for auction
     *             house server (required), args[1] = items list file
     *             (optional)
     */
    public static void main(String[] args) {
        File file = null;

        if (args.length >= 2) file = new File(args[1]);

        if (file !=  null) {
            try (BufferedReader br = new BufferedReader(
                    new FileReader(file))) {
                String in;
                List<Integer> ids = new ArrayList<>();

                while ((in = br.readLine()) != null) {
                    String[] arr = in.split(" ");

                    String item = "";

                    for (int a = 0; a < arr.length - 1; a++) {
                        item += a == 0 ? arr[a] : "-" + arr[a];
                    }

                    int id;

                    do {
                        id = (int)(random() * 1000);
                    } while (ids.contains(id));

                    item += "/" + id;
                    ids.add(id);

                    double startBid;
                    try {
                        startBid = Double.parseDouble(arr[arr.length - 1]);
                    } catch (NumberFormatException exc) {
                        startBid = 50.0;
                    }

                    initPrices.put(item, startBid);
                    items.add(item);
                }
            } catch (FileNotFoundException exc) {
                System.err.println("Items file was not found");
            } catch (IOException exc) {
                System.err.println("Unable to read items list");
            }
        }

        if (items.isEmpty() && initPrices.isEmpty()) {
            List<Integer> ids = new ArrayList<>();

            while (ids.size() < 4) {
                int newID = (int)(random() * 1000);

                if (!ids.contains(newID)) ids.add(newID);
            }

            items.add("gaming-chair/" + ids.get(0));
            items.add("hypercar-toy/" + ids.get(1));
            items.add("PS5-game/" + ids.get(2));
            items.add("monke-NFT/" + ids.get(3));

            initPrices.put(items.get(0), 75.0);
            initPrices.put(items.get(1), 10.0);
            initPrices.put(items.get(2), 60.0);
            initPrices.put(items.get(3), 100.0);
        }

        Collections.shuffle(items);

        try {
            port = Integer.parseInt(args[0]);

            if (port < 0) throw new Exception("Port value must be positive");
        } catch (ArrayIndexOutOfBoundsException exc) {
            System.err.println("Command line args missing");
            System.exit(1);
        } catch (NumberFormatException exc) {
            System.err.println("Invalid port value provided");
            System.exit(1);
        } catch (Exception exc) {
            System.err.println(exc.getMessage());
            System.exit(1);
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("Provide your auction's name below:");
        auctionName = scanner.nextLine();

        String[] array = auctionName.split(" ");
        if (array.length > 1) {
            auctionName = "";

            for (int a = 0; a < array.length - 1; a++) {
                auctionName += array[a] + "-";
            }

            auctionName += array[array.length - 1];
        }

        System.out.println("Provide the bank's host name below:");
        String bankHost = scanner.nextLine();

        System.out.println("Provide the bank's port number below:");
        int bankPort = askPort(scanner);

        System.out.println("Connecting to the bank...");

        try {
            // set up bank communication
            BankConnection bankComm = new BankConnection(bankHost, bankPort);
            Thread t3 = new Thread(bankComm);
            t3.start();


            // start auction server
            Server server = new Server(port);
            Thread t1 = new Thread(server);
            t1.start();

            // start bid manager
            BidManager bidManager = new BidManager();
            Thread t2 = new Thread(bidManager);
            t2.start();

            TimeUnit.SECONDS.sleep(1);

            // start cmd line menu
            System.out.println("Auction is open for business now");
            System.out.println("WARNING: Do not exit this program through " +
                    "any means other than the in-program options menu");
            Menu menu = new Menu(scanner);
            Thread t4 = new Thread(menu);
            t4.start();
        } catch (IOException exc) {
            System.err.println("Something went wrong. Double check that all " +
                    "host names and port numbers you provided are correct, " +
                    "then try again.");
            exc.printStackTrace();
            System.exit(2);
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Fetches the starting big price of an auction house item
     * @param itemName Name of the item
     * @return Starting bid price as a double value
     */
    protected static double getInitPrice(String itemName) {
        return initPrices.get(itemName);
    }

    /**
     * @return List of items which the auction house will sell (only the
     * first 3 items are currently for sale)
     */
    protected static synchronized List<String> getItems() {
        return items;
    }

    /**
     * @return Maximum amount of auctions which can occur at the same time
     * (usually 3)
     */
    protected static int getMaxConcurrentSales() {
        return maxItems;
    }

    /**
     * @return Port number which the auction house server is listening on
     */
    protected static int getPort(){
        return port;
    }

    /**
     * @return Name of this auction house
     */
    protected static String getName() {
        return auctionName;
    }

    /**
     * Prompts the user to enter the bank's port number, then fetches the
     * response from STDIN and checks that it is legitimate
     * @param userInput Scanner object used to retrieve user input
     * @return Port number which the bank server is listening on
     */
    private static int askPort(Scanner userInput) {
        try {
            String reply = userInput.nextLine();
            int out = Integer.parseInt(reply);

            if (out > 0) return out;
            else throw new Exception("Number must be positive");
        } catch (NumberFormatException exc) {
            System.err.println("Not a number, please try again.");
            return askPort(userInput);
        } catch (Exception exc) {
            System.err.println(exc.getMessage() + ", please try again.");
            return askPort(userInput);
        }
    }
}

