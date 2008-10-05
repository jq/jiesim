/**
 * Project: epic
 * File: Crawler.java
 * Author: Qinglan Li
 * 
 * Date: April 2nd, 2007
 * Version: 1.0
 * Description: It is built for caching scheduling with different user update prediction patterns
 *              This file is the main code to read user request and budget assignment, make decision on scheduling   
 * 
 * Date: October 20, 2007
 * Version: 1.1 read from database instead of trace files 
 * 
 */
package webdb;

import java.util.*;
import java.sql.*;

public class Crawler extends CrawlerBase {
	//private  String tracepath  =  "c:\\temp\\webdb\\trace\\";
	//private  String accessfile  =  "c:\\temp\\webdb\\access\\access-20k.txt";
	//trace and access files path
	private String tracepath = "/afs/cs.pitt.edu/projects/admt/projects/epic/trace/Upd688/";
	private String accessfile = "/afs/cs.pitt.edu/projects/admt/projects/epic/trace/access/access-20k.txt";
	//result file directory
	private  static String filename = "result";
	private  static String ext = ".txt";
	
	private  Vector vUpdateUser = new Vector ();
	private  Vector vQuery = new Vector ();
	private  Vector vSchedule = new Vector ();
	private  Vector vfinalSchedule = new Vector ();
	private  Vector vResult = new Vector ();
	private  int overallFreshness = 0;	
	private  int updateAll = 0;
	private  int accessAll = 0;

	public void process (double type1, double type2, double type3, double type4, boolean secondRoundAdjust, int algorithm) throws java.io.IOException, java.sql.SQLException 
    {
		//load in all log files in folder and load access information 
		//load budget information from base
        this.initialize ();	
		
		//distribute three types of users according to percentage parameter
		this.assignUserType(type1, type2, type3, type4);
		
		//make decision on scheduling
		this.schedule (secondRoundAdjust, algorithm);
	
		//display the result
		this.showResult(type1, type2, type3, type4, secondRoundAdjust);
    }
    
	public int process (double type1, double type2, double type3, double type4, boolean secondRoundAdjust, int algorithm, java.io.FileWriter out, int round) throws java.io.IOException, java.sql.SQLException 
    {
		this.initialize ();			
		this.assignUserType(type1, type2, type3, type4);
		this.schedule (secondRoundAdjust, algorithm);
		int allFreshness = this.showRandomResult(out, secondRoundAdjust, round);		
		return allFreshness;
	}

