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
    	return stale.get(seed % stale.size());
    }

    public ArrayList<Solution> getSolutions() {
    	ArrayList<Solution> slist = new ArrayList<Solution>(fresh.size() + stale.size()+1);
    	slist.set(0, new Solution(1, src.accessTime, this, false));

    	int i = 1;
    	for (int j = 0; j<fresh.size(); ++j, ++i) {
    		slist.set(i, new Solution(1, fresh.get(j).accessTime, this, false));
    	}
    	for (int j = 0; j<stale.size(); ++j, ++i) {
    		slist.set(i, new Solution(1, stale.get(j).accessTime, this, true));
    	}
    	return slist;
    }

    static Data[] getDatas(Server[] s) {
        Data[] d = new Data[dataNum];
        int serverSize = s.length;
        for (int i = 0; i<dataNum; ++i) {
        	int srcNum = i%serverSize;
        	d[i] = new Data(s[srcNum]);

        	// save cache
        	for (int j = 1; j<=cacheNum; ++j) {
        		int cacheNum = (srcNum + j) % serverSize;
        		d[i].stale.add(s[cacheNum]);
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
