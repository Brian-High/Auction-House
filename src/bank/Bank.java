package bank;

import javafx.util.Pair;

import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class Bank {
    protected static ConcurrentHashMap<Integer, Account> bank = new ConcurrentHashMap<>();
    protected static HashMap<Pair<String, Integer>, String> availableAuctions = new HashMap<>();
    protected static HashMap<Integer, PrintWriter> activeClients = new HashMap<>();
    public Bank(){
        try {
            Scanner sc = new Scanner(new File("src/bank/users.txt"));
            sc.nextLine();
            while (sc.hasNext()){
                String line = sc.nextLine();
                String[] inputs = line.split(", ");
                Account account = new Account(inputs[0], Double.parseDouble(inputs[3]), Integer.parseInt(inputs[1]), Integer.parseInt(inputs[2]), new HashMap<>());
                bank.put(Integer.parseInt(inputs[1]), account);
                System.out.println(account);
            }
            sc.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }
    }

    /**
     * This function creates an account record for a new human user with name User and an identification n umber ID
     * @param user the users picked username
     * @param ID the ID of the user
     */

    public static void createHumanUser(String user, int ID){
        Account account = new Account(user, 0, ID, 0, new HashMap<>());
        bank.put(ID, account);
    }

    /**
     * This function creates an account record for a new auction house user with name User and an identification n umber
     * ID
     * @param user the users picked username
     * @param ID the ID of the user
     */

    public static void createAuctionHouseUser(String user, int ID){
        Account account = new Account(user, 0, ID, 1, new HashMap<>());
        bank.put(ID, account);
    }

    /**
     * This function withdraws an amount of funds from the user with associated ID
     * @param ID of user whose funds are withdrawn
     * @param amount of money withdrawn from the account.
     * @return if the user does not have the funds to withdraw amount then false will be returned
     */

    public static boolean withdraw(int ID, double amount){
        Account tempAccount = bank.get(ID);
        if (tempAccount.Account() > amount){
            bank.remove(ID);
            Account account = new Account(tempAccount.User(), tempAccount.Account() - amount, ID, tempAccount.Type(), tempAccount.Holds());
            bank.put(ID, account);
            return true;
        } else {
            System.out.println("Not enough funds");
            return false;
        }

    }

    /**
     * This function deposits an amount of funds to the user with associated ID
     * @param ID of user where funds are deposited
     * @param amount of money deposited to the account.
     * @return always true
     */

    public static boolean deposit(int ID, double amount){
        Account tempAccount = bank.get(ID);
        bank.remove(ID);
        Account account = new Account(tempAccount.User(), tempAccount.Account() + amount, ID, tempAccount.Type(), tempAccount.Holds());
        bank.put(ID, account);
        return true;


    }

    /**
     * This function creates a new hold on a users account (with ID) for  an amount and an associated item id for
     * purchase
     * @param ID of user whose hold is placed
     * @param amount amount of money on the hold
     * @param itemID the ID of the item that is being purchased with the amount
     * @return false if there is not sufficient funds or the amount of total holds is over the users account balance.
     */

    public static boolean hold(int ID, double amount, int itemID){
        Account tempAccount = bank.get(ID);
        double totalHolds = 0;
        for(Double hold: tempAccount.Holds().values()){
            totalHolds = totalHolds +hold;
        }
        if (0 < tempAccount.Account() + amount + totalHolds){
            bank.remove(ID);
            tempAccount.Holds().put(itemID, amount);
            Account account = new Account(tempAccount.User(), tempAccount.Account(), ID, tempAccount.Type(),tempAccount.Holds());
            bank.put(ID, account);
            return true;
        } else {
            System.out.println("Not enough funds");
            return false;
        }

    }

    /**
     * This function removes a hold on an account (does not process funds)
     * @param ID of user whose hold is being removed.
     * @param itemID of transaction hold that must be removed.
     * @return always true
     */

    public static boolean removeHold(int ID, int itemID){
        Account tempAccount = bank.get(ID);
        tempAccount.Holds().remove(itemID);
        bank.remove(ID);
        Account account = new Account(tempAccount.User(), tempAccount.Account(), ID, tempAccount.Type(),tempAccount.Holds());
        bank.put(ID, account);
        return true;

    }

    /**
     * This function pushes funds that are held on an account.
     * @param ID of user whose funds are pushed
     * @param itemID item id of transaction
     * @return
     */

    public static boolean pushTransfer(int ID, int itemID){
        Account tempAccount = bank.get(ID);
        double tempPrice = tempAccount.Holds().get(itemID);
        bank.remove(ID);
        tempAccount.Holds().remove(itemID);
        Account account = new Account(tempAccount.User(), tempAccount.Account() + tempPrice, ID, tempAccount.Type(), tempAccount.Holds());
        bank.put(ID, account);
        return true;
    }
    public synchronized static void close(){
        try {
            File oldAccounts = new File("src/bank/users.txt");
            oldAccounts.delete();
            File newAccounts = new File("src/bank/users.txt");
            FileWriter docUpdate = new FileWriter(newAccounts);
            docUpdate.write("Users, ID, Type, Amount\n");
            for(Account account: bank.values()){
                docUpdate.write(account.User() + ", " + account.ID() + ", " + account.Type() + ", " + account.Account() + "\n");

            }
            docUpdate.close();


        } catch (Exception e){

        }
    }
}
