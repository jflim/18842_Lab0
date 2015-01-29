import java.io.*;
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
    int seqNum = 0;
    private HashMap<String, Socket> sockets;
    private HashMap<String, ObjectOutputStream> outputStreams;
    private Queue<Message> delayQueue;
    private Queue<Message> receivedDelayQueue;
    private Queue<Message> receivedQueue;

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
        sockets = new HashMap<String, Socket>();
        outputStreams = new HashMap<String, ObjectOutputStream>();

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
//        if (m.containsKey("sendRules")) {
//            sendRules = m.get("sendRules");
//        }
//        if (m.containsKey("receiveRules")) {
//            receiveRules = m.get("receiveRules");
//        }
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
    public void checkSendRules(Message message) {
        for (LinkedHashMap<String, Object> rule : sendRules) {
            if (checkRule(message, rule))
                processSendRule((String) rule.get("action"), message);

        }
    }


    /**
     * Check an incoming message with the receive rules. First rule that matches
     * is applied to the message.
     */
    public void checkReceiveRules(Message message) {
        for (LinkedHashMap<String, Object> rule : receiveRules) {
            if (checkRule(message, rule))
                processReceiveRule((String) rule.get("action"), message);

        }
    }

    /**
     * Checks if a rule matches, and calls another function
     * to apply the rule.
     *
     * @param message
     * @param rule
     */
    private boolean checkRule(Message message, LinkedHashMap<String, Object> rule) {
        // check all the rule entries with message attributes

        if (rule.containsKey("src") &&
                !((String) rule.get("src")).equalsIgnoreCase(message.src)) {
            return false;
        }

        if (rule.containsKey("dest") &&
                !((String) rule.get("dest")).equalsIgnoreCase(message.dest)) {
            return false;
        }
        if (rule.containsKey("kind") &&
                !((String) rule.get("kind")).equalsIgnoreCase(message.kind)) {
            return false;
        }
        if (rule.containsKey("seqNum") &&

                (((Integer) rule.get("seqNum")).equals(message.seqNum))) {
            return false;
        }

        if (rule.containsKey("duplicate") &&
                (rule.get("duplicate") == message.dup)) {
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
        if (action.equalsIgnoreCase("drop")) {
            return;
        } else if (action.equalsIgnoreCase("delay")) {
            this.delayQueue.add(message);
        } else if (action.equalsIgnoreCase("duplicate")) {
            sendMessage(message);
            message.set_duplicate(true);
        }

        sendMessage(message);
        while (!this.delayQueue.isEmpty()) {
            sendMessage(delayQueue.remove());
        }
    }

    /**
     * Apply the rule to the received message if matched.
     *
     * @param message
     * @param object
     */
    public synchronized void processReceiveRule(String action, Message message) {
        if (action.equals("drop")) {
            return;
        } else if (action.equals("delay")) {
            this.receivedDelayQueue.add(message);
        } else if (action.equals("duplicate")) {
            Message duplicateMessage = new Message(message);
            duplicateMessage.set_duplicate(true);
            this.receivedQueue.add(duplicateMessage);

        }

        this.receivedQueue.add(message);
        while (!this.receivedDelayQueue.isEmpty()) {
            this.receivedQueue.add(this.receivedDelayQueue.remove());
        }

    }


        /**
         * Send message to other processes
         * @param message
         */

    public void send(Message message) throws FileNotFoundException {
        message.set_source(local_name);
        message.set_seqNum(seqNum);
        message.set_duplicate(false);
        getSendRules();
        checkSendRules(message);
    }

    /**
     * Receive message from other processes
     *
     * @param message
     */
    public Message receive(){
        Message message = this.receivedQueue.remove();
        return message;
    }

    /**
     * Receive message from other processes
     *
     * @param message
     */
    public void receiveMessage(Message message) throws FileNotFoundException {
        getReceiveRules();
        checkReceiveRules(message);
    }
    /**
     * Setup server thread and client to send message
     */
    public void setUp() throws Exception {
        Socket clientSocket;
        ObjectOutputStream output;
        ServerThread serverThread = new ServerThread(this, nodes.get(local_name).port);
        new Thread(serverThread).start();
        System.out.println("Server thread on port " + nodes.get(local_name).port);
        //setup the connection
        try {
            for (String name : nodes.keySet()) {
                clientSocket = new Socket(nodes.get(name).ip, nodes.get(name).port);
                System.out.println("Connection setup with " + nodes.get(name).ip + " port " + nodes.get(name).port);
                output = new ObjectOutputStream(clientSocket.getOutputStream());

                sockets.put(name, clientSocket);
                outputStreams.put(name, output);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Send message that matched the rule
     */
    public void sendMessage(Message message) {
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
                clientSocket = new Socket(nodes.get(message.dest).ip, nodes.get(message.dest).port);
                System.out.println("Connection setup with " + nodes.get(message.dest).ip + " port " + nodes.get(message.dest).port);
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
