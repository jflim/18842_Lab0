import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by gs on 1/26/15.
 */
public class ServerThread implements Runnable{
    private MessagePasser messagePasser;
    private int port;

    public ServerThread(MessagePasser messagePasser, int port) {
        this.messagePasser = messagePasser;
        this.port = port;
    }

    @Override
    public void run()
    {
        try {
            ServerSocket serverSocket= new ServerSocket(port);
            Socket socket;
            while (true) {
                socket = serverSocket.accept();
                Thread thread = new Thread(new WorkThread(socket, messagePasser));
                thread.start();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
