

public class Event implements Comparable{
    Long timestamp;

    public void run(Cache c) {}

	public int compareTo(Object o) {

		return timestamp.compareTo(((Event)o).timestamp);
	}
}
