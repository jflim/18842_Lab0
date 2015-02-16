package core;
import Clock.ClockService;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by gs on 2/6/15.
 */
@SuppressWarnings("serial")
public class TimeStampedMessage extends Message implements Serializable{

    private ClockService timeStamp;
    private String groupName = null;
    
    // Multicast services
    private int groupSeqNum = -1;
    private Map<String, Integer> ACKs;
    boolean NACK;
    
	public TimeStampedMessage(String dest, String kind, Object data,
			ClockService timeStamp) {
		super(dest, kind, data);
        this.timeStamp = timeStamp.copy();

    }

    public TimeStampedMessage(TimeStampedMessage message) {
        super(message);
        this.timeStamp = message.timeStamp.copy();
    }

    public TimeStampedMessage(Message message, ClockService timeStamp) {
        super(message);
        this.timeStamp = timeStamp.copy();

    }

    public TimeStampedMessage(String dest, String kind, Object data,
        ClockService timeStamp, String num) {
        super(dest, kind, data);
        this.timeStamp = timeStamp.copy();
        this.groupName = num;

    }
    // These setters are used by MessagePasser.send, not the app
    public void set_source(String source) {
        this.src = source;
    }

    public void set_seqNum(int sequenceNumber) {
        this.seqNum = sequenceNumber;
    }

    public void set_duplicate(Boolean dup) {
        this.dup = dup;
    }

    public ClockService getTimeStamp(){
        return this.timeStamp;
    }
    
    public void setGroupName(String groupName){
    	this.groupName = groupName;
    }
    
    public void addGroupSeqNum(int gsn){
    	this.groupSeqNum = gsn;
    }
    
    public void addACKs(Map<String, Integer> R){
    	this.ACKs = R;
    }
    
    public void setNACK(boolean value){
    	NACK = value;
    }
    
    /* getters    */
    
    public String getGroupName(){
    	return this.groupName;
    }
    
    public int getGroupSeqNum(){
    	return this.groupSeqNum;
    }
    
    public Map<String, Integer> getACKS(){
    	return this.ACKs;
    }
    
	public String getData() {
		return (String) this.data;
	}
	
	public int getSeqNum() {
		return this.seqNum;
	}

    public boolean getDup() {
		return this.dup;
	}

    public String get_source(){
        return this.src;
    }

    public String get_dst() {
        return this.dest;
    }
	
}
