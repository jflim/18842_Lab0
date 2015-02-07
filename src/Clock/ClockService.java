package Clock;

import java.util.Map;
/**
 * Created by gs on 2/6/15.
 */
public abstract class ClockService {
    private static ClockService clock = null;
    public static ClockService newClock(boolean isLogicalClock, String hostName){

        if(isLogicalClock){
            clock = new LogicalClock();
        }
        else{
            clock = new VectorClock();
        }

        return clock;
    }

    public abstract void clockIncrement();
    public abstract void setClock(ClockService receivedClock);
    public abstract Object getClock();
    public abstract ClockService copy();
}
