import java.util.ArrayList;
import java.util.List;


public class sim {

	public static void main(String[] args) {
		// Server
		int serverSize = 10;
		Server[] s = Server.getServers(serverSize);
		// Data &
		Data[] d = Data.getDatas(s);
		// update
    	List<Event> u = new ArrayList<Event>(10000);
    	Update.getUpdate(d, u);
		// User

		// Access

		// read access from disk
		Access[] a = Access.getAccess();
		//User[] u = User.getUsers();
        Cache c = new Cache(50);
	}

}
