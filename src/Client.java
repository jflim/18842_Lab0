import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

/**
 * Created by gs on 1/26/15.
 */
public class Client {
    private String hostName;
    private HashMap<String, Socket> sockets;
    private HashMap<String, ObjectOutputStream> outputStreams;

//    public Client(String hostName){
//        this.hostName = hostName;
//
//    }

    public void send(){
        try {
            Socket clientSocket = new Socket("128.237.213.103", 12345);
            System.out.println("Connection setup!");
            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
