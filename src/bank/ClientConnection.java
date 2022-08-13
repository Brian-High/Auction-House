package bank;
import javafx.util.Pair;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ClientConnection implements Runnable{
    private Socket client;
    private int clientID = -1;
    private String clientName;
    private final PrintWriter toClient;
    private final BufferedReader fromClient;
    private final BlockingQueue<String> inbox;
    public ClientConnection(Socket client) throws IOException{
        this.client = client;
        System.out.println(client.getInetAddress().getHostAddress());
        toClient = new PrintWriter(client.getOutputStream(),true);
        InputStreamReader isr =
                new InputStreamReader(client.getInputStream());
        fromClient = new BufferedReader(isr);
        inbox = new ArrayBlockingQueue<>(100);
    }

    @Override
    public void run() {
        String input, output;
        while (!client.isClosed()) {
            try {
                Thread.sleep(100);
            }catch (InterruptedException ei){}

            try {
                input = fromClient.readLine();
            } catch (IOException e) {
                input = null;
            }

            if (input != null) {
                output = processClientRequest(input);

                if (output != null) {
                    toClient.println(output);
                    output = null;
                }
            }

            if (!inbox.isEmpty()) {
                toClient.println(inbox.poll());
            }
        }


    }

    public synchronized String processClientRequest(String request){
        String status = "";

        String[] args = request.split(" ");
        System.out.println("Message received from client " + request);

        switch (args[0]){
            case "reqHold" -> {
                boolean success = true;
                success = success == Bank.hold(Integer.parseInt(args[1]), Double.parseDouble(args[2]) * -1, Integer.parseInt(args[3]));
                if(success) {
                    Bank.hold(clientID, Double.parseDouble(args[2]), Integer.parseInt(args[3]));
                    System.out.println("holdSuccessful " + args[1] + " " + args[2] + " " + args[3]);
                    toClient.println("holdSuccessful " + args[1] + " " + args[2] + " " + args[3]);
                } else {
                    toClient.println("holdFailed " + args[1] + " " + args[3]);
                }
            }
            case "ReturningUser" -> {

                int work = 0;
                for(Account account: Bank.bank.values()){
                    if(account.User().equals(args[1]) && account.ID() == Integer.parseInt(args[2])){
                        clientID = Integer.parseInt(args[2]);
                        clientName = args[1];
                        toClient.println("SuccessfulLogin");
                        if (account.Type() == 0){
                            Bank.activeClients.put(account.ID(), toClient);
                        }

                        work ++;
                    }
                }
                if (work == 0){
                    toClient.println("FailedLogin");
                }
            }
            case "CheckBalance" -> {
                double holdAmt = 0;
                for (Double hold:
                        Bank.bank.get(clientID).Holds().values()) {
                    holdAmt += hold;
                }
                toClient.println("Balances " + Bank.bank.get(clientID).Account() + " " + holdAmt);
            }
            case "open" -> {
                Pair<String, Integer> auction = new Pair<>(client.getInetAddress().getHostAddress(), Integer.parseInt(args[1]));
                Bank.availableAuctions.put(auction, clientName);
                for(PrintWriter output: Bank.activeClients.values()){
                    output.print("Auctions ");
                    for (Pair<String, Integer> auctionHouse: Bank.availableAuctions.keySet()){
                        output.print(auctionHouse.getKey() + " " + Bank.availableAuctions.get(auctionHouse) + " " + String.valueOf(auctionHouse.getValue()) + " " );
                    }
                    output.println();
                }
                processClientRequest("Auctions");
            }
            case "Auctions" -> {
                toClient.print("Auctions");
                for (Pair<String, Integer> auctionHouse: Bank.availableAuctions.keySet()){
                    toClient.print(" ");
                    toClient.print(auctionHouse.getKey() + " " + Bank.availableAuctions.get(auctionHouse) + " " + String.valueOf(auctionHouse.getValue()) );
                }
                toClient.println();

            }
            case "removeHold" -> {
                Bank.removeHold(Integer.parseInt(args[1]), Integer.parseInt(args[3]));
                Bank.removeHold(clientID, Integer.parseInt(args[3]));
            }
            case "itemWon" -> {
                Bank.pushTransfer(clientID, Integer.parseInt(args[1]));
                Bank.pushTransfer(Integer.parseInt(args[2]), Integer.parseInt(args[1]));
                double holdAmt = 0;
                for (Double hold:
                        Bank.bank.get(Integer.parseInt(args[2])).Holds().values()) {
                    holdAmt += hold;
                }
                Bank.activeClients.get(Integer.parseInt(args[2])).println(
                        "Balances " + Bank.bank.get(Integer.parseInt(args[2])).Account() + " " + holdAmt);

            }
            case "registerAuction" -> {
                int repeat = 0;
                for(Account accounts: Bank.bank.values()){
                    if (accounts.User().equals(args[1]) || accounts.ID() == Integer.parseInt(args[2])){
                        repeat++;
                    }
                }
                if (repeat == 0){
                    Bank.createAuctionHouseUser(args[1], Integer.parseInt(args[2]));
                    clientName = args[1];
                    clientID = Integer.parseInt(args[2]);
                    toClient.println("SuccessfulLogin");
                } else {
                    toClient.println("FailedReg");
                }


            }
            case "registerAgent" -> {
                int repeat = 0;
                for(Account accounts: Bank.bank.values()){
                    if (accounts.User().equals(args[1]) || accounts.ID() == Integer.parseInt(args[2])){
                        repeat++;
                    }
                }
                if (repeat == 0){
                    Bank.createHumanUser(args[1], Integer.parseInt(args[2]));
                    clientName = args[1];
                    clientID = Integer.parseInt(args[2]);
                    toClient.println("SuccessfulLogin");
                } else {
                    toClient.println("FailedReg");
                }

            }
            case "deposit" -> {
                Bank.deposit(Integer.parseInt(args[1]),Double.parseDouble(args[2]));
                double holdAmt = 0;
                for (Double hold:
                        Bank.bank.get(clientID).Holds().values()) {
                    holdAmt += hold;
                }
                toClient.println("Balances " + Bank.bank.get(clientID).Account() + " " + holdAmt);
            }
            case "withdraw" -> {
                Bank.withdraw(Integer.parseInt(args[1]),Double.parseDouble(args[2]));
                double holdAmt = 0;
                for (Double hold:
                        Bank.bank.get(clientID).Holds().values()) {
                    holdAmt += hold;
                }
                toClient.println("Balances " + Bank.bank.get(clientID).Account() + " " + holdAmt);
            }
        }
        return status;
    }

    public void close(){
        try {
            toClient.close();
            fromClient.close();
            client.close();
        } catch (Exception e){

        }
    }
}
