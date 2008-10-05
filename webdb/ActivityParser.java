/**
 * Project: epic
 * File: ActivityParse.java
 * Author: Qinglan Li
 * Date: October 24, 2007
 * Version: 1.0
 * Description: Parse the trace and access files to import the result into db. 
   please run the following sql statements to create corresponding db tables:
  
    CREATE TABLE `UpdateUsers` (
      `UserUID` int(16) NOT NULL default 0,
      `UserPath` varchar(255) default NULL,
      PRIMARY KEY  (`UserUID`)
    );
    CREATE TABLE `QueryUsers` (
      `UserUID` int(16) NOT NULL default 0,
      `Name` varchar(50) default NULL,
      PRIMARY KEY  (`UserUID`)
    );
    CREATE TABLE `Queries` (
      `QueryUID` int(16) NOT NULL,      `UserUID` int(16) NOT NULL default 0,      `QueryTime` datetime default NULL,	  `TotalAccess` int(16) NOT NULL default 0,
      PRIMARY KEY  (`QueryUID`)
    );
    CREATE TABLE `Accesses` (
      `AccessUID` int(16) NOT NULL  AUTO_INCREMENT,      `QueryUID` int(16) NOT NULL default 0,      `WebPageID` int(16) NOT NULL default 0,      `AccessTime` datetime default NULL,
      PRIMARY KEY  (`AccessUID`)
    );
    CREATE TABLE `Updates` (
      `UpdateUID` int(16) NOT NULL AUTO_INCREMENT,      `UserUID` int(16) default NULL,      `WebPage` varchar(255) default NULL,      `ActualSize` int(16) NOT NULL default 0,      `NormalizedSize` int(16) NOT NULL default 0,
      `UpdateTime` datetime default NULL,
      PRIMARY KEY  (`UpdateUID`)
    );
 * 
 */
package webdb;

import java.sql.*;


public class ActivityParser extends CrawlerBase {
	//trace and access files path
	private String tracepath = "/afs/cs.pitt.edu/projects/admt/projects/epic/trace/Upd688/";
	private String accessfile = "/afs/cs.pitt.edu/projects/admt/projects/epic/trace/access/query20k-data688-5days-uniform.txt";


	String tables[]={"UpdateUsers", "QueryUsers", "Queries", "Accesses", "Updates"};
    
	public void process () throws java.sql.SQLException
    {
        this.makeConnection ();
		for (int i = 0; i < tables.length ; i++)
			clearTable(tables[i]);
        //If all log files in this folder are for the same user
        this.parseUpdateFiles (tracepath);
		this.parseAccessFile (accessfile);
        this.disconnect ();
    }

    /**
     * @Parse all log files in given folder and all its sub folders recursively
     * each user has one file to record all its update history
     */
    private void parseUpdateFiles (String path) throws java.sql.SQLException
    {
		java.io.File start = new java.io.File (path);
        String list[]=start.list();
        int total = (list == null)?0:list.length;
        for (int i = 0; i < total; i++) {
            java.io.File one = new java.io.File (path, list[i]);
            String fullpath = path;
            if (!fullpath.endsWith (java.io.File.separator))
                fullpath += java.io.File.separator;
            fullpath += list[i];
            if (one.isDirectory ()) {
			}
            else {
				UpdateUser user = new UpdateUser();
                user.setUserId (i+1);
				user.setUserType(CrawlerBase.PAGE_USER);
                this.saveUpdateUser (user, one.toString ());
                this.parseUpdate (user, fullpath);
            }
        }
    }    
	
