import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by gs on 1/26/15.
 */
public class WorkThread implements Runnable{
    private Socket socket;
    private MessagePasser messagePasser;
    private Logger logger;
    public WorkThread(Socket socket, MessagePasser messagePasser, Logger logger){
        this.socket = socket;
        this.messagePasser = messagePasser;
        this.logger = logger;
    }

	@Override
	public void run() {
		ObjectInputStream input = null;
		try {
			input = new ObjectInputStream(socket.getInputStream());
            Scanner scan = new Scanner(System.in);
			while (true) {

				messagePasser.receive(socket, input);

				// actually return the message content.
				TimeStampedMessage processedMessage = this.messagePasser.receiveMessage();
				while (processedMessage != null) {
					System.out.println("Data: " + processedMessage.data
							+ " SeqNum: " + processedMessage.seqNum
							+ " Duplicate: " + processedMessage.dup + " Timestamp: " + processedMessage.getTimeStamp());
                    System.out.println("Do you want to log this message? Y: N");

                    String line = scan.nextLine();
                    if(line.equalsIgnoreCase("Y"))
                        this.messagePasser.logger.logs.add(processedMessage);
                    messagePasser.getClock().setClock(processedMessage.getTimeStamp());
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
