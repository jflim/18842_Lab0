package core;

import java.io.Serializable;

public class Nack implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String groupName;
	String origSender;
	int groupSeqNumber;
	
	public Nack(String group_name, String orig_sender, int sequence_number){
		this.groupName = group_name;
		this.origSender = orig_sender;
		this.groupSeqNumber = sequence_number;
	}

	// getters
	
	public String getGroupName(){
		return groupName;
	}
	
	public String getSender(){
		return origSender;
	}
	
	public int getGroupSeqNum(){
		return groupSeqNumber;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("groupName: " + groupName + ", ");
		sb.append("origSender: " + origSender + ", ");
		sb.append("groupSeqNumber: " + groupSeqNumber);
		return sb.toString();
	}
	
}
