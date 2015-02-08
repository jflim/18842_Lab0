import java.util.LinkedList;
import java.util.List;

/**
 * Created by gs on 2/7/15.
 */
public class Logger {
    List<TimeStampedMessage> logs = new LinkedList<TimeStampedMessage>();


    public static void main(String[] args) {

        if (args.length != 2) {
            err_usage();
        }

        String config_file = args[0];
        String local_name = args[1];
        MessagePasser mp = new MessagePasser(config_file, local_name);

        // cl runs the main interactive process
        ClientThread cl = new ClientThread(mp);
        cl.run();

        

    }

    private static void err_usage() {
        System.err.println("Usage: java Main.java <conf_file> <local_name>");
    }

}