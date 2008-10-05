import java.util.Set;


public class Cache {
    int size;
    int profit;
    int cacheAccessTime = 10;
    Event[] e;
    User[] u;
    Data[] d;
    Server[] s;
    // cached data
    Set<Data> c;

	public Cache(int size_) {
		size = size_;
	}

    public void run() {
		int accessNum = e.length;
		for (int i = 0; i<accessNum; ++i) {
			if (e[i] instanceof Access) {
                run((Access)e[i]);
			} else {
				run((Update) e[i]);

			}
		}

    }

	private void run(Access a) {

	}

	private void run(Update u) {

	}

}
