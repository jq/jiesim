import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import webdb.Util;


public class Update extends Event{
	static String path = "update";
	//static String path = "../../src/update";
	static String origPath = "../../Upd68810days";
	// Ms
	static int updateTime = 1000;
    Data d;
    Server s;
    Update (Long time, Data d_) {
    	timestamp = time;
    	d = d_;
    }

    Update (Long time, Data d_, Server s_) {
    	timestamp = time;
    	d = d_;
    	s = s_;
    }

    public void run(Cache c) {
    	d.update(s);
    	if (d.src == s) {
    	    c.invalidate(d);
    	}
    }
    static void getUpdate(Data[] d, List<Event> u) {
    	try {
	        java.io.BufferedReader reader = new  java.io.BufferedReader (
	        new java.io.InputStreamReader (new java.io.FileInputStream (path)));
	        String line = reader.readLine ();
	        while (line != null) {
                java.util.StringTokenizer st = new java.util.StringTokenizer (line, "|");
                int id = Integer.parseInt(st.nextToken ());
                Data data = d[id];
                while (st.hasMoreTokens()) {
		            Date t = Util.toDate (st.nextToken ());
		            if (t != null) {
				    	Update update = new Update(t.getTime(), data, data.src);
				        Update cacheUpdate = new Update(t.getTime() + updateTime,
				        		data, data.getRandomCacheServer());
				    	u.add(update);
				    	u.add(cacheUpdate);
		            }

                }
                line = reader.readLine();
	        }
    	}   catch (java.io.IOException ioe) {
            ioe.printStackTrace ();
		}

    }


    static void saveUpdateToFastFormat() throws IOException {
		java.io.File start = new java.io.File (origPath);
        String list[]=start.list();
        int total = (list == null)?0:list.length;
        assert(total == Data.dataNum);
        Writer output = new BufferedWriter(new FileWriter(new File(path)));

        for (int i = 0; i < total; i++) {
            java.io.File one = new java.io.File (origPath, list[i]);
            String fullpath = origPath;
            if (!fullpath.endsWith (java.io.File.separator))
                fullpath += java.io.File.separator;
            fullpath += list[i];
            if (!one.isDirectory ()) {
                saveUpdate (fullpath, i, output);
            }
        }
        output.close();
    }

    /**
     * @return how many update records are parsed
     * sample: /projects/web.archive/rss/cnn/latest/01-Nov-2006/01-Nov-2006_00:30.rss|14721|14720|1 Nov 2006 00:15:25
     */
    private static void saveUpdate (String file, int d, Writer output)    {
		try {
            java.io.BufferedReader reader = new  java.io.BufferedReader (new java.io.InputStreamReader (new java.io.FileInputStream (file)));
            String line = reader.readLine ();
            int beforeSize = -1;
            StringBuilder b = new StringBuilder(512);
            while (line != null) {
                java.util.StringTokenizer st = new java.util.StringTokenizer (line, "|");
                if (st.countTokens() == 4) {
	                st.nextToken ();
	                st.nextToken ();
	                int currentSize = Util.toInteger (st.nextToken (), 0);
					if (beforeSize == -1) {
						beforeSize = currentSize;
						continue;
					}

				    if (beforeSize != currentSize){
				    	if (b.length() == 0) {
				    		b.append(String.valueOf(d));
				    	}
			    		b.append('|');
			    		b.append(st.nextToken ());
				    	beforeSize = currentSize;
					}
                }
                line = reader.readLine ();
            }
            if (b.length() != 0) {
            	b.append('\n');
                output.write(b.toString());
            }
			reader.close();
		}        catch (java.io.IOException ioe) {
            ioe.printStackTrace ();
		}
    }

    // read original file, and store as
    // data id, timestamp, timestamp
	public static void main(String[] args) throws IOException {
		saveUpdateToFastFormat();
	}
}
