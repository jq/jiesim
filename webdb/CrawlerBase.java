/*
* CrawlerBase.java 
* constants definition
*/
package webdb;

public class CrawlerBase implements java.io.Serializable {

	//OVERALL USER TYPES
	public static final int PAGE_USER = 10;
	public static final int ACCESS_USER = 11;
	
	//PAGE USER TYPES
    public static final int PAGE_NAIVE_USER			       = 0;
	public static final int PAGE_NORMAL_USER			   = 1;
    public static final int PAGE_SMART_USER				   = 2;
	public static final int PAGE_ADVANCED_USER             = 3;
	
	//ALGORITHMS
	public static final int UNIFORM_RANDOM				   = 1;
	public static final int FREQUENCY_BASED_RANDOM		   = 2;
	public static final int COUNT_BASED_RANDOM			   = 3;
	public static final int COUNT_BASED_DETERMENISTIC	   = 4;
	public static final int POPULARITY_RANDOM			   = 5;
	public static final int USER_SCORE					   = 6;

    //SCHEDULING ALGORITHM TYPES    
    /*public static final int SBR           = 2;
    public static final int USER_CENTRIC  = 4;
    public static final int UIC			  = 8;*/
    
	//TIME UNIT, HOUR -> MINUTE BASED, DEFAULT IS ONE DAY
	public static final int TIME_UNIT		   = 24 * 60;
	public static final int BUDGET_UNIT		   = 30;
	//time block length in minutes
    public static final int BLOCK_LEN_IM       = TIME_UNIT;
	//convert form ms to hour
	public static final int MS_HOUR            = 1000 * 60 * 60;
	//convert form ms to minute
	public static final int MS_MINUTE = 1000 * 60;
	//convert form minute to hour
	public static final int MIN_HOUR = 60;
	
	//repeat runs for random algorithms
	public static final int REPEAT_TIMES					   = 100;
	
	//ratio of past 1 unit vs past 4 units (last 5th - last 2nd units) in future score computation
	public static final double alpha		   = 0.8;
	//ratio of past score vs future score
	public static final double beta 		   = 0.8;
	//ratio of past score vs future score in advanced algorithm
	public static final double delta 		   = 0.5;
	
	//probability of correct update in computing smart score
	public static final double prob 		   = 1;
	
    public static final int START_YEAR         = 2007;
	public static final int END_YEAR           = 2007;
	public static final int START_MON          = 9;
	public static final int END_MON            = 9;
	public static final int START_DAY          = 19;
	public static final int END_DAY            = 24;

    //jdbc connection settings
    private String dbdriver = "com.mysql.jdbc.Driver";
    private String dburl    = "jdbc:mysql://localhost/epic";
    private String user     = "epic";
    private String password = "epic";
    
    protected java.sql.Connection connection = null;
    /**
     * Connect to db with specified url, user id and password
     */
    protected void makeConnection () throws java.sql.SQLException
    {
        try {
            Class.forName (this.dbdriver);
            connection = java.sql.DriverManager.getConnection(dburl, user, password);
        }
        catch (ClassNotFoundException e3) {
            throw new java.sql.SQLException ("Driver not found");
        }
    }
	
    /**
     * disconnect from db
    */
    protected void disconnect ()  throws java.sql.SQLException
    {
        if (connection != null) {
            connection.close ();
            connection = null;
        }
    }

	protected java.sql.Timestamp toSqlTimestamp(java.util.Date date)
	{
		if (date == null)
			return null;
		return new java.sql.Timestamp(date.getTime());
	}
    
	
	protected final boolean debugging = false;
    /**
     * Output message if in debug mode
     */
    protected void trace (String msg)
    {
        if (debugging)
            System.out.println (msg);
    }
}
