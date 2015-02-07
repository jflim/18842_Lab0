import java.io.EOFException;
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
			while (true) {

				messagePasser.receive(socket, input);

				// actually return the message content.
				Message processedMessage = this.messagePasser.receiveMessage();
				while (processedMessage != null) {
					System.out.println("Data: " + processedMessage.data
							+ " SeqNum: " + processedMessage.seqNum
							+ " Duplicate: " + processedMessage.dup);
					processedMessage = this.messagePasser.receiveMessage();
				}
			}
		} catch (IOException e) {

			try {
				input.close();
				String socketName = messagePasser.removeInputStream(input);
				messagePasser.removeSocket(socketName);
				return;
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