	private void initialize() throws java.sql.SQLException
    {
        //If all log files in this folder are for the same user
        this.loadDB();
    }
	/**
	 * Load all update and access from database
	 */
	private void loadDB() throws java.sql.SQLException
	{
	
		this.makeConnection(); 
		
		//load update trace
		String qry = "select * from UpdateUsers order by UserUID asc";
		Statement qryStmt = this.connection.createStatement();
		ResultSet rs = qryStmt.executeQuery(qry);		
		while (rs.next ())
		{
            UpdateUser user = new UpdateUser ();
            user.setUserId (rs.getInt (1));
			this.vUpdateUser.add(user);

			//find all updates performed by this user, must be sorted on update time
            String _qry = "select * from Updates where UserUID=" + user.getUserId () + " order by UpdateTime asc";
		    java.sql.Statement _qryStmt = this.connection.createStatement ();
            java.sql.ResultSet _rs = _qryStmt.executeQuery (_qry);
            while (_rs.next ()) {
			    UpdateActivity activity = new UpdateActivity();
                
			    activity.setPage (_rs.getString(3));
                activity.setOrgSize (_rs.getInt(4));
                activity.setSize (_rs.getInt(5));
                activity.setTimestamp (_rs.getTimestamp(6));

                activity.setUser (user);
                user.addAllUpdate (activity);
            }
            _rs.close ();
            _qryStmt.close ();
		}
		rs.close();
		qryStmt.close();
		
		qry = "select count( UpdateUID ) from Updates";
		qryStmt = this.connection.createStatement();        
        rs = qryStmt.executeQuery(qry);
		while (rs.next())
			updateAll = rs.getInt (1);
		qryStmt.close();		
		
		qry = "select count( AccessUID ) from Accesses";
		qryStmt = this.connection.createStatement();        
        rs = qryStmt.executeQuery(qry);
		while (rs.next())
			accessAll = rs.getInt (1);
		qryStmt.close();
		
		//load access trace, must be sorted on query time
		qry = "select * from Queries order by QueryTime asc";
		qryStmt = this.connection.createStatement();
		rs = qryStmt.executeQuery(qry);
		while (rs.next())
		{
			int queryId = rs.getInt(1);
            int _userId = rs.getInt (2);
            java.util.Date ts = rs.getTimestamp (3);

            QueryActivity qryAct = new QueryActivity ();
            qryAct.setUid (queryId);
            qryAct.setTimestamp (ts);
            
            QueryUser _user = new QueryUser ();
            _user.setUserId (_userId);
            qryAct.setUser (_user);
            this.vQuery.add (qryAct);
            
            //load all accesses which belong to this query,  must be sorted on access time
		    String _qry = "select * from Accesses where QueryUID=" + qryAct.getUid () + " order by AccessTime asc";
		    java.sql.Statement _qryStmt = this.connection.createStatement ();
            java.sql.ResultSet _rs = _qryStmt.executeQuery (_qry);

            while (_rs.next ()) {
                AccessActivity accAct = new AccessActivity ();
                accAct.setUid (_rs.getInt (1));
                accAct.setWebPageId (_rs.getInt (3));
				accAct.setTimestamp (_rs.getTimestamp (4));
                
                accAct.setQuery (qryAct);
                qryAct.addAccess (accAct);
            }
            _rs.close ();
            _qryStmt.close ();
            
		}
		rs.close();
		qryStmt.close();
			
		//printUpdateAndQuery();
		
		this.disconnect();
		
	}
	//Testing purpose, print loaded update and query access
	private void printUpdateAndQuery()
	{
		for (int i = 0; i < this.vUpdateUser.size(); i ++)
		{
			UpdateUser user = (UpdateUser)this.vUpdateUser.get(i);
			int userId = user.getUserId ();
			java.util.Vector updates = user.getAllUpdates ();
			for (int j = 0; j < updates.size(); j++)
			{
				UpdateActivity upd = (UpdateActivity)updates.get(j);
				trace(userId + ", " + upd.getOrgSize () + ", " + upd.getSize () + ", " + Util.formatTimestamp(upd.getTimestamp ()));
			}
		}
		
		trace ("Now comes to query information.");
		
		for (int i = 0; i < this.vQuery.size(); i ++)
		{
			QueryActivity qryAct = (QueryActivity)this.vQuery.get(i);
			java.util.Vector pageAccess = qryAct.getAccess ();
			for (int j = 0; j < pageAccess.size(); j ++)
			{
				AccessActivity accAct = (AccessActivity)pageAccess.get(j);
				int qryId = accAct.getQuery ().getUid ();
				trace(accAct.getUid () + ", " + qryId + ", " + accAct.getWebPageId () + ", " + Util.formatTimestamp(accAct.getTimestamp ()));
				
			}
		}
	}
	/*
	* @Distribute FOUR types of users according to percentage parameter
	* there are four types of users: PAGE_NAIVE_USER, PAGE_NORMAL_USER, PAGE_SMART_USER,  PAGE_ADVANCED_USER  
	*/
	private void assignUserType(double type1, double type2, double type3, double type4){
        int total = this.vUpdateUser.size ();
        if (total <= 0)
            return;
        int types[] = new int[total];
        //numbers of users with type 1
        int start = 0;
        int end   = start + (int)(type1 * total);
        if (end > total)
            end = total;
        for (int i = start; i < end; ++i) {
            types[i] = CrawlerBase.PAGE_NAIVE_USER;
        }
        //type 2
        start = end;
        end   = start + (int)(type2 * total);
        if (end > total)
            end = total;
        for (int i = start; i < end; ++i) {
            types[i] = CrawlerBase.PAGE_NORMAL_USER;
        }
		 //type 3
        start = end;
        end   = start + (int)(type3 * total);
        if (end > total)
            end = total;
        for (int i = start; i < end; ++i) {
            types[i] = CrawlerBase.PAGE_SMART_USER;
        }
        //all left users are assigned with type4
        start = end;
        end   = total;
        for (int i = start; i < end; ++i) {
            types[i] = CrawlerBase.PAGE_ADVANCED_USER;
        }
        //do random
        java.util.Random random = new java.util.Random (System.currentTimeMillis ());
        for (int i = 0; i < total; ++i) {
            int first = random.nextInt (total);
            int second = random.nextInt (total);
            int tmp = types[first];
            types[first]=types[second];
            types[second]=tmp;
        }
        //Assign types to users
		for (int i = 0; i < total; ++i)
		{
			UpdateUser user = (UpdateUser)this.vUpdateUser.get(i);
			user.setUserType(types[i]);
			trace("UpdateUser " + user.getUserId() + " type is " + user.getUserType());
		}
		}
	
