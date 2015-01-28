import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * Created by gs on 1/26/15.
 */

public class MessagePasser {

	public enum ActionType {
		Drop, // any message will be ignored/dropped
		Duplicate, // duplicate depending on the side.
		Delay; // set message aside and send after non-delay message is sent
	}

	public enum RuleOption {
		src, dest, kind, seqNum, duplicate;
	}

	/* class variables */
	String configuration_filename;
	String local_name;
    private HashMap<String, Socket> sockets;
    private HashMap<String, ObjectOutputStream> outputStreams;

	// node configuration
	Map<String, Node> nodes;

	// Send rule list
	ArrayList<LinkedHashMap<String, Object>> sendRules;

	// Receive rule list
	ArrayList<LinkedHashMap<String, Object>> receiveRules;

	class Node {
		String name;
		String ip;
		int port;

		Node(String name, String ip, int port) {
			this.name = name;
			this.ip = ip;
			this.port = port;
		}
	}

	public MessagePasser(String configuration_filename, String local_name) {
		this.configuration_filename = configuration_filename;
		this.local_name = local_name;
		nodes = new HashMap<String, Node>();

		
       	try {
       		parseConfig();
            setUp();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	/**
	 * parse the configuration file and store the elements into local Java data
	 * structures.
	 * 
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public void parseConfig() throws FileNotFoundException {
		InputStream input = new FileInputStream(
				new File(configuration_filename));
		Yaml yaml = new Yaml();
		Object data = yaml.load(input);

		Map<String, ArrayList<LinkedHashMap<String, Object>>> m;
		m = (Map<String, ArrayList<LinkedHashMap<String, Object>>>) data;
		if (m.containsKey("configuration")) {
			parseNodes(m.get("configuration"));
		}
		if (m.containsKey("sendRules")) {
			sendRules = m.get("sendRules");
		}
		if (m.containsKey("receiveRules")) {
			receiveRules = m.get("receiveRules");
		}
	}

	/**
	 * Convert nodes from YAML format to Java objects and add nodes to a HashMap
	 * 
	 * @param arrayList
	 */
	public void parseNodes(ArrayList<LinkedHashMap<String, Object>> arrayList) {
		for (LinkedHashMap<String, Object> node : arrayList) {
			String name = (String) node.get("name");
			String ip = (String) node.get("ip");
			int port = (int) node.get("port");
			Node n = new Node(name, ip, port);
			nodes.put(name, n);
		}
	}

	/**
	 * Check an outgoing message with the sending rules. First rule that matches
	 * is applied to the message. Called by MessagePasser.send()
	 */
	public void checkSendRules(Message message) {
		for (LinkedHashMap<String, Object> rule : sendRules) {		
			checkRule(message, rule);
		}
	}
	

	/**
	 * Check an incoming message with the receive rules. First rule that matches
	 * is applied to the message.
	 */
	public void checkReceiveRules(Message message) {
		for (LinkedHashMap<String, Object> rule : receiveRules) {		
			checkRule(message, rule);
		}
	}

	/**
	 * Checks if a rule matches, and calls another function
	 * to apply the rule.
	 * @param message
	 * @param rule
	 */
	private void checkRule(Message message, LinkedHashMap<String, Object> rule) {
		// check all the rule entries with message attributes

		if (rule.containsKey("src") && 
				!((String) rule.get("src")).equalsIgnoreCase(message.src)) {
			return;
		}

		if (rule.containsKey("dest") && 
				!((String) rule.get("dest")).equalsIgnoreCase(message.dest)) {
			return;
		}
		if (rule.containsKey("kind") && 
				!((String) rule.get("kind")).equalsIgnoreCase(message.kind)) {
			return;
		}
		if (rule.containsKey("seqNum") && 
				((int) rule.get("seqNum") == message.seqNum)) {
			return;
		}

		if (rule.containsKey("duplicate") && 
				(rule.get("duplicate") == message.dup)) {
			return;
		}
		
		processRule((String) rule.get("action"), message);
	}


	/**
	 * Apply the rule to the message if matched.
	 * @param message 
	 * @param object 
	 */
	public void processRule(String action, Message message) {


	}

    /**
     * Setup server thread and client to send message
     *
     */
    public void setUp() throws Exception {

        ServerThread serverThread = new ServerThread(this, nodes.get(local_name).port);
        new Thread(serverThread).start();
        System.out.println("Server thread on port " + nodes.get(local_name).port);

    }

    /**
     * Send message that matched the rule
     *
     */
    public void sendMessage(Message message){
        Socket clientSocket;
        ObjectOutputStream output;
        //If the connection has been established
        if (sockets.get(message.dest) != null) {
            clientSocket = sockets.get(message.dest);
            output = outputStreams.get(message.dest);
            try {
                output.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //else build the connection
        else {
            try {
                clientSocket = new Socket(nodes.get(message.dest).ip,nodes.get(message.dest).port);
                System.out.println("Connection setup with " + nodes.get(message.dest).ip + " port "+nodes.get(message.dest).port);
                output = new ObjectOutputStream(clientSocket.getOutputStream());
                output.writeObject(message);

                sockets.put(message.dest, clientSocket);
                outputStreams.put(message.dest, output);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
