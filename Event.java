import java.util.Comparator;
import java.util.Date;


public class Event implements Comparator{
    Long timestamp;

	public int compare(Object o1, Object o2) {
		// TODO Auto-generated method stub
		Long t1 = (Long) o1;
		Long t2 = (Long) o2;

		return t1.compareTo(t2) ;
	}
}
