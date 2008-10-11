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
    static String asPath = "/Volumes/src/jxu/sim/src/as";
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// Server
		int serverSize = 10;
		Server[] s = Server.getServers(serverSize);
		// Data &
		Data[] d = Data.getDatas(s);
		// update
    	ArrayList<Event> e = new ArrayList<Event>(10000);
    	Update.getUpdate(d, e);
		// User

		// Access
        String[] asFiles = getListFiles(asPath);
        for (int i = 0; i < asFiles.length; ++i) {
        	java.io.File one = new java.io.File(asFiles[i]);
        	if (one.isFile()) {
        		// avoid the .svn folder
        		// read access from disk
        		ArrayList<Event> clone = (ArrayList<Event>)e.clone();
        		Access.getAccess(d, clone, asFiles[i]);
        		Collections.sort(clone);

        		//User[] u = User.getUsers();
                //Cache c = new Cache(50);
        	}
        }
	}

}
