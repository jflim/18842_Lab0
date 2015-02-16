package core;
import java.io.Serializable;

public class Message implements Serializable {

	String src;
	String dest;
	String kind;
	int seqNum;
	Boolean dup;
	Object data; //content of the message
	
	public Message(String dest, String kind, Object data) {
		this.dest = dest;
		this.kind = kind;
		this.dup = false; //default value	
		this.data = data;
	}
    public Message(Message message) {
        this.dest = message.dest;
        this.kind = message.kind;
        this.dup = message.dup; //default value
        this.data = message.data;
        this.seqNum = message.seqNum;
        this.src = message.src;
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

    public void set_dst(String dst) {
        this.dest = dst;
    }
	// other accessors, toString, etc as needed
	
}
