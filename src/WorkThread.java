import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by gs on 1/26/15.
 */
public class WorkThread implements Runnable{
    private Socket socket;
    private MessagePasser messagePasser;

    public WorkThread(Socket socket, MessagePasser messagePasser){
        this.socket = socket;
        this.messagePasser = messagePasser;
    }

    @Override
    public void run() {
        ObjectInputStream input = null;
        try {
            input = new ObjectInputStream(socket.getInputStream());
            while(true) {
                Message receivedMessage = (Message) input.readObject();
                if(!messagePasser.getSockets().containsKey(receivedMessage.src)){
                	messagePasser.addSockets(receivedMessage.src, socket);
                    messagePasser.addStream(receivedMessage.src, new ObjectOutputStream(socket.getOutputStream()));
                }
                messagePasser.receiveMessage(receivedMessage);

                Message processedMessage = this.messagePasser.receive();
                if(processedMessage != null)
                    System.out.println("Data: " + processedMessage.data + " SeqNum: " + processedMessage.seqNum
                            +" Duplicate: " + processedMessage.dup);
                processedMessage = this.messagePasser.receive();
                if(processedMessage != null)
                    System.out.println("Data: " + processedMessage.data + " SeqNum: " + processedMessage.seqNum
                            +" Duplicate: " + processedMessage.dup);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
