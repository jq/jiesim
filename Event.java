import java.util.Comparator;
import java.util.Date;


public class Event implements Comparator{
    Date timestamp;

	public int compare(Object o1, Object o2) {
		// TODO Auto-generated method stub
		Date t1 = (Date) o1;
		Date t2 = (Date) o2;

		return t1.compareTo(t2) ;
	}
}
