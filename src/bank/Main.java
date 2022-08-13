package bank;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Main {



    public static void main(String[] args) throws UnknownHostException, InterruptedException {
        System.out.println(args[0]);
        Bank bank = new Bank();
        Server server = new Server(Integer.parseInt(args[0]));
        System.out.println(InetAddress.getLocalHost().getHostAddress());
        Thread thread = new Thread(server);
        thread.start();
        while(true){
            Scanner sc = new Scanner(System.in);
            System.out.println("Quit [q]");
            String response = sc.nextLine();
            if (response.charAt(0) == 'q' || response.charAt(0) == 'Q'){
                Bank.close();
                server.close();
                System.exit(0);
            }
            else {
                Thread.sleep(100);
            }
        }
    }
}
