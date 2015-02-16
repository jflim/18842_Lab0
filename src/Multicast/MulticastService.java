package Multicast;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import core.Group;
import core.MessagePasser;
import core.TimeStampedMessage;

public class MulticastService {

	Map<String, Integer> gSeqNum;
	Map<String, HashMap<String, Integer>> delivered; // Group for each <member,
													// delivered>
	Map<String, HashMap<String, List<TimeStampedMessage>>> holdBackQueues;
	MessagePasser mp;

	public MulticastService(MessagePasser mp) {

		gSeqNum = new HashMap<String, Integer>();
		delivered = new HashMap<String, HashMap<String, Integer>>();
		holdBackQueues = new HashMap<String, HashMap<String, List<TimeStampedMessage>>>();

		this.mp = mp; // get a reference to a MessagePasser
	}

	// TODO: assume group is not empty..
	/**
	 * called only in initialization stages of Multicast Service used to add a
	 * single group into the service's maps
	 * 
	 * @param g
	 */
	void addGroup(Group g) {
		gSeqNum.put(g.getName(), 0);
		HashMap<String, Integer> deliverGroup = new HashMap<String, Integer>();
		for (String memberName : g.memberNames()) {
			deliverGroup.put(memberName, 0);
		}
		delivered.put(g.getName(), deliverGroup);
	}

	void send_multicast(String groupName, TimeStampedMessage m)
			throws FileNotFoundException {
		int selfGroupSeqNum = this.gSeqNum.get(groupName);

		for (String targetNode : delivered.keySet()) {
			TimeStampedMessage tm = new TimeStampedMessage(m);
			tm.addGroupSeqNum(selfGroupSeqNum);
			tm.addACKs(delivered.get(groupName));

			// set the dest
			tm.set_dst(targetNode);
			mp.send(tm);
		}
	}

	void receive_multicast(String groupName, TimeStampedMessage m) {

		Map<String, Integer> groupDelivers = delivered.get(groupName);
		int R_for_sender = groupDelivers.get(m.get_source());

		// decide what to do now
		if (m.getGroupSeqNum() == R_for_sender + 1) { // next expected Message
			deliver(m);
			groupDelivers.put(m.get_source(), R_for_sender + 1);
			handleHoldBackQueue(groupName, m.get_source());

		} else if (m.getGroupSeqNum() <= R_for_sender) { // already handled,
															// duplicate
			return;
		}else if(m.getGroupSeqNum() > R_for_sender){ //not delivered and not next
			insertInHoldBackQueue(groupName, m);	
		}
		 else if(seenMissingMessage(groupName, m.getACKS()) == true){
			sendNACK(m.getGroupSeqNum(), groupName); // request missed packet from this sender
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

		List<TimeStampedMessage> li = holdBackQueues.get(groupName).get(m.get_source());
		li.add(m);
		Collections.sort(li, new Comparator<TimeStampedMessage>(){
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

	/**
	 * Compares each ACK to figure out if a message wasn't received
	 * @param acks
	 * @return
	 */
	private boolean seenMissingMessage(String groupName, Map<String, Integer> acks) {
		for(Entry<String, Integer> ack: acks.entrySet()){
			if(ack.getValue() > delivered.get(groupName).get(ack.getKey())){
				return true;
			}
		}
		return false;
	}

	private void sendNACK(int groupSeqNum, String groupName) {
		// TODO Auto-generated method stub
		
	}

	private void handleHoldBackQueue(String groupName, String src) {
		List<TimeStampedMessage> li = holdBackQueues.get(groupName).get(src);
		HashMap<String, Integer> groupDelivers = delivered.get(groupName);
		ListIterator<TimeStampedMessage> listItor = li.listIterator();
		while(listItor.hasNext()){
			TimeStampedMessage tm = listItor.next();
			if(tm.getGroupSeqNum() == groupDelivers.get(src) + 1){
				groupDelivers.put(src, groupDelivers.get(src) +1);
				deliver(tm);
			}
			else{
				break; // no more expected
			}
		}

	}

	private void deliver(TimeStampedMessage m) {
		System.out.println("Unimplemented yet. Delivering Message to app ");
	}

}
