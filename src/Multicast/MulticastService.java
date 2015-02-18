package Multicast;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import core.Group;
import core.Message;
import core.MessagePasser;
import core.TimeStampedMessage;

public class MulticastService {

	Map<String, Integer> gSeqNum;
	Map<String, HashMap<String, Integer>> delivered; // Group for each <member, delivered>
	Map<String, HashMap<String, List<TimeStampedMessage>>> holdBackQueues; // group, list
	Map<String, HashMap<String, List<TimeStampedMessage>>> caches;
	MessagePasser mp;
	
	// cachesize 
	int cacheSize = 3;

	public MulticastService(MessagePasser mp) {

		gSeqNum = new HashMap<String, Integer>();
		delivered = new HashMap<String, HashMap<String, Integer>>();
		holdBackQueues = new HashMap<String, HashMap<String, List<TimeStampedMessage>>>();
		caches = new HashMap<String, HashMap<String, List<TimeStampedMessage>>>();	

		this.mp = mp; // get a reference to a MessagePasser
	}

	// TODO: assume group is not empty..
	/**
	 * called only in initialization stages of Multicast Service used to add a
	 * single group into the service's maps
	 * 
	 * @param g
	 */
	public void addGroup(Group g) {
		gSeqNum.put(g.getName(), 0);
		HashMap<String, Integer> deliverGroup = new HashMap<String, Integer>();
		for (String memberName : g.memberNames()) {
			deliverGroup.put(memberName, 0);
		}
		delivered.put(g.getName(), deliverGroup);
	}

	public void send_multicast(String groupName, TimeStampedMessage m)
			throws FileNotFoundException {
		
		//increment the sequence number before sending
		this.gSeqNum.put(groupName, gSeqNum.get(groupName)+1);
		int selfGroupSeqNum = this.gSeqNum.get(groupName);

		for (String targetNode : delivered.get(groupName).keySet()) {
			TimeStampedMessage tm = new TimeStampedMessage(m);
			tm.addGroupSeqNum(selfGroupSeqNum);
			tm.addACKs(delivered.get(groupName));

			// set the dest
			tm.set_dst(targetNode);
			mp.send(tm);
		}
	}

