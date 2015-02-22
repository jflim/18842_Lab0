package core;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;

import Clock.ClockService;
import Multicast.MulticastService;

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
    private HashMap<ObjectInputStream, ObjectOutputStream> connectStreams;

    private Queue<TimeStampedMessage> delayQueue;
    private Queue<TimeStampedMessage> receivedDelayQueue;
    private Queue<TimeStampedMessage> receivedQueue;
    private boolean isProcessedRules = false;
    private ClockService clock = null;
    private HashMap<String, Group> groups;
    public int numOfReplies = 0;

    public MulticastService multicastService;
    // node configuration
    Map<String, Node> nodes;

    // Send rule list
    ArrayList<LinkedHashMap<String, Object>> sendRules;

    // Receive rule list
    ArrayList<LinkedHashMap<String, Object>> receiveRules;
	
    // logger variables
    private boolean isLogger;
	private List<TimeStampedMessage> logs;

    public class Node {
        String name;
        String ip;
        int port;

        Node(String name, String ip, int port) {
            this.name = name;
            this.ip = ip;
            this.port = port;
        }
    }

	public MessagePasser(String configuration_filename, String local_name,
			boolean isLogger, List<TimeStampedMessage> logs) {
		this.configuration_filename = configuration_filename;
        this.local_name = local_name;

        nodes = new LinkedHashMap<String, Node>();
        sockets = new HashMap<String, Socket>();
        outputStreams = new HashMap<String, ObjectOutputStream>();
        connectStreams = new HashMap<ObjectInputStream, ObjectOutputStream>();

        delayQueue = new LinkedList<TimeStampedMessage>();
        receivedDelayQueue = new LinkedList<TimeStampedMessage>();
        receivedQueue = new LinkedList<TimeStampedMessage>();
        this.groups = new HashMap<String, Group>();
        // logger variables
        this.isLogger = isLogger;
        this.logs = logs;
        //multicast service
        this.multicastService = new MulticastService(this);


        try {
            parseConfig();

            serverSetUp();

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
		
        // collect nodes in the configuration
        if (m.containsKey("configuration")) {
			parseNodes(m.get("configuration"));
		}
		
        // set up the clock
		if (this.clock == null && m.containsKey("clock")) {
			ArrayList<LinkedHashMap<String, Object>> clockRule = m.get("clock");
			boolean isLogical = (Boolean) clockRule.get(0).get("Logical");
			
			// determine local_index according to node hashmap
			int local_index = 0;
			Iterator<String> x = nodes.keySet().iterator();
			while (x.hasNext()) {
				String nodeName = x.next();
				if (nodeName.equals(local_name)) {
					break;
				}
				local_index++;
			}
			clock = ClockService.newClock(isLogical, local_index, nodes.size());
		}
        //set up groups
        if (m.containsKey("groups")) {
            ArrayList<LinkedHashMap<String, Object>> groupsList = m.get("groups");
            for (LinkedHashMap<String, Object> group : groupsList) {
                String groupName = (String) group.get("name");
                ArrayList<String> members = (ArrayList<String>) group.get("members");
                Group tmp = new Group(groupName, members, nodes, clock.copy());
                if(members.contains(local_name))
                    multicastService.addGroup(tmp);
                groups.put(groupName, tmp);
            }
            
            multicastService.initCache();
    		multicastService.initHoldBackQueue();
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
    public synchronized void checkSendRules(TimeStampedMessage message) {
        // there are no receive rules
        if (sendRules == null || sendRules.isEmpty()) {
            sendMessage(message);
            while (!this.delayQueue.isEmpty()) {
                sendMessage(delayQueue.remove());
            }
            return;
        }
        isProcessedRules = false;
        
        // print message information in client
        message.displayMessageInfo("Send");

        for (LinkedHashMap<String, Object> rule : sendRules) {
            if (checkRule(message, rule)) {
                processSendRule((String) rule.get("action"), message);
                isProcessedRules = true;
                break;
            }
        }
        if (isProcessedRules != true) {
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
    public synchronized void checkReceiveRules(TimeStampedMessage message) {
        isProcessedRules = false;

        // there are no receive rules
        if (receiveRules == null || receiveRules.isEmpty()) {
            receivedQueue.add(message);
            while (!this.receivedDelayQueue.isEmpty()) {
                this.receivedQueue.add(this.receivedDelayQueue.remove());
            }
            return;
        }

        // check if any rules match
        for (LinkedHashMap<String, Object> rule : receiveRules) {
            if (checkRule(message, rule)) { // if rule matched
                processReceiveRule((String) rule.get("action"), message);
                isProcessedRules = true;
                break;
            }
        }

        // no rules match
        if (isProcessedRules != true) {
            receivedQueue.add(message);
            while (!this.receivedDelayQueue.isEmpty()) {
                this.receivedQueue.add(this.receivedDelayQueue.remove());
            }
        }
    }

    /**
     * Checks if a rule matches, and calls another function to apply the rule
     * if it is.
     *
     * @param message
     * @param rule
     * @return true  if a rule matches
     * false if a rule doesn't match
     */
    private boolean checkRule(TimeStampedMessage message,
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
        
    	// added for multicast
    	if(rule.containsKey("origSrc") && 
    			!((String) rule.get("origSrc")).equalsIgnoreCase(message.getOrigSender())){
    		return false;
    	}

        return true;
    }

    /**
     * Apply the rule to the sent message if matched.
     *
     * @param message
     */
    public synchronized void processSendRule(String action, TimeStampedMessage message) {

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
            TimeStampedMessage m = new TimeStampedMessage(message);
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
     */
    public synchronized void processReceiveRule(String action, TimeStampedMessage message) {
        if (action.equals("drop")) {
            message.displayMessageInfo("Drop");
            message = null;
            return;
        } else if (action.equals("delay")) { // 1 delayed message
            this.receivedDelayQueue.add(message);
            message = null;
            return;
        } else if (action.equals("duplicate")) { // 2 messages
            TimeStampedMessage duplicateMessage = new TimeStampedMessage(message);
            this.receivedQueue.add(message);
            duplicateMessage.set_duplicate(true);
            this.receivedQueue.add(duplicateMessage);
            while (!this.receivedDelayQueue.isEmpty()) {
                this.receivedQueue.add(this.receivedDelayQueue.remove());
            }
        }
    }

    /**
     * Request to send message to other processes
     *
     * @param message
     */

    public void send(TimeStampedMessage message){

        // Add TimeStamp to Message
    	this.getClock().clockIncrement(); // clock increment before sending
    	message.setTimeStamp(this.getClock());
 
    	try {
			getSendRules();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        checkSendRules(message);

    }

    /**
     * Receive message from other processes
     */
    public TimeStampedMessage receiveMessage() {
        TimeStampedMessage message = this.receivedQueue.poll();
        return message;
    }

    /**
     * Request to receive message from other processes
     *
     * @param socket
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void receive(Socket socket, ObjectInputStream input) throws ClassNotFoundException, IOException {
        TimeStampedMessage receivedMessage = null;
    	try {
        	receivedMessage = (TimeStampedMessage) input.readObject();
            // may throw IO exception.
        } catch (IOException e) {
        	e.printStackTrace();
        }
        

        //add the socket and stream of the sender.
        if (!sockets.containsKey(receivedMessage.src)) {
            this.addSockets(receivedMessage.src, socket);
            ObjectOutputStream output;
            try {
                output = new ObjectOutputStream(socket.getOutputStream());
                this.addOutputStream(receivedMessage.src, output);
                this.addConnectionStreams(input, output);
            } catch (IOException e) {
                // TODO Auto-generated catch block
            	// e.printStackTrace();
            }

        }

        if (receivedMessage != null) {
            getReceiveRules();
            checkReceiveRules(receivedMessage);
        }
    }

    /**
     * method for message passer to increment clock
     * @return
     */
    public ClockService incrementClock(){
    	clock.clockIncrement();
    	return clock;   	
    }
    
    /**
     * Setup server thread and client to send message
     */
    public void serverSetUp() throws Exception {

        // start up the listening socket
		ServerThread serverThread = new ServerThread(this,
				nodes.get(local_name).port, isLogger, logs);
		new Thread(serverThread).start();
        System.out.println("Server thread on port " + nodes.get(local_name).port);

    }

    /**
     * Send message that matched the rule
     * through use of streams
     */
    public void sendMessage(TimeStampedMessage message) {
    	sendMessage(message, false);
    }


	private void sendMessage(TimeStampedMessage message, boolean toLogger) {
		String targetName = message.dest;
    	if(toLogger == true){
    		targetName = "logger";
    	}
    	
        Socket clientSocket = null;
        ObjectOutputStream output = null;

        // If the connection has been established
        if (sockets.get(targetName) != null) {
            clientSocket = sockets.get(targetName);
            output = outputStreams.get(targetName);
            try {
                output.writeObject(message);
                output.reset();
            } catch (IOException e) {
                // assume the connection is dead.
                System.err.println("Connection refused. Check the receiving side.");
                this.removeSocket(targetName);
                return;
            }
        } else {
            //set up a new connection
            String targetIp = nodes.get(targetName).ip;
            int targetPort = nodes.get(targetName).port;
            int timeout = TIMEOUT_IN_SECS * 1000; // sec to msec

            boolean connected = false;
            while (connected == false) {
                try {
                    clientSocket = new Socket();
                    clientSocket.connect(new InetSocketAddress(targetIp,
                            targetPort), timeout);
                    System.out.println("Connected to " + targetIp + " port "
                            + targetPort);

                    connected = true;
                } catch (IOException e) {
                    System.err.println("Unable to set up connection with "
                            + nodes.get(targetName).ip + " port "
                            + nodes.get(targetName).port);

                    // wait a while before attempt to connect again.
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    continue;
                }

                Thread thread = new Thread(new WorkThread(clientSocket, this, false, null));
                thread.start();

                try {
                    output = new ObjectOutputStream(clientSocket.getOutputStream());
                    output.writeObject(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                sockets.put(targetName, clientSocket);
                outputStreams.put(targetName, output);
            }
        }
	}

    /**
     * removes output streams from saved streams.
     * usually called when send fails.
     *
     * @param dest
     */
    private void removeOutputStream(String dest) {
        Iterator<String> x = outputStreams.keySet().iterator();
        while (x.hasNext()) {
            if (x.equals(dest)) {
                x.remove();
            }
        }
    }

    /**
     * add a socket to the list of sockets
     *
     * @param remote_host
     * @param remote_socket
     */
    public void addSockets(String remote_host, Socket remote_socket) {
        sockets.put(remote_host, remote_socket);
    }

    public void addOutputStream(String remote_host, ObjectOutputStream stream) {
        outputStreams.put(remote_host, stream);
    }

    public void addConnectionStreams(ObjectInputStream in, ObjectOutputStream out) {
        connectStreams.put(in, out);
    }

    /**
     * get the list of known target sockets
     *
     * @return
     */
    public HashMap<String, Socket> getSockets() {
        return sockets;
    }

    /**
     * removes stream from saved output streams ;usually called to remove
     * streams that are destroyed (i.e. received a SIG INT)
     */
    public String removeInputStream(ObjectInputStream input) {
        Iterator<Entry<ObjectInputStream, ObjectOutputStream>> streamIter = connectStreams
                .entrySet().iterator();
        while (streamIter.hasNext()) {
            Entry<ObjectInputStream, ObjectOutputStream> x = streamIter.next();
            if (x.getKey().equals(input)) {

                // find the outputStream
                Iterator<Entry<String, ObjectOutputStream>> oIter = outputStreams
                        .entrySet().iterator();
                while (oIter.hasNext()) {
                    Entry<String, ObjectOutputStream> entry = oIter.next();
                    if (entry.getValue().equals(x.getValue())) {
                        String targetName = entry.getKey();
                        streamIter.remove();
                        oIter.remove();
                        System.out.println("removed the Streams");
                        return targetName;
                    }
                }
            }
        }

        // stream not found in the hashmap
        return null;

    }

    public void removeSocket(String targetName) {
        Iterator<String> socketIter = sockets.keySet().iterator();
        while (socketIter.hasNext()) {
            String socketName = socketIter.next();
            if (socketName.equals(targetName)) {
                socketIter.remove();
                System.out.println("removed the socket");
            }
        }
    }

    public ClockService getClock() {
        return this.clock;
    }


    public String getLocalName(){
    	return this.local_name;
    }
    
	public void sendMessageToLogger(TimeStampedMessage processedMessage) {
		boolean toLogger = true;
    	sendMessage(processedMessage, toLogger);	
	}
	
	public int incSequenceNumber(){
		this.seqNum++;
		return this.seqNum;
	}

    public HashMap<String, Group> getGroups(){
        return this.groups;
    }
}
