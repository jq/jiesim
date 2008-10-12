import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;


public class Cache {
	static final int	FIFO_ALL = 0;
    int cachesize;
    double profit;

    static int cacheAccessTime = 100;
    int totalSuccess;
    List<Event> e;
    ArrayList<User> u;
    Data[] d;
    Server[] s;
    // cached data
    LinkedList<Data> fresh = new LinkedList<Data>();
    LinkedList<Data> stale = new LinkedList<Data>();
    Writer o;

    public static Cache getCache(int t) {
    	switch (t) {
    	case FIFO_ALL:
    	    return new Cache();
    	default:

    	    return new Cache();
    	}
    }

	public void init(int size_, List<Event> e_, Data[] d_, Server[] s_,
			Writer output, ArrayList<User> u_) {
		cachesize = size_;
		e = e_;
		d = d_;
		s = s_;
		o = output;
		u = u_;
	}

    public void invalidate(Data data) {
    	if (fresh.remove(data)) {
    		stale.addFirst(data);
    	}
    }

    public boolean inCacheFresh(Data data) {
    	return fresh.contains(data);
    }

    public boolean inCacheStale(Data data) {
    	return stale.contains(data);
    }

    // a recent access to the data, and data is in cache
    // we adjust its position in cache
    public void adjustCache(Data data, boolean isStale) {
    	// must be in the cache
    	if (isStale) {
    		if (stale.remove(data)) {
    			stale.addLast(data);
    		}
    	} else {
    		if (fresh.remove(data)) {
    			fresh.addLast(data);
    		}
    	}
    }

    // a new data add to cache
    public void addToCache(Data data, boolean isStale) {
    	if (fresh.size() + stale.size() == cachesize) {
        	if (stale.size() > 0) {
        		stale.removeFirst();
        	} else {
        		fresh.removeFirst();
        	}

    	}
		if (isStale) {
			stale.addLast(data);
		} else {
		    fresh.addLast(data);
		}
    }
    public void run() throws IOException {
		int accessNum = e.size();
		for (int i = 0; i<accessNum; ++i) {
			Event ev = e.get(i);
			ev.run(this);
		}
        result();
    }

	public void result() throws IOException {
        o.write("profit:" + Double.toString(profit) + "\n");
        for (int i = 0; i<u.size(); ++i) {
        	o.write(u.get(i).getString());
        }
        o.close();
	}
}
