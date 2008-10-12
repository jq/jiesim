import java.util.ArrayList;
import java.util.LinkedList;


public class Data implements Comparable<Data>{
	static final int dataNum = 688;
	// when src updated, how many cache server will update at the same time
	static int updateNum = 1;
	static int cacheNum = 2;
    Server src;
    int seed = 0;
    Long time;
    ArrayList<Server> fresh = new ArrayList<Server>(cacheNum);
    ArrayList<Server> stale = new ArrayList<Server>(cacheNum);
    Data(Server s) {
    	src = s;
    	time = new Long(0);
    }

    public Server getRandomCacheServer() {
    	seed++;
    	Server s = stale.get(seed % stale.size());
    	if (s==null) {
    		throw new RuntimeException();
    	}
    	return s;
    }

    private static Server getFast(ArrayList<Server> sList) {
    	Server s = null;
    	for (int i = 0; i<sList.size(); ++i) {
    		Server ns = sList.get(i);
    		if (s == null || s.accessTime > ns.accessTime) {
    			s = ns;
    		}
    	}
    	return s;
    }

    public Solution getFreshSolution() {
    	Server s = getFast(fresh);
    	if (s==null || s.accessTime>src.accessTime){
    		s = src;
    	}
    	return new Solution(1, s.accessTime, this, false);
    }

    public ArrayList<Solution> getSolutions() {
    	ArrayList<Solution> slist = new ArrayList<Solution>(2);
    	Solution so = getFreshSolution();
    	slist.add(so);

    	// add stale
    	Server s = getFast(stale);
    	if (s!=null && so.getTime() > s.accessTime) {
        	slist.add(new Solution(1, src.accessTime, this, true));

    	}
    	return slist;
    }

    static Data[] getDatas(ArrayList<Server> s) {
        Data[] d = new Data[dataNum];
        int serverSize = s.size();
        for (int i = 0; i<dataNum; ++i) {
        	int srcNum = i%serverSize;
        	d[i] = new Data(s.get(srcNum));

        	// save cache
        	for (int j = 1; j<=cacheNum; ++j) {
        		int cacheNum = (srcNum + j) % serverSize;
        		d[i].stale.add(s.get(cacheNum));
        	}
        }
        return d;
    }

    public void update(Server s) {
    	if (s == src) {
    		stale.addAll(fresh);
    		fresh.clear();
    	} else {
    		stale.remove(s);
    		fresh.add(s);
    	}
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public int compareTo(Data arg0) {
		// TODO Auto-generated method stub
		return time.compareTo(arg0.time);
	}

}
