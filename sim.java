import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
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
// -c cache size -a access file -u user config file
	public static void main(String[] args) throws IOException {
    	int cacheSize = 50;
    	String asFile = "/Volumes/src/jxu/sim/src/as/query20k-data688-10days-uniform.txt.as";
    	String uFile = "";
    	String oFile = "output";
        for (int i = 0; i<args.length; ++i) {
        	String s = args[i];
        	if (s.compareTo("-c") == 0) {
        		++i;
        		cacheSize = Integer.parseInt(args[i]);
        	} else if (s.compareTo("-u") == 0) {
        		++i;
        		uFile = args[i];
        	} else if (s.compareTo("-a") == 0) {
        		++i;
        		asFile = args[i];
        	} else if (s.compareTo("-o") == 0) {
        		++i;
        		oFile = args[i];
        	}
        }
		// Server
		int serverSize = 10;
		Server[] s = Server.getServers(serverSize);
		// User
		new UserProfileGenerator("userConfig.txt", "userProfile.txt");
		
		// Access
		// Data can't be cloned, since data's location changed during running
		Data[] d = Data.getDatas(s);
		// update
    	ArrayList<Event> e = new ArrayList<Event>(40000);
    	Update.getUpdate(d, e);
		// avoid the .svn folder
		// read access from disk
    	ArrayList<Access> a = new ArrayList<Access>(20000);
		Access.getAccess(d, a, asFile);
		
		User.addUser(a, "userProfile.txt", "query.txt");

		e.addAll(a);
		Collections.sort(e);

		//User[] u = User.getUsers();
		Writer output = new BufferedWriter(new FileWriter(new File(oFile)));

        Cache c = new Cache();
        c.init(cacheSize, e, d, s, output);
        c.run();
	}

}
