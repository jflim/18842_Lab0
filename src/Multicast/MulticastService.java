package Multicast;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.Group;
import core.MessagePasser;
import core.TimeStampedMessage;
public class MulticastService {

	
	Map<String, Integer> gSeqNum;
	Map<String, Map<String, Integer>> delivered; //Group for each <member, delivered>
	Map<String, List<TimeStampedMessage>> holdBackQueue;
	MessagePasser mp;
	
	public MulticastService(MessagePasser mp) {
		
		gSeqNum = new HashMap<String, Integer>();
		delivered = new HashMap<String, Map<String, Integer>>();
		holdBackQueue = new HashMap<String, List<TimeStampedMessage>>();
		
		this.mp = mp;  // get a reference to a MessagePasser
	}
	
	// TODO: assume group is not empty..
	/**
	 *  called only in initialization stages of Multicast Service
	 *  used to add a single group into the service's maps
	 * @param g
	 */
	void addGroup(Group g){
		gSeqNum.put(g.getName(), 0);
		Map<String, Integer> deliverGroup = new HashMap<String, Integer>();
		for(String memberName: g.memberNames()){
			deliverGroup.put(memberName, 0);
		}
		delivered.put(g.getName(), deliverGroup);
	}
	
	void send_multicast(String groupName, TimeStampedMessage m) throws FileNotFoundException{
		int selfGroupSeqNum = this.gSeqNum.get(groupName);
		
		for(String targetNode : delivered.keySet()){
			TimeStampedMessage tm = new TimeStampedMessage(m);
			tm.addGroupSeqNum(selfGroupSeqNum);
			tm.addACKs(delivered.get(groupName));
			
			//set the dest
			tm.set_dst(targetNode);
			mp.send(tm);
		}
	}
	
	void receive_multicast(String groupName, TimeStampedMessage m){
		
		int R_for_sender = delivered.get(groupName).get(m.get_source());

		// decide what to do now
		if(m.getGroupSeqNum() == R_for_sender + 1){ // next expected Message
			deliver(m);
			checkHoldBackQueue(groupName);
		}
	}

	private void checkHoldBackQueue(String groupName) {
		// TODO Auto-generated method stub
		
	}

	private void deliver(TimeStampedMessage m) {
		System.out.println("Unimplemented yet. Delivering Message to app ");
	}

}