	/*
	* @schedule the task 
	*/
	private void schedule (boolean secondRoundAdjust, int algorithm) throws java.sql.SQLException{
        java.util.Calendar startTime = java.util.Calendar.getInstance ();        
        startTime.set(START_YEAR, START_MON, START_DAY);
        startTime.set(startTime.HOUR_OF_DAY , 0);
        startTime.set(startTime.MINUTE , 0);
        startTime.set(startTime.SECOND, 0);
        
		//set default last crawl time
        for (int i = 0; i < this.vUpdateUser.size(); ++i) {
            UpdateUser user = (UpdateUser)this.vUpdateUser.get (i);
            user.setLastCrawlTime (startTime.getTime());
        }
		
		java.util.Date firstCrawlTime = startTime.getTime ();
        
        java.util.Calendar endTime = java.util.Calendar.getInstance ();
        endTime.set(END_YEAR, END_MON, END_DAY);
		endTime.set(startTime.HOUR_OF_DAY , 0);
        endTime.set(startTime.MINUTE , 0);
        endTime.set(startTime.SECOND, 0);
		
        int startFrom = 0;
        while (startTime.before (endTime)) {
            //find the end time of current time slot
            java.util.Calendar slotEndTime = java.util.Calendar.getInstance ();
            slotEndTime.setTime (startTime.getTime ());
            slotEndTime.add (slotEndTime.MINUTE, BLOCK_LEN_IM); //advance by crawl period
			
			//start scheduling 
			Scheduler sd = new Scheduler (firstCrawlTime, startTime.getTime(), slotEndTime.getTime());			
			sd.preSchedule ();
			sd.setUpdateCount (updateAll);
			sd.setAccessCount (accessAll);
			java.util.Calendar timestamp = java.util.Calendar.getInstance ();
			//for all user, compute their socres
			for (int n = 0 ; n < vUpdateUser.size (); n ++){
			//trace("slot end time " +  slotEndTime.getTime()  + ", user " + n );
				UpdateUser user = (UpdateUser)vUpdateUser.get(n);
				user.getUpdates().removeAllElements();				
                                
				//find all activities 
				java.util.Vector vUpdates = user.getAllUpdates();					
			
				//now read all update request (activities) in future time period
				int count = 0;
				for (int i = 0; i < vUpdates.size(); ++i) {
                    //UpdateActivity actBefore = (i > 0)?(UpdateActivity)vUpdates.get (i-1):(UpdateActivity)vUpdates.get (i);
				    UpdateActivity act = (UpdateActivity)vUpdates.get (i);	
					//convert the activity timestamp to calendar
					timestamp.setTime (act.getTimestamp ());
					if (timestamp.before (slotEndTime)) {
						if (timestamp.after (startTime)) {  
						    //if ((act.getSize () != actBefore.getSize())) //file size changed
								user.addUpdate(act);
								sd.addUpdate (act);	
						        ++count;						
						}
					}
                    else {
                        //update time is sorted, optimized
                        break;
                    }
				}	
			}
			sd.setAllUpdateUsers (vUpdateUser);
			sd.setAllQueries(vQuery);
            //now read all access information in this crawl period
			for (; startFrom < vQuery.size(); ++startFrom) {
			    QueryActivity act = (QueryActivity)vQuery.get (startFrom);
			    //convert the activity timestamp to calendar
				timestamp.setTime(act.getTimestamp ());
				//find one
				if (timestamp.before (slotEndTime)) {
					if (timestamp.after (startTime))
						sd.addQuery (act);
				}
                else {
                    //optimized, beacuse query access time is sorted
                    break;
                }
			}
			if (algorithm == USER_SCORE){
				//compute their scores and rank, 
				vSchedule.add(sd.rank());
				vfinalSchedule.add(sd.scheduleWithinBudget(secondRoundAdjust));				
			}
			//another set of schedule algorithms 
			//uniform random
			else if (algorithm == UNIFORM_RANDOM){
				vfinalSchedule.add(sd.scheduleWithUniformRandom(this.vUpdateUser.size()));
			}
			//frequency-based random
			else if (algorithm == FREQUENCY_BASED_RANDOM){
				vfinalSchedule.add(sd.scheduleWithFrequencyBasedRandom(this.vUpdateUser.size()));
			}
			//count-based random
			else if (algorithm == COUNT_BASED_RANDOM){
				vfinalSchedule.add(sd.scheduleWithCountBasedRandom(this.vUpdateUser.size()));
			}
			//count-based determenistic
			else if (algorithm == COUNT_BASED_DETERMENISTIC){
				vfinalSchedule.add(sd.scheduleWithCountBasedDetermenistic(this.vUpdateUser.size()));
			}
			//popularity
			else if (algorithm == POPULARITY_RANDOM){
				vfinalSchedule.add(sd.scheduleWithPopularity(this.vUpdateUser.size()));
			}
			else errorProcess();
			
			vResult.add(new Integer(sd.computeFreshness()));
			//advance to next time slot
            startTime = slotEndTime;
			
			sd.postSchedule ();
        }	
	}
	
