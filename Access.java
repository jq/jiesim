

public class Access extends Event implements Scanner{
	static int accessNum = 100;
    int[] data;
    int user;
    public Object scan(String line) {

    }
    public static Access[] getAccess() {
    	Access[] a = new Access[accessNum];
    	return a;
    }
    // generate access files
	public static void main(String[] args) {
        int users = User.UserNumber;
	}

}