	public void receive_multicast(String groupName, TimeStampedMessage m) {
		//System.out.println("In receive_multicast");
		//System.out.println(m.getGroupSeqNum());
		
		if(m.getNACK() == true){
			handleNACK(m);
			return;
		}

		Map<String, Integer> groupDelivers = delivered.get(groupName);
		int R_for_sender = groupDelivers.get(m.get_source());
		String missingSender; // sender whose message was missed. 

		//first insert into queue
		insertInHoldBackQueue(groupName, m);
		
		missingSender = seenMissingMessage(groupName, m.getACKS());
		if(missingSender != null){
			try {
				 // request missed packet from someone
				sendNACK(m.getGroupSeqNum(), groupName, missingSender);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		// decide what to do now
		if (m.getGroupSeqNum() > R_for_sender + 1) {
			// not delivered and not next
			return;
		}
		else if (m.getGroupSeqNum() <= R_for_sender) { // already handled,
			// duplicate
			return;
		} 

		
		if (m.getGroupSeqNum() == R_for_sender + 1) { // next expected Message
			deliver(m);
			groupDelivers.put(m.get_source(), R_for_sender + 1);
			handleHoldBackQueue(groupName, m.get_source());
		}
	}

	/**
	 * Add into proper HoldBackQueue by groupName and sorts
	 * based on the group sequence number
	 * TODO: insert in sorted order instead of add-then-sort
	 * @param groupName
	 * @param m
	 */
	private void insertInHoldBackQueue(String groupName, TimeStampedMessage m) {

		HashMap<String, List<TimeStampedMessage>> groupQueue = holdBackQueues.get(groupName);
		if(groupQueue.containsKey(m.get_source())){
			List<TimeStampedMessage> hbList = groupQueue.get(m.get_source());
			groupQueue.get(m.get_source()).add(m);
			Collections.sort(hbList, new Comparator<TimeStampedMessage>(){
				public int compare(TimeStampedMessage first, TimeStampedMessage second){
					if(first.getGroupSeqNum() < second.getGroupSeqNum()){
						return -1;
					}
					else{
						return 1;
					}
				}
			});
		}
		else{ // not initialized yet
			List<TimeStampedMessage> li = new LinkedList<TimeStampedMessage>();
			li.add(m);
			groupQueue.put(m.get_source(), li);
		}

	}

	/**
	 * Compares each ACK to figure out if a message wasn't received
	 * @param acks
	 * @return
	 */
	private String seenMissingMessage(String groupName, Map<String, Integer> acks) {
		for(Entry<String, Integer> ack: acks.entrySet()){
			if(ack.getValue() > delivered.get(groupName).get(ack.getKey())){
				return ack.getKey();
			}
		}
		return null;
	}

	private void sendNACK(int groupSeqNum, String groupName,
			String missingSender) throws FileNotFoundException {
		// determine which nodes had delivered the message already
		for (Entry<String, Integer> node : delivered.get(groupName).entrySet()) {
			if (node.getValue() >= groupSeqNum) {
				String request = missingSender + ":::" + groupSeqNum;
				Message m = new Message(node.getKey(), "NACK", request);
				TimeStampedMessage t = new TimeStampedMessage(m, mp.getClock());
				
				// set Multicast fields
				t.setNACK(true);
				t.setGroupName(groupName);
				
				// set regular fields
				t.set_source(mp.getLocalName());
				t.set_seqNum(mp.incSequenceNumber()); // increments seq number before sending
				
				System.out.println("Sending a NACK to " + node.getKey()
						+ " for: " + groupSeqNum + "from " + missingSender);
				mp.send(t);
			}
		}
	}

	private void handleHoldBackQueue(String groupName, String src) {
		List<TimeStampedMessage> li = holdBackQueues.get(groupName).get(src);
		
		if(li == null){ // holdBackQueue not even initialized
			return;
		}
		
		//find smallest R and check if we satisfy R
		
		// while satisfy R:
		//     deliver
		//	   find next smallest R and check if we satisfy R
		// else: 
		//    return
		
		HashMap<String, Integer> groupDelivers = delivered.get(groupName);
		ListIterator<TimeStampedMessage> listItor = li.listIterator();
		while(listItor.hasNext()){
			TimeStampedMessage tm = listItor.next();
			if(tm.getGroupSeqNum() == groupDelivers.get(src) + 1){
				groupDelivers.put(src, groupDelivers.get(src) +1);
				listItor.remove();
				TimeStampedMessage expectedMessage = new TimeStampedMessage(tm);
				deliver(expectedMessage);
			}
			else if(tm.getGroupSeqNum() <= groupDelivers.get(src)){
				listItor.remove();
				continue; // if we somehow had a duplicate..
			}
			else{
				break; // no more expected
			}
		}

	}

	/**
	 * Handles incoming NACKs with the request missed message in the data section
	 * @param m
	 */
	public void handleNACK(TimeStampedMessage m){
		System.out.println("Handling a NACK from " + m.get_source());
		TimeStampedMessage cachedMessage = checkCache(m.getGroupName(), m.getData());
		if(cachedMessage == null){
			System.err.println("Message from Group " + m.getGroupName()
					+ ", RequestData " + m.getData() + "not stored in cache");
		}
		else{
			try {
				cachedMessage.set_dst(m.get_source());
				mp.send(cachedMessage);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private TimeStampedMessage checkCache(String groupName, String data) {
		String[] tmp = data.split(":::");
		String origSrc = tmp[0];
		int requestMessageSeqNum = Integer.parseInt(tmp[1]);
		Iterator<TimeStampedMessage> li = caches.get(groupName).get(origSrc).iterator();
		while(li.hasNext()){
			TimeStampedMessage mess = li.next();
			if(mess.getGroupSeqNum() == requestMessageSeqNum){
				return mess;
			}
		}
		
		return null;
	}

	private void deliver(TimeStampedMessage m) {
		System.out.println("Data: " + m.getData()
				+ " Kind: " + m.getKind()
				+ " SeqNum: " + m.getSeqNum()
				+ " Dup: " + m.getDup()
				+ " Timestamp: " + m.getTimeStamp()    
				+ "\nGroup Name: " + m.getGroupName()
				+ ", Src: " + m.get_source() 
				+ "\nGroup Sequence Number: "  + m.getGroupSeqNum());
		
		
		if(caches.get(m.getGroupName()).containsKey(m.get_source())){
			List<TimeStampedMessage> l = caches.get(m.getGroupName()).get(m.get_source());
			l.add(m);
			if(l.size() >  cacheSize){
				l.remove(0);
			}
		}
		else{
			List<TimeStampedMessage> newList = new LinkedList<TimeStampedMessage>();
			newList.add(m);
			caches.get(m.getGroupName()).put(m.get_source(), newList);
		}
	}

	public void initCache() {
		// init cache
		for(String groupName: delivered.keySet()){
			caches.put(groupName, new HashMap<String, List<TimeStampedMessage>>());
		}
	}
	
	public void initHoldBackQueue(){
		// init holdBackqueue
		for(String groupName: delivered.keySet()){
			holdBackQueues.put(groupName, new HashMap<String, List<TimeStampedMessage>>());
		}
	}
}
