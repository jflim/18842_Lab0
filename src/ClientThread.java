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
        	System.out.println("Enter your command: send [kind] [target_node]");
        	String line = scan.nextLine();
        	String[] tmpline = line.split("\\s+"); 
        	
        	String command = tmpline[0];

        	if(command.equalsIgnoreCase("send")){
        		if(tmpline.length != 3){
            		print_error("Enter your command: send [kind] [target_node]");
            		continue;
            	}
            	String kind = tmpline[1];
            	String target = tmpline[2];
        		System.out.println("Content: ");
        		String content = scan.nextLine();
        		Message m = new Message(target, kind, content);
        		messagePasser.sendMessage(m);
        		
        	}
        	else if(command.equalsIgnoreCase("exit")){
        		return;
        	}
        	else{
        		print_error("Enter your command: send [kind] [target_node]");
        		continue;
        	}
        	System.out.println(command);
        }
    }

    /**
     * Error printing function
     * @param string
     */
	private void print_error(String string) {
		System.err.println(string);
	}
}