    /**
     * @return how many update records are parsed
     * sample: /projects/web.archive/rss/cnn/latest/01-Nov-2006/01-Nov-2006_00:30.rss|14721|14720|1 Nov 2006 00:15:25
     */
    private int parseUpdate (UpdateUser user, String file) throws java.sql.SQLException
    {
        int count = 0;
		java.util.Date timestamp = new java.util.Date();
		try {
            java.io.BufferedReader reader = new  java.io.BufferedReader (new java.io.InputStreamReader (new java.io.FileInputStream (file)));
            String line = reader.readLine ();
			UpdateActivity actBefore = new UpdateActivity();
            while (line != null) {
                UpdateActivity activity = new UpdateActivity ();
                java.util.StringTokenizer st = new java.util.StringTokenizer (line, "|");
                if (st.hasMoreTokens ())
                    activity.setPage (st.nextToken ());
                if (st.hasMoreTokens ())
                    activity.setOrgSize (Util.toInteger (st.nextToken (), 0));
                if (st.hasMoreTokens ())
                    activity.setSize (Util.toInteger (st.nextToken (), 0));
                //the second half: timestamp
				if (st.hasMoreTokens ()){
					timestamp = Util.toDate (st.nextToken ());                    
					activity.setTimestamp (timestamp);
                }
				if ((count <= 0) || (activity.getSize() != actBefore.getSize())){
					//save to db
					this.saveUpdate(user, activity);
					actBefore = activity;
				}
                line = reader.readLine ();
				count++;
            }
			reader.close();			
		}
        catch (java.io.IOException ioe) {
            ioe.printStackTrace ();
		}
        return count;
    }

	/**
	 * @return how many accesses are parsed, there is one access file for all users
	 * accessuserid|queryid|providerid|timestamp
	 * sample: 0|0|1|19 Oct 2007 11:00
	 */
	public int parseAccessFile (String file) throws java.sql.SQLException
    {
        int count = 0;
		java.util.Date timestamp = new java.util.Date();
		try {
            java.io.BufferedReader reader = new  java.io.BufferedReader (new java.io.InputStreamReader (new java.io.FileInputStream (file)));
            String line = reader.readLine ();
            while (line != null) {
                AccessActivity activity = new AccessActivity ();
                QueryActivity query = new QueryActivity ();
                activity.setQuery (query);
                java.util.StringTokenizer st = new java.util.StringTokenizer (line, "|");
				//file in user information, retrieve that user object, add access history to vAccess
                int userId = 0;
                if (st.hasMoreTokens ()) {
                    userId = Util.toInteger (st.nextToken(), 0);
                }
                //query id
                int qryId = 0;
                if (st.hasMoreTokens ()) {
                    qryId = Util.toInteger (st.nextToken(), 0);
                }
                activity.getQuery().setUid (qryId);
                //webpage id
                int webpageId = 0;
                if (st.hasMoreTokens ()) {
                    webpageId = Util.toInteger (st.nextToken(), 0);
                }
                activity.setWebPageId (webpageId+1);
                //time stamp
				if (st.hasMoreTokens ()){
					timestamp = Util.toDate (st.nextToken ());                    
					activity.setTimestamp (timestamp);
				}
				//save to db
                UpdateUser user = new UpdateUser ();
				user.setUserType(CrawlerBase.ACCESS_USER);
				user.setUserId (++userId);				
				this.saveQueryUser(user);
                this.saveAccess (user, activity);
                
                line = reader.readLine ();
            }
			reader.close();			
		}
        catch (java.io.IOException ioe) {
            ioe.printStackTrace ();
		}
        return count;
    }

    /**
     * Save user information to db by inserting a record to table content provider Users.
     */
    public void saveUpdateUser(UpdateUser user, String path) throws java.sql.SQLException
    {
        //check if this user already exists in db
		String qry = "select UserUID from UpdateUsers where UserUID=" + user.getUserId ();
        boolean bExist = false;
		java.sql.Statement qryStmt = this.connection.createStatement ();
        java.sql.ResultSet rs = qryStmt.executeQuery (qry);
        if (rs.next ())
            bExist = true;
        rs.close ();
        qryStmt.close ();
        if (!bExist) {
		    String sql = "insert into UpdateUsers (UserUID, UserPath) VALUES (?,?)";
            java.sql.PreparedStatement stmt = this.connection.prepareStatement (sql);
            stmt.setInt(1, user.getUserId ());
		    stmt.setString(2, path);
            stmt.executeUpdate ();
            stmt.close ();
        }
    }
	/**
     * Save access user information to db by inserting a record to table AccessUsers.
     */
	public void saveQueryUser(UpdateUser user) throws java.sql.SQLException
	{
        //check if this user already exists in db
		String qry = "select UserUID from QueryUsers where UserUID=" + user.getUserId ();
        boolean bExist = false;
		java.sql.Statement qryStmt = this.connection.createStatement ();
        java.sql.ResultSet rs = qryStmt.executeQuery (qry);
        if (rs.next ())
            bExist = true;
        rs.close ();
        qryStmt.close ();
        if (!bExist) {
		    String sql = "insert into QueryUsers (UserUID) VALUES (?)";
		    java.sql.PreparedStatement stmt = this.connection.prepareStatement(sql);
		    stmt.setInt(1, user.getUserId());
		    stmt.executeUpdate();
		    stmt.close();
        }
	}

