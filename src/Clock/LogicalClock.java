package Clock;

/**
 * Created by gs on 2/6/15.
 */
public class LogicalClock extends ClockService{
    private int counter = 0;
    public LogicalClock(){
        this.counter = 0;
    }

    public void clockIncrement() {
        this.counter++;
    }

    public void setClock(ClockService receivedClock) {
        this.counter = Math.max(counter, ((LogicalClock)receivedClock).counter) + 1;
    }

    public Object getClock(){
        return this.counter;
    }

    @Override
    public ClockService copy() {
        LogicalClock clock = new LogicalClock();
        clock.counter = this.counter;
        return clock;
    }
    public int compareTo(ClockService clock) {
        LogicalClock otherClock = (LogicalClock)clock;
        if (this.counter < otherClock.counter)
            return -1;
        if (this.counter > otherClock.counter)
            return 1;
        return 0;
    }

	@Override
	public String toString() {
		return Integer.toString(counter);
	}
}
