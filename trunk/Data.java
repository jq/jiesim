import java.util.LinkedList;


public class Data {
	static final int dataNum = 688;
	// when src updated, how many cache server will update at the same time
	static int updateNum = 1;
	static int cacheNum = 4;
    Server src;
    int seed = 0;
    LinkedList<Server> fresh = new LinkedList<Server>();
    LinkedList<Server> stale = new LinkedList<Server>();
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

	public Data() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
