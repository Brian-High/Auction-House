package bank;

import javax.imageio.IIOException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server implements Runnable {
    ServerSocket serverSocket;
    ArrayList<ClientConnection> clientList = new ArrayList<>();
    public Server(int port) {
        try{
            serverSocket = new ServerSocket(port);
            System.out.println(serverSocket.getLocalPort());
        } catch (IIOException e){} catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        while (true){
            try {
                Socket clients = serverSocket.accept();
                ClientConnection cc = new ClientConnection(clients);
                clientList.add(cc);
                Thread thread = new Thread(cc);
                thread.start();
            } catch (IOException e) {}



        }

    }
    public void close(){
        for(ClientConnection client: clientList){
            client.close();
        }
    }
}