	private void errorProcess(){
		trace("ERROR: It is out of range.");
		System.exit(-1);
	}
	private void showResult(double type1, double type2, double type3, double type4, boolean secondRoundAdjust) throws java.io.IOException
	{	
		String line ="";
		for (int i = 0; i < this.vSchedule.size(); ++i) {
            Object obj = this.vSchedule.get (i);
            if (obj instanceof UserScore) {
                UserScore userScore = (UserScore)obj;
                trace (userScore.getUser().getUserId () + "=" + userScore.getScore ());
				line += userScore.getUser().getUserId () + "=" + userScore.getScore () + "\r\n";
            }
            else if (obj instanceof java.util.Vector) {
                Vector vUserScore = (java.util.Vector)obj;
                for (int j = 0; j < vUserScore.size(); ++j) {
                    UserScore userScore = (UserScore)vUserScore.get (j);
                    trace (userScore.getUser().getUserId () + "=" + userScore.getScore ());
					line += userScore.getUser().getUserId () + "=" + userScore.getScore () + "\r\n";
                }
            }
        }
		
		line += "Now comes to final schedule\r\n";
		
		for (int i = 0; i < this.vfinalSchedule.size(); ++i) {
            Object obj = this.vfinalSchedule.get (i);
            if (obj instanceof UserScore) {
                UserScore userScore = (UserScore)obj;
                trace (userScore.getUser().getUserId () + "=" + userScore.getScore ());
				line += userScore.getUser().getUserId () + "=" + userScore.getScore () + "\r\n";
            }
            else if (obj instanceof java.util.Vector) {
                Vector vUserScore = (java.util.Vector)obj;
                for (int j = 0; j < vUserScore.size(); ++j) {
                    UserScore userScore = (UserScore)vUserScore.get (j);
                    trace (userScore.getUser().getUserId () + "=" + userScore.getScore ());
					line += userScore.getUser().getUserId () + "=" + userScore.getScore () + "\r\n";
                }
            }
        }
		
		int totalfreshness = 0;
		for (int i = 0; i < vResult.size(); i ++){			
			int freshness = ((Integer)this.vResult.get(i)).intValue ();
			line += "round " + i + " freshness = " + freshness + "\r\n";
			totalfreshness += freshness;
		}
		line += totalfreshness;
		java.io.FileWriter out = new java.io.FileWriter(filename + "-" + type1 + "-" + type2 + "-" + type3 + "-" + type4 + "-" + secondRoundAdjust + ext);		
		out.write(line);
		out.close();
		trace (" files has been created. refreshness=" + totalfreshness); 			
	}
	
