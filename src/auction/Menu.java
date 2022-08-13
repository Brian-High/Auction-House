package auction;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Command line menu used to control the auction house during runtime.
 * Can be used to log in with bank, check bank account balance of the
 * auction house, check current auction house status, and close the auction
 * house if possible.
 */
public class Menu implements Runnable {
    private boolean run = true;
    private final Scanner scanner;
    private static final BlockingQueue<Double> inbox =
            new ArrayBlockingQueue<>(10);

    /**
     * Creates a new menu object with the given parameters
     * @param userIn Scanner object used to fetch input form the user
     */
    public Menu(Scanner userIn) {
        scanner = userIn;
    }

    /**
     * Prints options menu, fetches user selection, then processes the request
     */
    @Override
    public void run() {
        String input;

        while (run) {
            System.out.println("Auction House Menu");
            if (BankConnection.isRegistered())
                System.out.println("[B] Check Balance");
            else System.out.println("[L] Login to Bank");
            System.out.println("[S] Check Status");
            System.out.println("[Q] Close Auction");

            input = scanner.nextLine();

            switch (input.toLowerCase()) {
                case "b" -> {
                    if (BankConnection.isRegistered()) {
                        BankConnection.sendMessage("CheckBalance");

                        int incr = 0, bool = 0;
                        while (bool == 0 && incr < 30) {
                            if (!inbox.isEmpty()) {
                                System.out.println("Current Balance: $" +
                                        inbox.poll());
                                bool = 1;
                            } else {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException exc) {
                                    Thread.currentThread().interrupt();
                                }

                                incr++;
                            }
                        }

                        if (incr >= 30) {
                            System.err.println("Unable to fetch balance: " +
                                    "request timed out");
                        }
                    } else
                        System.err.println(input +
                                " is not a valid option");
                } case "l" -> {
                    if (!BankConnection.isRegistered()) {
                        System.out.println("Please select a login option:");
                        System.out.println("[N] New User");
                        System.out.println("[R] Returning User");

                        String userSelect = scanner.nextLine();
                        String toBank = processLogin(userSelect);
                        BankConnection.sendMessage(toBank);

                        int incr = 0, bool = 0;
                        while (bool == 0 && incr < 30) {
                            if (BankConnection.isRegistered()) {
                                System.out.println("Login successful");
                                bool = 1;
                            } else {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException exc) {
                                    Thread.currentThread().interrupt();
                                }

                                incr++;
                            }
                        }

                        if (incr >= 30) {
                            System.err.println("Unable to login with bank: " +
                                    "request timed out");
                        }
                    } else
                        System.err.println(input +
                                " is not a valid option");
                }
                case "s" -> {
                    String reg = BankConnection.isRegistered() ? "Y" : "N";
                    System.out.println("Registered w/ bank: " + reg);
                    int sz = Auction.getItems().size();
                    System.out.println("Items Remaining: " + sz);
                    int max4Sale = Auction.getMaxConcurrentSales();
                    int curr4Sale = Math.min(max4Sale, sz);
                    System.out.println("Current Items for Sale: " +
                            curr4Sale);
                    String bidding = BidManager.hasUnresolvedBids() ?
                            "Y" : "N";
                    System.out.println("Bidding in Progress: " + bidding);
                }
                case "q" -> {
                    if (!BidManager.hasUnresolvedBids()) {
                        run = false;

                        try {
                            Server.close();
                            BankConnection.close();
                        } catch (IOException exc) {
                            System.err.println("An error occurred while" +
                                    " closing: ");
                            System.err.print(exc.getMessage());
                        }

                        System.out.println("Closing auction house...");
                        System.exit(0);
                    } else
                        System.err.println("Unable to quit program: " +
                            "there are auctions in progress");
                }
                default -> System.err.println(input +
                        " is not a valid option");
            }

            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException exc) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Use this to send the auction house account's current balance after
     * a check balance request by the user
     * @param balance Current balance of auctions house bank account
     */
    protected static void sendBalance(double balance) {
        inbox.add(balance);
    }

    /**
     * Helper method to process bank account login request from the user
     * @param option Login option selected by the user ("n" for new user,
     *               "r" for returning user)
     * @return Login request message to be sent to the bank
     */
    private String processLogin(String option) {
        String out = "";

        switch (option.toLowerCase()) {
            case "n" -> {
                // this need to be changes to auction_name auction_ID which would be preferably not hardcoded
                //port is sent later in "open"
                out += "registerAuction " + Auction.getName();

                System.out.println("Provide a 4-digit PIN: " +
                        "(write it down for future logins)");
                out += " " + askPIN();

                System.out.println("Attempting login as new user...");
            }
            case "r" -> {
                out += "ReturningUser " + Auction.getName();

                System.out.println("Please enter your PIN below:");
                out += " " + askPIN();

                System.out.println("Attempting login as returning user...");
            }
        }

        return out;
    }

    /**
     * Fetches the account PIN for the auction house to login to its bank
     * account using STDIN
     * @return PIN number as an integer value
     */
    private int askPIN() {
        String pin = scanner.nextLine();
        int out;

        try {
            if (pin.length() != 4)
                throw new Exception("PIN must be 4 numbers");

            out = Integer.parseInt(pin);

            if (out < 0) throw new Exception("PIN must be positive");
        } catch (NumberFormatException exc) {
            System.err.println("Not a number, please try again.");
            return askPIN();
        } catch (Exception exc) {
            System.err.println(exc.getMessage() + ", please try again.");
            return askPIN();
        }

        return out;
    }
}
