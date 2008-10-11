import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class sim {
    static String[] getListFiles(String path) {
		java.io.File start = new java.io.File (path);
        String list[]=start.list();
        int total = (list == null)?0:list.length;
        for (int i = 0; i < total; i++) {
            String fullpath = path;
            if (!fullpath.endsWith (java.io.File.separator))
                fullpath += java.io.File.separator;
            fullpath += list[i];
            list[i] = fullpath;
        }
        return list;

    }

	@SuppressWarnings("unchecked")

	public static void main(String[] args) {
    	String asPath = "/Volumes/src/jxu/sim/src/as";
        //for
		// Server
		int serverSize = 10;
		Server[] s = Server.getServers(serverSize);
		// User

		// Access
        String[] asFiles = getListFiles(asPath);
        for (int i = 0; i < asFiles.length; ++i) {
        	java.io.File one = new java.io.File(asFiles[i]);
        	if (one.isFile()) {
        		// Data can't be cloned, since data's location changed during running
        		Data[] d = Data.getDatas(s);
        		// update
            	ArrayList<Event> e = new ArrayList<Event>(40000);
            	Update.getUpdate(d, e);
        		// avoid the .svn folder
        		// read access from disk
            	ArrayList<Access> a = new ArrayList<Access>(20000);
        		Access.getAccess(d, a, asFiles[i]);


        		e.addAll(a);
        		Collections.sort(e);

        		//User[] u = User.getUsers();
                Cache c = new Cache();
                c.init(50, e, d, s);
        	}
        }
	}

}
