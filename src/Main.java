import java.io.FileNotFoundException;

public class Main {

	public static void main(String[] args) {

		if (args.length != 2) {
			err_usage();
		}

		String config_file = "config.yaml";
		String local_name = "alice";

		MessagePasser mp = new MessagePasser(config_file, local_name);

		// cl runs the main interactive process
		ClientThread cl = new ClientThread(mp);
		cl.run();
	}

	private static void err_usage() {
		System.err.println("Usage: java Main.java <conf_file> <local_name>");
	}

}
