import java.util.ArrayList;
import java.util.LinkedList;


public class Data {
	static final int dataNum = 688;
	// when src updated, how many cache server will update at the same time
	static int updateNum = 1;
	static int cacheNum = 4;
    Server src;
    int seed = 0;
    ArrayList<Server> fresh = new ArrayList<Server>(cacheNum);
    ArrayList<Server> stale = new ArrayList<Server>(cacheNum);
    Data(Server s) {
    	src = s;
    }

    public Server getRandomCacheServer() {
    	seed++;
    	return stale.get(seed % stale.size());
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

}
