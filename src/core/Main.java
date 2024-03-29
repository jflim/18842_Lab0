package core;

public class Main{

	public static void main(String[] args) {

		if (args.length != 2) {
			err_usage();
		}

		String config_file = args[0];
		String local_name = args[1];

		MessagePasser mp = new MessagePasser(config_file, local_name, false, null);

		// cl runs the main interactive process
		ClientThread cl = new ClientThread(mp);
		cl.run();
	}

	private static void err_usage() {
		System.err.println("Usage: java Main.java <conf_file> <local_name>");
	}

}