	private int showRandomResult (java.io.FileWriter out, boolean multipleRuns, int round) throws java.io.IOException {	
		String line = "";
		int totalfreshness = 0;
		
		for (int i = 0; i < vResult.size(); i ++){			
			int freshness = ((Integer)this.vResult.get(i)).intValue ();
			if (!multipleRuns) 
				line += "round " + i + " freshness = " + freshness + "\r\n";
			totalfreshness += freshness;
		}
		if (multipleRuns)
			line += (round+1) + ", " + totalfreshness + "\r\n"; 
		
		out.write(line);
		trace("The " + round + " run total refreshness = " + totalfreshness); 			
		overallFreshness += totalfreshness;
		return overallFreshness;
	}
	
	public static void main(java.lang.String[] args) {
        if (args.length < 1) {
            System.out.println ("Usage: java Crawler userype1 usertype2 usertype3 usertype4 algorithm adjust");
			System.out.println ("Algorithm: Uniform random ==> 1;");
			System.out.println ("Algorithm: frequency_based random ==> 2;");
			System.out.println ("Algorithm: count_based random ==> 3;");
			System.out.println ("Algorithm: count_based determenistic ==> 4;");
			System.out.println ("Algorithm: popularity random ==> 5;");
			System.out.println ("Algorithm: user score ==> 6;");
            System.exit(-1);
        }
		 
        try {
			double type1 = 0.3;
			double type2 = 0.3;
			double type3 = 0.4;
			double type4 = 0.0;
			boolean secondRoundAdjust = true;
			int algorithm = 0;
			if (args.length >= 4){
				Double t1 = new Double (args[0]);
				type1 = t1.doubleValue ();
				Double t2 = new Double (args[1]);
				type2 = t2.doubleValue ();
				Double t3 = new Double (args[2]);
				type3 = t3.doubleValue ();
				Double t4 = new Double (args[3]);
				type4 = t4.doubleValue ();
				Integer t5 = new Integer (args[4]);
				algorithm = t5.intValue ();
				secondRoundAdjust = "true".equals (args[5]);				
			}			
			if (algorithm != 6){
				int freshness = 0;
				if (secondRoundAdjust == true)
				{
					java.io.FileWriter out = new java.io.FileWriter(filename + "-" + REPEAT_TIMES + "-" + algorithm + ".txt");		
					for (int i = 0; i < REPEAT_TIMES; i++)
					{
						Crawler amain = new Crawler();
						freshness += amain.process(type1, type2, type3, type4, secondRoundAdjust, algorithm, out, i);
					}
					freshness = freshness / REPEAT_TIMES;
					out.write("Overall freshness of " + REPEAT_TIMES + " runs is " + freshness + "\r\n");
					out.close();
					System.out.println("The overall freshness of " + REPEAT_TIMES + " runs is " + freshness);
				}
				else
				{
					java.io.FileWriter out = new java.io.FileWriter(filename + "-" + algorithm + ext);		
					Crawler amain = new Crawler();
					freshness = amain.process(type1, type2, type3, type4, secondRoundAdjust, algorithm, out, 1);
					System.out.println("The overall freshness of " + freshness);
					out.write("Overall freshness is " + freshness + "\r\n");
					out.close();					
				}
			}
			else {
				Crawler amain = new Crawler ();
				amain.process (type1, type2, type3, type4, secondRoundAdjust, algorithm);
			}
		}
        catch (Throwable e) {
            e.printStackTrace ();
        }
    }
}
