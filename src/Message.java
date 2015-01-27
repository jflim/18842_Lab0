import java.io.Serializable;

public class Message implements Serializable {

	public Message(String dest, String kind, Object data) {

	}

	// These setters are used by MessagePasser.send, not the app
	public void set_source(String source) {

	}

	public void set_seqNum(int sequenceNumber) {

	}

	public void set_duplicate(Boolean dup) {
		// other accessors, toString, etc as needed
	}
}
