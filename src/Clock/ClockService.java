package Clock;

import java.util.Map;
/**
 * Created by gs on 2/6/15.
 */
public abstract class ClockService {
    private static ClockService clock = null;
    public static ClockService newClock(boolean isLogicalClock, int selfIndex, int size){

        if(isLogicalClock){
            clock = new LogicalClock();
        }
        else{
            clock = new VectorClock(selfIndex, size);
        }

        return clock;
    }

    public abstract void clockIncrement();
    public abstract void setClock(ClockService receivedClock);
    public abstract Object getClock();
    public abstract ClockService copy();
    public abstract int compareTo(ClockService clock);
}
