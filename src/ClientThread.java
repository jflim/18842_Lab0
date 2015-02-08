import Clock.ClockService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by gs on 1/27/15.
 */
public class ClientThread implements Runnable{


    private MessagePasser messagePasser;
    private int port;

    public ClientThread(MessagePasser messagePasser) {
        this.messagePasser = messagePasser;
    }

    @Override
    public void run()
    {
    	Scanner scan = new Scanner(System.in);
    	
        while(true){
        	usage();
        	String line = scan.nextLine();
        	String[] tmpline = line.split("\\s+");
            ClockService clock;
        	String command = tmpline[0];

        	if(command.equalsIgnoreCase("send")){
        		if(tmpline.length != 3){
            		continue;
            	}
            	String kind = tmpline[1];
            	String target = tmpline[2];
        		System.out.println("Enter the message content: ");
        		String content = scan.nextLine();
        		TimeStampedMessage m = new TimeStampedMessage(target, kind, content, messagePasser.getClock());
                try {
                    m.set_source(messagePasser.local_name);
                    m.set_seqNum(messagePasser.seqNum++);
                    messagePasser.send(m);

                    System.out.println("Do you want to log this message? Y: N");//log message
                    line = scan.nextLine();
                    if(line.equalsIgnoreCase("Y")){
                        m.set_dst("logger");
                        messagePasser.send(m);
                    }

                    messagePasser.getClock().clockIncrement(); //clock increment
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }
        	else if(command.equalsIgnoreCase("exit")){
                System.exit(0);
        		return;
        	}
        	else if(command.equalsIgnoreCase("help")){
        		usage();
        	}
            else if(command.equalsIgnoreCase("time")){
                int m = (Integer)messagePasser.getClock().getClock();
                System.out.println("Time: " + m);
            }

        }
    }

    /**
     * Error printing function
     */
	private void usage() {
		System.out.println("Usage:");
		System.out.println("send [kind] [target node]");
		System.out.println("help -display this help message");
		System.out.println("exit -exit the program");
	}
}
