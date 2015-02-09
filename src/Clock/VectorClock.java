package Clock;

/**
 * Created by gs on 2/6/15.
 */
public class VectorClock extends ClockService{

	private int counters[]; //the timestamp
	private int size;
	private int selfIndex;
	
    public VectorClock(int selfIndex, int size){
		counters = new int[size];
		this.size = size;
		this.selfIndex = selfIndex;
    }

    @Override
    public void clockIncrement() {
    	counters[selfIndex]++;
    }

    @Override
	public void setClock(ClockService receivedClock) {
		int index;
		for (index = 0; index < size; index++) {
			this.counters[index] = Math.max(counters[index],
					((VectorClock) receivedClock).counters[index]);
		}
		this.counters[selfIndex]++; // receiver adds 1
	}

    public void setIndex(int index){
    	this.selfIndex = index;
    }
    
	private int getClockValue(int index) {
		return counters[index];
	}

	@Override
	public ClockService copy() {
        VectorClock clock = new VectorClock(selfIndex, size);
        int i = 0;
        for(i = 0 ; i < size; i++){
        	clock.counters[i] = this.counters[i];
        }
        return clock;
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
	public int compareTo(ClockService other){
//		int index;
//
//		int initialthis  = this.getClockValue(0);
//		int initialother = ((VectorClock) other).getClockValue(0);
//
		// set initial values
//		int status = compare(initialthis, initialother);
//
//		for(index = 1; index < size; index++){
//			int a = this.getClockValue(index);
//			int b = ((VectorClock)other).getClockValue(index);
//
//			int nextStatus = compare(a,b);
//			if(nextStatus == 0){
//				continue;
//			}
//			else if(status == 0){ // they were equal before.
//				status = nextStatus;
//			}
//			else if(nextStatus != status){ //concurrent
//				return 2;
//			}
//		}
//		return status;

        boolean less = false;
        boolean greater = false;
        for (int i = 0; i < size; i++) {
            int my = this.getClockValue(i);
            int oth = ((VectorClock) other).getClockValue(i);
            if (my < oth)
                less = true;
            else if (my > oth)
                greater = true;
        }
        if (less && !greater)
            return -1;
        else if (greater && !less)
            return 1;

        return 0;
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
		return counters;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		int i;
		if (size == 1) {
			return Integer.toString(counters[0]);
		}

		else {
			for (i = 0; i < size - 1; i++) {
				sb.append(counters[i]);
				sb.append(":");
			}
			sb.append(counters[size - 1]);
			return sb.toString();
		}
	}
}
