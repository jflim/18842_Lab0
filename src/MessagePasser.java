import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

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
	public static final int TIMEOUT_IN_SECS = 3;

	int seqNum = 0;
	private HashMap<String, Socket> sockets;
	private HashMap<String, ObjectOutputStream> outputStreams;
	private Queue<Message> delayQueue;
	private Queue<Message> receivedDelayQueue;
	private Queue<Message> receivedQueue;
    private boolean isProcessedRules = false;
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

		nodes = new LinkedHashMap<String, Node>();
		sockets = new HashMap<String, Socket>();
		outputStreams = new HashMap<String, ObjectOutputStream>();
		delayQueue = new LinkedList<Message>();
		receivedDelayQueue = new LinkedList<Message>();
		receivedQueue = new LinkedList<Message>();
		
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
	}

	/**
	 * parse the configuration file and store the sendRules
	 *
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public void getSendRules() throws FileNotFoundException {
		InputStream input = new FileInputStream(
				new File(configuration_filename));
		Yaml yaml = new Yaml();
		Object data = yaml.load(input);

		Map<String, ArrayList<LinkedHashMap<String, Object>>> m;
		m = (Map<String, ArrayList<LinkedHashMap<String, Object>>>) data;

		if (m.containsKey("sendRules")) {
			sendRules = m.get("sendRules");
		}
	}

	/**
	 * parse the configuration file and store the elements into local Java data
	 * structures.
	 *
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public void getReceiveRules() throws FileNotFoundException {
		InputStream input = new FileInputStream(
				new File(configuration_filename));
		Yaml yaml = new Yaml();
		Object data = yaml.load(input);

		Map<String, ArrayList<LinkedHashMap<String, Object>>> m;
		m = (Map<String, ArrayList<LinkedHashMap<String, Object>>>) data;

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
			int port = (Integer) node.get("port");
			Node n = new Node(name, ip, port);
			nodes.put(name, n);
		}
	}

	/**
	 * Check an outgoing message with the sending rules. First rule that matches
	 * is applied to the message. Called by MessagePasser.send()
	 */
	public synchronized void checkSendRules(Message message) {
        isProcessedRules = false;
        System.out.println("Send: " + message.data + " to " + message.dest +" Seqnum: " +message.seqNum);
		for (LinkedHashMap<String, Object> rule : sendRules) {
			if (checkRule(message, rule)){
				processSendRule((String) rule.get("action"), message);
                isProcessedRules = true;
				break;
			}
		}
		if(isProcessedRules != true) {
            sendMessage(message);
            while (!this.delayQueue.isEmpty()) {
                sendMessage(delayQueue.remove());
            }
        }
	}

	/**
	 * Check an incoming message with the receive rules. First rule that matches
	 * is applied to the message.
	 */
	public synchronized void checkReceiveRules(Message message) {
        isProcessedRules = false;
		for (LinkedHashMap<String, Object> rule : receiveRules) {
			
			if (checkRule(message, rule)) { // if rule matched
				processReceiveRule((String) rule.get("action"), message);
                isProcessedRules = true;
				break;
			}
		}
        if(isProcessedRules != true) {
            receivedQueue.add(message);
            while (!this.receivedDelayQueue.isEmpty()) {
                this.receivedQueue.add(this.receivedDelayQueue.remove());
            }
        }
	}

	/**
	 * Checks if a rule matches, and calls another function to apply the rule
	 * if it is.
	 * @param message
	 * @param rule
	 * @return true  if a rule matches
	 * 		   false if a rule doesn't match
	 */
	private boolean checkRule(Message message,
			LinkedHashMap<String, Object> rule) {
		// check all the rule entries with message attributes

		if (rule.containsKey("src")
				&& !((String) rule.get("src")).equalsIgnoreCase(message.src)) {
			return false;
		}

		if (rule.containsKey("dest")
				&& !((String) rule.get("dest")).equalsIgnoreCase(message.dest)) {
			return false;
		}
		if (rule.containsKey("kind")
				&& !((String) rule.get("kind")).equalsIgnoreCase(message.kind)) {
			return false;
		}
		if (rule.containsKey("seqNum") 
				&& !(((Integer) rule.get("seqNum")).equals(message.seqNum))) {
			return false;
		}

		if (rule.containsKey("duplicate")
				&& !(rule.get("duplicate").equals(message.dup))) {
			return false;
		}

		return true;
	}

	/**
	 * Apply the rule to the sent message if matched.
	 *
	 * @param message
	 * @param object
	 */
	public synchronized void processSendRule(String action, Message message) {

		//System.out.println("Action: " + action + "|| " + message.data + " " + message.dup);
		if (action.equalsIgnoreCase("drop")) {
            System.out.println("Drop");
            message = null;
            return;
		} else if (action.equalsIgnoreCase("delay")) {
            System.out.println("Delay");
			this.delayQueue.add(message);
            message = null;
            return;
		} else if (action.equalsIgnoreCase("duplicate")) {
            System.out.println("Duplicate");
			sendMessage(message);
			Message m = new Message(message);
			m.set_duplicate(true);
		    sendMessage(m);
		     
			//sent a non-delayed message. send any delayed messages
			while (!this.delayQueue.isEmpty()) {
				sendMessage(delayQueue.remove());
			}
		}	
	}

	/**
	 * Apply the rule to the received message if matched.
	 *
	 * @param message
	 * @param object
	 */
	public synchronized void processReceiveRule(String action, Message message) {
		//System.out.println("Action: " + action + "|| " + message.data + " " + message.dup);
		if (action.equals("drop")) {
            System.out.println("Drop");
            message = null;
			return;
		} else if (action.equals("delay")) { // 1 delayed message
			this.receivedDelayQueue.add(message);
            message = null;
            return;
		} else if (action.equals("duplicate")) { // 2 messages
			Message duplicateMessage = new Message(message);
            this.receivedQueue.add(message);
			duplicateMessage.set_duplicate(true);
			this.receivedQueue.add(duplicateMessage);
			while (!this.receivedDelayQueue.isEmpty()) {
				this.receivedQueue.add(this.receivedDelayQueue.remove());				
			}
		}
	}

	/**
	 * Send message to other processes
	 * 
	 * @param message
	 */

	public void send(Message message) throws FileNotFoundException {
		message.set_source(local_name);
		message.set_seqNum(seqNum++);
		message.set_duplicate(false);
		getSendRules();
		checkSendRules(message);
	}

	/**
	 * Receive message from other processes
	 *
	 * @param message
	 */
	public Message receive() {
		Message message = this.receivedQueue.poll();
		return message;
	}

	/**
	 * Receive message from other processes
	 *
	 * @param message
	 */
	public void receiveMessage(Message message) throws FileNotFoundException {

		//System.out.println("Received something, but need to check rules.");
		//System.out.println(message.data + " " + message.dup);
        if(message != null) {
            getReceiveRules();
            checkReceiveRules(message);
        }
	}

	/**
	 * Setup server thread and client to send message
	 */
	public void setUp() throws Exception {

		// start up the listening socket
		ServerThread serverThread = new ServerThread(this, nodes.get(local_name).port);
        new Thread(serverThread).start();
        System.out.println("Server thread on port " + nodes.get(local_name).port);

		// set up connections to the nodes ordered before local in config file.
		List<String> nodeList = new ArrayList<String>(nodes.keySet());
		//System.out.println(nodeList);
		int local_index = nodeList.indexOf(local_name);
		List<String> targetList = nodeList.subList(0, local_index);

		System.out.println(targetList);
		while (!targetList.isEmpty()) {

			// try to make a connection with remote "server"
			// no delay at the moment if fail.
			for (Iterator<String> iter = targetList.iterator(); iter.hasNext();) {
				String targetName = iter.next();
				String targetIp = nodes.get(targetName).ip;
				int targetPort = nodes.get(targetName).port;
				int timeout = TIMEOUT_IN_SECS * 1000; // sec to msec

				try {
					Socket clientSocket = new Socket();
					clientSocket.connect(new InetSocketAddress(targetIp,
							targetPort), timeout);
                    Thread thread = new Thread(new WorkThread(clientSocket, this));
                    thread.start();
					System.out.println("Connection setup with " + targetIp
							+ " port " + targetPort);
					ObjectOutputStream output = new ObjectOutputStream(
							clientSocket.getOutputStream());

					sockets.put(targetName, clientSocket);
					outputStreams.put(targetName, output);
				  	
					// initial HELLO request
			    	Message initialM = new Message(targetName, "HELLO", "HELLO");
			    	send(initialM);	    	
					iter.remove();

				} catch (IOException e) {
					//e.printStackTrace();
					System.err.println("Unable to set up connection with "
							+ nodes.get(targetName).ip + " port "
							+ nodes.get(targetName).port);
					Thread.sleep(TIMEOUT_IN_SECS * 1000);
				}
			}
		}
	}

	/**
	 * Send message that matched the rule
	 */
	public void sendMessage(Message message) {

		//System.out.println("|| " + message.data + " " + message.dup);
		Socket clientSocket;
		ObjectOutputStream output;
		// If the connection has been established
		if (sockets.get(message.dest) != null) {
			clientSocket = sockets.get(message.dest);
			output = outputStreams.get(message.dest);
            try {
				output.writeObject(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * add a socket to the list of sockets
	 * @param remote_host
	 * @param remote_socket
	 */
	public void addSockets(String remote_host, Socket remote_socket){
		sockets.put(remote_host, remote_socket);
	}

    public void addStream(String remote_host, ObjectOutputStream stream){
        outputStreams.put(remote_host, stream);
    }
	/**
	 * get the list of known target sockets
	 * @return
	 */
	public HashMap<String, Socket> getSockets(){
		return sockets;
	}
}
