package agent;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * An object that handles the connection to the bank. Stores auction info in an auction object.
 * Uses an ArrayBlockingQueue for message processing
 */
public class BankConnection implements Runnable{
    private final Socket socket;
    private final BufferedReader bufferedReader;
    private final PrintWriter printWriter;
    private final BlockingQueue<String> inbox;
    private static boolean registered = false;


    public BankConnection(String hostName, int portNumber) throws Exception{
        socket = new Socket(hostName,portNumber);
        InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
        bufferedReader = new BufferedReader(inputStreamReader);
        printWriter = new PrintWriter(socket.getOutputStream(), true);
        inbox = new ArrayBlockingQueue<>(20);
    }

    @Override
    public void run() {
        while(!socket.isClosed()){
            try {
                Thread.sleep(100);
            }catch (InterruptedException ei){}

            try{
                inbox.add(bufferedReader.readLine());
            } catch (IOException e) {}
            catch (NullPointerException npe){
                System.out.println("The Bank ShutDown");
                System.exit(1);
            }

            processInbox();
        }
        System.exit(1);
    }
    private void processInbox(){
        while (!inbox.isEmpty()) {
            String message = inbox.poll();
            String[] args = message.split(" ");
            System.out.println("Message received from bank " + message);
           switch (args[0]) {
                //format: "Auctions hostName auctionName portNum ..."
                case "Auctions" -> {
                    ArrayList<Auction> auctions = new ArrayList<>();
                    for (int i = 1; i < args.length; i += 3) {
                        auctions.add(new Auction(args[i],args[i+1],Integer.parseInt(args[i+2])));
                    }
                    Platform.runLater(() -> Agent.updateAuctions(auctions));
                }
                //format: "Balances totalBalance withHeldFunds"
                case "Balances" -> {
                    Platform.runLater(() -> Agent.updateBalance(Double.parseDouble(args[1]), Double.parseDouble(args[2])));
                }
                case "SuccessfulLogin" -> {
                    registered = true;
                }
                case "FailedLogin" -> {
                    Platform.runLater(() -> Agent.failedLogin());
                }
                case "FailedReg" -> {
                    Platform.runLater(() -> Agent.failedReg());
                }
            }
        }
    }
    public static boolean isRegistered() {
        return registered;
    }

    protected void sendMessage(String message){
        printWriter.println(message);
    }
}
