package Multicast;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import core.Group;
import core.Message;
import core.MessagePasser;
import core.Nack;
import core.TimeStampedMessage;

public class MulticastService {

	public enum State{
		HELD, WANTED, RELEASED,
	}
	
	// class variables
	Map<String, Integer> gSeqNum;
	Map<String, HashMap<String, Integer>> delivered; // Group for each <member, delivered>
	Map<String, HashMap<String, List<TimeStampedMessage>>> holdBackQueues; // group, list
	Map<String, HashMap<String, List<TimeStampedMessage>>> caches;
	MessagePasser mp;
	
	// cachesize 
	int cacheSize = 3;

	// added for Mutual Exclusion functionality
	boolean voted = false;
	State state = State.RELEASED;
	// queue holding unprocessed requests for critical section
	List<TimeStampedMessage> queueCS = new LinkedList<TimeStampedMessage>(); 
	
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

	public synchronized void send_multicast(String groupName, TimeStampedMessage m, boolean resent)
			throws FileNotFoundException {

		/*
		// count its own as delivered.
		String self = mp.getLocalName();
		HashMap<String, Integer> groupDelivers = delivered.get(groupName);
		groupDelivers.put(self, groupDelivers.get(self)+1);
		 *
		 */
		
		if(!resent){
			//increment the sequence number before sending
			this.gSeqNum.put(groupName, gSeqNum.get(groupName)+1);
		}
		
		int selfGroupSeqNum = this.gSeqNum.get(groupName);
		
		//System.out.println("Delivered ACKs in multicast");
		//System.out.println(delivered.get(groupName));
		for (String targetNode : delivered.get(groupName).keySet()) {
				
			TimeStampedMessage tm = new TimeStampedMessage(m);
		
			// don't resend to original sender
			// and don't change the values from before
			if(resent){/*
				if(targetNode.equals(mp.getLocalName()) || // don't resend to self
						targetNode.equals(tm.get_source()) || // don't resend to sender
						targetNode.equals(tm.getOrigSender())){ // don't resend to sender
					continue;
				}
				*/
				tm.changeSender(mp.getLocalName());	
			}
			else{
				tm.addGroupSeqNum(selfGroupSeqNum);
				tm.addACKs(delivered.get(groupName));
			}
			
			// set the dest
			tm.set_dst(targetNode);
			mp.send(tm);
		}
		
		
	}

