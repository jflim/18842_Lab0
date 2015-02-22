package core;

import Clock.ClockService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import Multicast.MulticastService.State;
/**
 * Created by gs on 1/27/15.
 */
public class ClientThread implements Runnable {

	private MessagePasser messagePasser;
	private int port;
	public ClientThread(MessagePasser messagePasser) {
		this.messagePasser = messagePasser;
	}

	@Override
	public void run() {
		Scanner scan = new Scanner(System.in);
		usage();
		while (true) {
			printPrompt();
			String line = scan.nextLine();
			String[] tmpline = line.split("\\s+");
			ClockService clock;
			String command = tmpline[0];

			if (command.equalsIgnoreCase("send")) {
				if (tmpline.length != 3) {
					continue;
				}
				String kind = tmpline[1];
				String target = tmpline[2];
				System.out.println("Enter the message content: ");
				String content = scan.nextLine();

				TimeStampedMessage m = new TimeStampedMessage(target, kind,
						content, messagePasser.getClock().copy());
				m.set_source(messagePasser.local_name);
				m.set_seqNum(messagePasser.seqNum++);
				messagePasser.send(m);

				System.out.println("Do you want to log this message? Y: N");// log
																			// message
				line = scan.nextLine();
				if (line.equalsIgnoreCase("Y")) {
					messagePasser.sendMessageToLogger(m);
				}

			} else if (command.equalsIgnoreCase("exit")) {
				System.exit(0);
				return;
			} else if (command.equalsIgnoreCase("help")) {
				usage();
			} else if (command.equalsIgnoreCase("time")) {

				System.out.println(messagePasser.getClock());
			}

			else if (command.equalsIgnoreCase("generate")) {
				System.out.println(messagePasser.incrementClock());
			}
			
			else if(command.equalsIgnoreCase("displayDelivered")){
				messagePasser.multicastService.displayDelivered();
			}

			else if (command.equalsIgnoreCase("multicast")) {
				if (tmpline.length != 3) {
					continue;
				}
				String kind = tmpline[1];
				String groupName = tmpline[2];
                if(!messagePasser.getGroups().containsKey(groupName))
                    continue;

				System.out.println("Enter the message content: ");
				String content = scan.nextLine();

				TimeStampedMessage m = new TimeStampedMessage("", kind,
						content, messagePasser.getClock().copy(), groupName);
				try {
					m.set_source(messagePasser.local_name);
					m.set_seqNum(messagePasser.seqNum++);
					System.out.println(m.get_source());
					messagePasser.multicastService.send_multicast(groupName, m, false);
//
//					System.out.println("Do you want to log this message? Y: N");// log
//					// message
//					line = scan.nextLine();
//					if (line.equalsIgnoreCase("Y")) {
//						messagePasser.sendMessageToLogger(m);
//					}

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

			}
            else if(command.equalsIgnoreCase("request")){
                messagePasser.multicastService.state = State.WANTED;
                TimeStampedMessage m = new TimeStampedMessage("", "request",
                        "", messagePasser.getClock().copy(), messagePasser.getLocal_group_name());
                try {
                    m.set_source(messagePasser.local_name);
                    m.set_seqNum(messagePasser.seqNum++);
                    System.out.println(m.get_source());
                    messagePasser.multicastService.send_multicast(messagePasser.getLocal_group_name(), m, false);


                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                while(messagePasser.numOfReplies < messagePasser.getGroups().get(messagePasser.getLocal_group_name())){
                }
                messagePasser.numOfReplies = 0;
                messagePasser.multicastService.state = State.HELD;

            }
            else if(command.equalsIgnoreCase("release")){
                messagePasser.multicastService.state = State.RELEASED;
                TimeStampedMessage m = new TimeStampedMessage("", "release",
                        "", messagePasser.getClock().copy(), messagePasser.getLocal_group_name());
                try {
                    m.set_source(messagePasser.local_name);
                    m.set_seqNum(messagePasser.seqNum++);
                    System.out.println(m.get_source());
                    messagePasser.multicastService.send_multicast(messagePasser.getLocal_group_name(), m, false);


                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

            }
		}
	}

	private void printPrompt() {
		System.out.print("\n% ");

	}

	/**
	 * Error printing function
	 */
	private void usage() {
		System.out.println();
		System.out.println("Usage:");
		System.out.println("send [kind] [target node]");
		System.out.println("help -display this help message");
		System.out.println("exit -exit the program");
		System.out.println("time -display the current timestamps");
		System.out.println("generate -increment timestamp and display");
		System.out.println("multicast [kind] [group] -multicast a message to a group");
        System.out.println("request -request resources");
        System.out.println("release -release resources");

    }
	
	/**
	 * Request the critical section
	 */
	private void requestCS(){
		
	}
	
	/**
	 * Exit from a critical section
	 */
	private void exitCS(){
	
	}
	
	
}
