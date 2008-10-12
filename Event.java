

public class Event implements Comparable<Event>{
    Long timestamp;

    public void run(Cache c) {}

	public int compareTo(Event o) {

		return timestamp.compareTo(o.timestamp);
	}
}
