import java.util.List;
import java.util.Set;


public class Cache {
    int size;
    int profit;
    int cacheAccessTime = 10;
    List<Event> e;
    User[] u;
    Data[] d;
    Server[] s;
    // cached data
    Set<Data> c;

	public void init(int size_, List<Event> e_, Data[] d_, Server[] s_) {
		size = size_;
		e = e_;
		d = d_;
		s = s_;
	}

    public void run() {
		int accessNum = e.size();
		for (int i = 0; i<accessNum; ++i) {
			Event ev = e.get(i);
			if (ev instanceof Access) {
                run((Access)ev);
			} else {
				run((Update) ev);

			}
		}

    }

	private void run(Access a) {

	}

	private void run(Update u) {

	}

	public void result(String file) {

	}
}
