import java.io.Writer;
import java.util.List;
import java.util.Set;


public class Cache {
    int size;
    double profit;

    int cacheAccessTime = 10;
    List<Event> e;
    User[] u;
    Data[] d;
    Server[] s;
    // cached data
    Set<Data> c;
    Writer o;

	public void init(int size_, List<Event> e_, Data[] d_, Server[] s_, Writer output) {
		size = size_;
		e = e_;
		d = d_;
		s = s_;
		o = output;
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
        result();
    }

	private void run(Access a) {
		// try the combination
	}

	private void run(Update u) {
		u.run();
	}

	public void result() {

	}
}
