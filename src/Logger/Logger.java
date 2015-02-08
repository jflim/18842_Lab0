package Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import core.MessagePasser;
import core.TimeStampedMessage;

/**
 * Created by gs on 2/7/15.
 */
public class Logger {

	static List<TimeStampedMessage> logs = Collections
			.synchronizedList(new LinkedList<TimeStampedMessage>());

	public static void main(String[] args) {

		if (args.length != 2) {
			err_usage();
		}

		System.out.println("Logger is turned on");
		String config_file = args[0];
		String local_name = args[1];
		MessagePasser mp = new MessagePasser(config_file, local_name, true,
				logs);

		Scanner scan = new Scanner(System.in);
		while (true) {
			String input = scan.nextLine();
			if (input.equals("print log")) {
				displayLog();
			}
		}

	}

	private static void displayLog() {
		Iterator<TimeStampedMessage> it = logs.iterator();
		while(it.hasNext()){
			TimeStampedMessage x = it.next();
			displayMessage(x);
		}

	}

	private static void displayMessage(TimeStampedMessage processedMessage){
		System.out.println("Data: " + processedMessage.getData()
				+ " SeqNum: " + processedMessage.getSeqNum()
				+ " Duplicate: " + processedMessage.getDup()
				+ " Timestamp: "
				+ processedMessage.getTimeStamp());
	}
	
	private static void err_usage() {
		System.err.println("Usage: java Logger.java <conf_file> <local_name>");
	}

}
