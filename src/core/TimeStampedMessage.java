package core;
import Clock.ClockService;

import java.io.Serializable;

/**
 * Created by gs on 2/6/15.
 */
@SuppressWarnings("serial")
public class TimeStampedMessage extends Message implements Serializable{

    private ClockService timeStamp;

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
    
    /* getters    */
    
	public String getData() {
		return (String) this.data;
	}
	
	public int getSeqNum() {
		return this.seqNum;
	}
	public boolean getDup() {
		return this.dup;
	}
	
}