    /**
     * Save one update activity information to db.
     */
	public void saveUpdate(UpdateUser user, UpdateActivity act) throws java.sql.SQLException
    {
        String sql = "insert into Updates (UserUID, WebPage, ActualSize, NormalizedSize, UpdateTime) VALUES (?,?,?,?,?)";
        java.sql.PreparedStatement stmt = this.connection.prepareStatement (sql);
		stmt.setInt (1, user.getUserId ());
		stmt.setString(2, act.getPage());
        stmt.setInt (3, act.getOrgSize ());
        stmt.setInt (4, act.getSize ());
		stmt.setTimestamp(5, this.toSqlTimestamp(act.getTimestamp()));        
        stmt.executeUpdate ();
        stmt.close ();
    }
    
    /**
     * Save one query activity information to db and corresponding query information if required.
     */
    public void saveAccess(UpdateUser user, AccessActivity act) throws java.sql.SQLException
    {
        String sql = "insert into Accesses (QueryUID, WebPageID, AccessTime) VALUES (?,?,?)";
        java.sql.PreparedStatement stmt = this.connection.prepareStatement (sql);
        stmt.setInt (1, act.getQuery().getUid ());
        stmt.setInt (2, act.getWebPageId ());
		stmt.setTimestamp(3, this.toSqlTimestamp(act.getTimestamp()));
        stmt.executeUpdate ();
        stmt.close ();
        //check if this query already exists
		String qry = "select TotalAccess from Queries where QueryUID=" + act.getQuery().getUid ();
        int totalAccess = 0;
		java.sql.Statement qryStmt = this.connection.createStatement ();
        java.sql.ResultSet rs = qryStmt.executeQuery (qry);
		if (rs.next ()) {
            totalAccess = rs.getInt (1);
		}
        rs.close ();
        qryStmt.close ();
		++totalAccess;
        if (totalAccess > 1) {
			//increase total access
		    sql = "update Queries set TotalAccess=? where QueryUID=?";
		    stmt = this.connection.prepareStatement(sql);
		    stmt.setInt(1, totalAccess);
		    stmt.setInt(2, act.getQuery().getUid ());
		    stmt.executeUpdate();
		    stmt.close();
		}
		else {
		    sql = "insert into Queries (QueryUID, UserUID, QueryTime, TotalAccess) VALUES (?, ?, ?, ?)";
		    stmt = this.connection.prepareStatement(sql);
		    stmt.setInt(1, act.getQuery().getUid ());
		    stmt.setInt(2, user.getUserId());
            stmt.setTimestamp (3, this.toSqlTimestamp(act.getTimestamp()));
            stmt.setInt (4, totalAccess);
		    stmt.executeUpdate();
		    stmt.close();
        }
    }
    /**
     * Convert java time to sql time.
     */
    private java.sql.Date toSqlTime (java.util.Date date)
    {
        if (date == null)
            return null;
        return new java.sql.Date (date.getTime ());
    }

    public static void main(java.lang.String[] args) {
        try {
            ActivityParser amain = new ActivityParser ();
            amain.process ();
        }
        catch (Throwable e) {
            e.printStackTrace ();
        }
    }
	private void clearTable(String tablename) throws java.sql.SQLException
	{
		String qry = "select * from " + tablename;
		boolean bExist = false;
		java.sql.Statement qryStmt = connection.createStatement ();
        java.sql.ResultSet rs = qryStmt.executeQuery (qry);
        if (rs.next ())
            bExist = true;
        rs.close ();
        qryStmt.close ();
		
		if (bExist){
			String sql = "truncate table " + tablename;
			java.sql.PreparedStatement stmt = connection.prepareStatement (sql);
			stmt.executeUpdate ();
			stmt.close ();
		}
	}
}
