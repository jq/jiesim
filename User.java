
public class User {
	static int UserNumber;
    String config = "userConfig.txt";
    static User[] getUsers() {
		User[] u = new User[User.UserNumber];
		for (int i = 0; i<UserNumber; ++i) {
			u[i] = new User();
		}
        return u;
    }
    public int pay(int responseTime, float dataFresh) {
    	return 0;
    }
    
    
}
