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

	// These setters are used by MessagePasser.send, not the app
	public void set_source(String source) {

	}

	public void set_seqNum(int sequenceNumber) {

	}

	public void set_duplicate(Boolean dup) {
	}
	
	// other accessors, toString, etc as needed
	
}
