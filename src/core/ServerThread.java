package core;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

/**
 * Created by gs on 1/26/15.
 */
public class ServerThread implements Runnable{
    private MessagePasser messagePasser;
    private int port;
    private boolean isLogger;
    List<TimeStampedMessage> logs;

    public ServerThread(MessagePasser messagePasser, int port, boolean isLogger, List<TimeStampedMessage> logs) {
        this.messagePasser = messagePasser;
        this.port = port;
        this.isLogger = isLogger;
        this.logs = logs;
    }

    @Override
    public void run()
    {
        try {
            ServerSocket serverSocket= new ServerSocket(port);
            Socket socket;
            while (true) {
                socket = serverSocket.accept();
                Thread thread = new Thread(new WorkThread(socket, messagePasser, isLogger, logs));
                thread.start();
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
