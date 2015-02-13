package Logger;

import java.util.*;

import Clock.ClockService;
import core.MessagePasser;
import core.TimeStampedMessage;

/**
 * Created by gs on 2/7/15.
 */
public class Logger {

	List<TimeStampedMessage> logs = Collections
			.synchronizedList(new LinkedList<TimeStampedMessage>());

	public static void main(String[] args) {
        Logger logger = new Logger();
		if (args.length != 2) {
			logger.err_usage();
		}

		System.out.println("Logger is turned on");
		String config_file = args[0];
		String local_name = args[1];

		MessagePasser mp = new MessagePasser(config_file, local_name, true,
				logger.logs);

		Scanner scan = new Scanner(System.in);
		while (true) {
			System.out.println("Command: ");
			String input = scan.nextLine();
			if (input.equals("print log")) {
				logger.displayLog();
			}
			else{
				System.out.println("Usage: print log");
			}
		}

	}

	private void displayLog() {
        Collections.sort(logs, new TimeStampedMessageComparator());
        Iterator<TimeStampedMessage> it = logs.iterator();
//        while (it.hasNext()) {
//            TimeStampedMessage x = it.next();
//            displayMessage(x);
//        }



        for (int i = 0; i < logs.size() - 1; i++) {
            for (int j = i + 1; j < logs.size(); j++) {
                if ((logs.get(i).getTimeStamp()).compareTo(logs.get(j).getTimeStamp()) == 0) {
                    System.out.println("\nConcurrent message:");
                    displayMessage(logs.get(i));
                    displayMessage(logs.get(j));
                    System.out.println("");

                }else{
                    System.out.println("\nMessage in sequence:");
                    displayMessage(logs.get(i));
                    displayMessage(logs.get(j));
                    System.out.println("");
                }
            }

        }
    }

	private void displayMessage(TimeStampedMessage processedMessage){
		System.out.println("Src: " + processedMessage.get_source()
                + " Dest: " + processedMessage.get_dst()
                + " Data: " + processedMessage.getData()
				+ " SeqNum: " + processedMessage.getSeqNum()
				+ " Duplicate: " + processedMessage.getDup()
				+ " Timestamp: "
				+ processedMessage.getTimeStamp());
	}
	
	private void err_usage() {
		System.err.println("Usage: java Logger.java <conf_file> <local_name>");
	}

    public class TimeStampedMessageComparator implements Comparator<TimeStampedMessage> {
        @Override
        public int compare(TimeStampedMessage m1, TimeStampedMessage m2) {
            ClockService clock1 = m1.getTimeStamp();
            ClockService clock2 = m2.getTimeStamp();
            int result = clock1.compareTo(clock2);
            return result;
        }
    }

}
