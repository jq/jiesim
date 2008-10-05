
public class Server {
    int accessTime;
    Server(int time) {
    	accessTime = time;
    }

    static Server[] getServers(int size) {
        Server[] s = new Server[size];
        for (int i = 0; i<size; ++i) {
        	s[i] = new Server (1000);
        }

        return s;
    }
	public Server() {
		// TODO Auto-generated constructor stub
	}

}
