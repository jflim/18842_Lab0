package Clock;

/**
 * Created by gs on 2/6/15.
 */
public class VectorClock extends ClockService{

	private int counters[]; //the timestamp
	private int size;
	
    public VectorClock(int size){
		counters = new int[size];
		this.size = size;
    }

    @Override
    public void clockIncrement() {
    	
    }

    @Override
    public void setClock(ClockService receivedClock) {

    }

	public int getClockValue(int index) {
		return counters[index];
	}

	@Override
	public ClockService copy() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Compares two VectorClock instances
	 * @param other
	 * @return
	 * -1 : this clock is smaller
	 *  0 : the clocks are the same
	 *  1 : this clock is larger
	 *  2 : concurrent clocks
	 */
	public int compareTo(VectorClock other){
		int index;
		
		int initialthis  = this.getClockValue(0);
		int initialother = other.getClockValue(0);
		
		// set initial values
		int status = compare(initialthis, initialother);
		
		for(index = 1; index < size; index++){
			int a = this.getClockValue(index);
			int b = other.getClockValue(index);
			
			int nextStatus = compare(a,b);
			if(nextStatus == 0){
				continue;
			}
			else if(status == 0){ // they were equal before.
				status = nextStatus;
			}
			else if(nextStatus != status){ //concurrent
				return 2;
			}
			
		}
		return status;
		
	}
	
	private int compare(int a, int b){
		if(a < b){
			return -1;
		}
		else if(a > b){
			return 1;
		}
		else{
			return 0;
		}
	}

	@Override
	public Object getClock() {
		return null;
	}
}
