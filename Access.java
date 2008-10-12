import java.util.Date;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import webdb.Util;

public class Access extends Event {
	static int accessNum = 100;
    Data[] data;
    User u;
    int queryID;

    static String pathPostfix = ".as";
	//static String path = "../../src/update";
	static String origPath = "../access/";

    Access (int q, Long time, Data[] d_) {
    	queryID = q;
    	timestamp = time;
    	data = d_;
    }

    public void run(Cache c) {
		// try the combination
		// if you get two data from one server, choose the longer time
    	if (!u.hasMoney()) {
    		return;
    	}
    	int time = 0;
    	int frashData = 0;
    	Solutions s = new Solutions();
    	for (int i = 0; i<data.length; i++) {
    		Data d = data[i];
    		int dTime = 0;
        	if (c.inCacheFresh(d)) {
        		// no need to add to solution, since it is just one choice
        		s.insertCacheFresh();
        		c.adjustCache(d, true);

        	} else if (c.inCacheStale(d)) {
        		// just try src of data and cache
        	    ArrayList<Solution> ss = new ArrayList<Solution>(2);
        	    Solution staleCache = new Solution(1, Cache.cacheAccessTime, d, true);
        	    Solution freshServer = new Solution(1, d.src.accessTime, d, false);
        	    ss.set(0, staleCache);
        	    ss.set(1, freshServer);
        	    s.insert(ss);
        	} else {
        		// get it from servers
        		ArrayList<Solution> ss = d.getSolutions();
        		s.insert(ss);
        	}
    	}
    	c.profit += s.pay(u, data.length, c);
    	c.totalSuccess++;
    }

    /*
     * @input: one access file in reorged format: queryID|data1,data2,...,datan|arrTime
     * @output: one access object
     */
    static void getAccess(Data[] d, List<Access> a, String filename) {
    	try {
	        java.io.BufferedReader reader = new  java.io.BufferedReader (
	        new java.io.InputStreamReader (new java.io.FileInputStream (filename)));

	        String line = reader.readLine ();
	        while (line != null) {
                java.util.StringTokenizer st = new java.util.StringTokenizer (line, "|");
                if (st.countTokens() == 3) {
	                int qid = Integer.parseInt(st.nextToken ());
	                String dStr = st.nextToken();
	                java.util.StringTokenizer dt = new java.util.StringTokenizer (dStr, ",");
	                int dataSize = dt.countTokens();
	                Data[] data = new Data[dataSize];
	                for (int i = 0; i < dataSize; ++i) {
			            int dID = Integer.parseInt(dt.nextToken ());
			            data[i] = d[dID];
	                }
	                Date t = Util.toDate (st.nextToken ());
		            if (t != null) {

				    	Access access = new Access(qid, t.getTime(), data);
				    	a.add(access);
		            }
                }
                line = reader.readLine();
	        }
    	}   catch (java.io.IOException ioe) {
            ioe.printStackTrace ();
		}

    }

    static void saveAccessToShorterFormat() throws IOException {
		java.io.File start = new java.io.File (origPath);
        String list[]=start.list();
        int total = (list == null)?0:list.length;
        assert(total == Data.dataNum);

        for (int i = 0; i < total; i++) {
            java.io.File one = new java.io.File (origPath, list[i]);
            String fullpath = origPath;
            if (!fullpath.endsWith (java.io.File.separator))
                fullpath += java.io.File.separator;
            fullpath += list[i];
            if (!one.isDirectory ()) {
            	String outPath = fullpath + pathPostfix;
                Writer output = new BufferedWriter(new FileWriter(new File(outPath)));
                saveAccess(fullpath, output);
                output.close();
            }
        }
    }

    /**
     * @input access file, e.g.:query20k-data688-10days-uniform.txt
     * sample:  userID|queryID|dataID|arrTime
     * 			0|18877|10|21 Oct 2007 03:10
	 * 			0|18877|31|21 Oct 2007 03:10
	 *			0|18877|35|21 Oct 2007 03:10
	 *			0|18877|60|21 Oct 2007 03:10
	 *			0|18877|70|21 Oct 2007 03:10
	 *
	 * @output: queryID|data1,data2,...,datan|arrTime
     */
    private static void saveAccess (String file, Writer output)    {
		try {
            java.io.BufferedReader reader = new  java.io.BufferedReader (new java.io.InputStreamReader (new java.io.FileInputStream (file)));

            String line = reader.readLine ();
            int beforeQid = -1;
            StringBuilder b = new StringBuilder(102400);

            //process first line separately
            java.util.StringTokenizer st = new java.util.StringTokenizer (line, "|");
            if (st.countTokens() < 4) {
            	int i = 0;
            	i++;
            }

            //pass userID
        	st.nextToken ();

        	int currentQid = Util.toInteger(st.nextToken (), 0);
            if (beforeQid == -1) {
            	beforeQid = currentQid;
            }
        	b.append(String.valueOf(currentQid));
        	b.append('|');

            //dataID
            String data = st.nextToken();
            b.append(data);

            //get arrTime, will append after appending all dataID in the same query
            String time = st.nextToken();

            //start processing 2nd line to the end;
            line = reader.readLine ();
            while (line != null) {
                st = new java.util.StringTokenizer (line, "|");
                if (st.countTokens() < 4) {
                	line = reader.readLine ();
                	continue;
                }

                //pass userID
                st.nextToken ();

                currentQid = Util.toInteger(st.nextToken (), 0);
                data = st.nextToken();

                //same query
                if (beforeQid == currentQid){
                	b.append(',');
                	b.append(String.valueOf(data));
			    } else { //next query starts
			    	//finish writing previous one
		    		b.append('|');
		    		b.append(time);
		    		b.append('\n');
			    	beforeQid = currentQid;
			    	//save time for next query, start writing next query
			    	time = st.nextToken();
			    	b.append(currentQid);
			    	b.append('|');
			    	b.append(String.valueOf(data));
				}
                //output.write(b.toString());
                line = reader.readLine ();
            }
            b.append('|');
    		b.append(time);
    		b.append('\n');

            if (b.length() != 0) {
            	b.append('\n');
            	//String s = b.toString();
            	char[] c = new char[(int)(b.length()* 1.2)];
            	b.getChars(0, b.length(), c, 0);
                output.write(c);
            }
			reader.close();
		}        catch (java.io.IOException ioe) {
            ioe.printStackTrace ();
		}
    }

    public static Access[] getAccess() {
    	Access[] a = new Access[accessNum];
    	return a;
    }
    // generate access files
	public static void main(String[] args) throws IOException{
		saveAccessToShorterFormat();
	}

}