	public synchronized void receive_multicast(String groupName, TimeStampedMessage m) {

		if(m.getNACK() == true){
			handleNACK(m);
			return;
		}

		Map<String, Integer> groupDelivers = delivered.get(groupName);
		String sender = m.getOrigSender();
		if(sender == null){
			sender = m.get_source();
		}
		
		int R_for_sender = groupDelivers.get(sender);
	
		
		// decide what to do now
		if (m.getGroupSeqNum() <= R_for_sender) { // already handled,
			// duplicate
			return;
		} 

		// send to everyone else if not received before
		try {
			TimeStampedMessage copy = new TimeStampedMessage(m);
			send_multicast(groupName, copy, true);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// insert message into queue
		insertInHoldBackQueue(groupName, m);

		// not delivered and not next expected from this sender
		// send any NACKS for missing messages
		if (m.getGroupSeqNum() > R_for_sender + 1) {
			List<Nack> NacksToSend = seenMissingMessages(groupName, m.getACKS());
			while(!NacksToSend.isEmpty()){
				
				try {
					 // request missed packet from someone
					Nack next = NacksToSend.remove(0);
					sendNACK(next);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			return;
		}
		
		if (m.getGroupSeqNum() == R_for_sender + 1) { // next expected Message
			System.out.println("next expected from a process");
			handleHoldBackQueue(groupName);
		}
	}

	private void sendNACK(Nack next) throws FileNotFoundException {
		System.out.println("sendNack time");
		String groupName = next.getGroupName();
		int groupSeqNum = next.getGroupSeqNum();
		String missingSender = next.getSender();
		
		// send NACK to everyone in group except self for the missing message
		for (Entry<String, Integer> node : delivered.get(groupName).entrySet()) {
			
			// don't send NACK to self
			if(node.getKey().equals(mp.getLocalName())){
				continue;
			}
			
			String request = next.toString();
			Message m = new Message(node.getKey(), "NACK", request);
			TimeStampedMessage t = new TimeStampedMessage(m, mp.getClock());

			// set multicast fields
			t.setNACK(true);
			t.setGroupName(groupName);

			// set regular fields
			t.set_source(mp.getLocalName());
			t.set_seqNum(mp.incSequenceNumber()); // increments seq number
											      // before sending

			System.out.println("Sending a NACK to " + node.getKey());
			System.out.println("NACK request: " + next.toString());
			mp.send(t);
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
		String source = m.getOrigSender();
		if(source == null){
			source = m.get_source();
		}
		
		if(groupQueue.containsKey(source)){
			List<TimeStampedMessage> hbList = groupQueue.get(source);
			groupQueue.get(source).add(m);
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
			groupQueue.put(source, li);
		}

	}

	/**
	 * Compares each ACK to figure out if a message wasn't received
	 * @param acks
	 * @return
	 */
	private List<Nack> seenMissingMessages(String groupName, Map<String, Integer> acks) {
		System.out.println("seenMissingMessages");
		List<Nack> neededNacks = new LinkedList<Nack>();
		for(Entry<String, Integer> ack: acks.entrySet()){
			int nextExpected = delivered.get(groupName).get(ack.getKey());
			while(ack.getValue() > nextExpected){
				
				// TODO: catch to change
				//if(nextExpected == 0){
				//	nextExpected++;
				//}
				
				// send NACK to everyone in the group.
				Nack req = new Nack(groupName, ack.getKey(), nextExpected);
				neededNacks.add(req);
				nextExpected++;
			}
		}
		return neededNacks;
	}

	private void handleHoldBackQueue(String groupName) {

		// locate the smallest R in this group's holdBackQueues.
        HashMap<String, List<TimeStampedMessage>> holdqueue = holdBackQueues.get(groupName);

		while (true) {
			TimeStampedMessage minAcksMessage = null;
			for (String process : holdqueue.keySet()) {
				boolean isLess = true;
				if (holdqueue.get(process) == null
						|| holdqueue.get(process).size() == 0) {
					continue;
				}

				// assume that sorted by groupseqnum per process --> sorted by
				// min R 0in process's HoldBackQueues
				
				// hack: delete things that should have been handled already.
				TimeStampedMessage t = holdqueue.get(process).get(0);
				/*while (t != null) {
					if (t.getSeqNum() <= delivered.get(groupName).get(process)) {
						holdqueue.get(process).remove(t);
						t = holdqueue.get(process).get(0);
					} else {
						break;
					}
				}*/
				
				if (minAcksMessage == null) {
					minAcksMessage = t;
					continue;
				} else { // compare ACKS to see which one is smaller
					for (String name : t.getACKS().keySet()) {
						if (t.getACKS().get(name) > minAcksMessage.getACKS()
								.get(name)) {
							isLess = false;
						}
					}
					if (isLess == true) {
						minAcksMessage = t;
					}
				}
			}

			// do nothing if no messages in the holdbackqueues.
			if(minAcksMessage == null){
				return; 
			}
			
			//System.out.println("GroupSeqNum of minAck: " + minAcksMessage.getGroupSeqNum());
			//System.out.println("ACKS of minAck: " + minAcksMessage.getACKS());
		
			// check how many messages in minAcksMessage ACKs are not delivered
			// for the local

			HashMap<String, Integer> groupDelivers = delivered.get(groupName);

			int difference = 0;
			for (String name : groupDelivers.keySet()) {
				int localDel = groupDelivers.get(name);
				int minACKsMessageDel = minAcksMessage.getACKS().get(name);
				if (groupDelivers.get(name) < minAcksMessage.getACKS().get(name)) {
					difference += (minACKsMessageDel-localDel);
				}
			}
			
			//System.out.println("diff: " + difference);
			if (difference <= 1) {
				
				// account for a resent message
				String name = minAcksMessage.getOrigSender();
				if(name == null){
					name = minAcksMessage.get_source();
				}
				
				//System.out.println(name + "::" + groupDelivers.get(name));
				//System.out.println("minACKSEqNUM: " + minAcksMessage.getGroupSeqNum());
				if (minAcksMessage.getGroupSeqNum() == groupDelivers.get(name) + 1) {
					groupDelivers.put(name, groupDelivers.get(name) + 1);
					holdqueue.get(name).remove(minAcksMessage);
					TimeStampedMessage expectedMessage = new TimeStampedMessage(minAcksMessage);
					deliver(expectedMessage);
					continue;
				}
				//hack remove
				else if(minAcksMessage.getGroupSeqNum() < groupDelivers.get(name) + 1){
					holdqueue.get(name).remove(minAcksMessage);
				}

			} else {
				// send any NACKS for missing messages
				List<Nack> NacksToSend = seenMissingMessages(groupName,
						minAcksMessage.getACKS());
				while (!NacksToSend.isEmpty()) {

					try {
						// request missed packet from someone
						Nack next = NacksToSend.remove(0);
						sendNACK(next);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}
				return;
			}
		}
	}

	/**
	 * Handles incoming NACKs with the request missed message in the data section
	 * @param m
	 */
	public void handleNACK(TimeStampedMessage m){
		System.out.println("Handling a NACK from " + m.get_source());
		TimeStampedMessage cachedMessage = checkCache(m.getData());
		if(cachedMessage == null){
			System.err.println("Message from Group " + m.getGroupName()
					+ ", RequestData " + m.getData() + " not stored in cache");
		}
		else{
			cachedMessage.set_dst(m.get_source());
			mp.send(cachedMessage);
		}
	}
	
	private TimeStampedMessage checkCache(String data) {
		String[] tmp = data.split(", ");
		String groupName = tmp[0].split(":")[1].replaceAll("\\s","");
		String origSrc = tmp[1].split(":")[1].replaceAll("\\s","");
		int requestMessageSeqNum = Integer.parseInt(tmp[2].split(":")[1].replaceAll("\\s",""));
		
		//if cache not init yet here
		if(caches.get(groupName).get(origSrc) == null){
			return null;
		}
		
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
		m.displayMessageInfo("Received");		
		insertInCache(m);
		if(m.getKind().equals("Released")){
			handleReleasedCS();
		}
		else if(m.getKind().equals("Request")){
			handleRequestCS(m);
		}
	}


	private void insertInCache(TimeStampedMessage m) {
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
	
	public void displayDelivered(){
		Set<String> groups = delivered.keySet();
		for(String group : groups){
			System.out.println("GroupName: " + group);
			HashMap<String, Integer> groupDel = delivered.get(group);
			for(String member : groupDel.keySet()){
				System.out.println("Member: " + member + ", " + groupDel.get(member));
			}
		}
	}
	
	/**
	 * handles any received message from a process notifying
	 * that the CS is about to be released
	 */
	private void handleReleasedCS(){
		if(!queueCS.isEmpty()){
			TimeStampedMessage mes = queueCS.remove(0);
			mp.send(mes);
			voted = true;
		}
		else{
			voted = false;
		}
	}
	
	/**
	 * handles any received message from a process requesting
	 * access for the critical section, CS.
	 */
	private void handleRequestCS(TimeStampedMessage m) {
		if(state == State.RELEASED || voted == true){
			queueCS.add(m);
		}
		else{ // send an ACK for the request
			String name = m.getOrigSender();
			if(name == null){
				name = m.get_source();
			}
			Message newmes = new Message(name, "Request ACK", "Request ACK");
			TimeStampedMessage reply = new TimeStampedMessage(newmes, mp.getClock());
			
			// set regular fields
			reply.set_source(mp.getLocalName());
			reply.set_seqNum(mp.incSequenceNumber()); // increments seq number before sending
			
			mp.send(reply);
		}
		
	}
}
