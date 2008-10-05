/**
 * Project: epic
 * File: Scheduler.java
 * Author: Qinglan Li
 * 
 * Date: April 2nd, 2007
 * Version: 1.0
 * Description: This file is to schedule caching updates according to user scores
 *              We have three types of user score algorithm
 * 
 * Date: August 22, 2007
 * Version: 1.1
 * Comment: Add update information, new score functions, new user type
 * 
 * Date: October 20, 2007
 * Modify scheduling algorithms, db connection
 */

package webdb;

import java.sql.*;

public class Scheduler extends CrawlerBase implements java.io.Serializable {
    private java.util.Date firstCrawlTime, crawlStartTime, crawlEndTime;
    private java.util.Vector vUpdateUsers = null;
    //private java.util.Vector vQueryUsers = new java.util.Vector ();

	private java.util.Vector update = new java.util.Vector();
	private java.util.Vector query = new java.util.Vector();
	private java.util.Vector vAllQueries = null;
	private  int TOTAL_UPDATE_COUNT = 0;
	private  int MAX_ACCESS_COUNT = 0;
	//private java.util.Vector crawl = new java.util.Vector();


	//keep a priority queue to record which has the highest score to be scheduled in this time period
    //the element is UserScore;
    private java.util.Vector scheduleList = new java.util.Vector ();	
	private UserScore[] finalscheduleList = new UserScore[Crawler.TIME_UNIT / Crawler.BUDGET_UNIT ];

    public Scheduler (java.util.Date first, java.util.Date start, java.util.Date end)
    {
		this.firstCrawlTime = first;
        this.crawlStartTime = start;
        this.crawlEndTime = end;		
    }
	public void preSchedule () throws java.sql.SQLException 
	{
		super.makeConnection ();
	}
	public void postSchedule () throws java.sql.SQLException 
	{
		super.disconnect ();
	}
	public void setAllQueries (java.util.Vector _vQuery){
		this.vAllQueries = _vQuery;
	}
	public void setAllUpdateUsers (java.util.Vector v){
		this.vUpdateUsers = v;
	}
	public void addUpdate (UpdateActivity act)
	{
		this.update.add(act);
	}
	public void addQuery (QueryActivity act)
	{
		this.query.add(act);
	}
	public void setUpdateCount (int count)
	{
		this.TOTAL_UPDATE_COUNT = count;
	}
	public void setAccessCount (int count)
	{
		this.MAX_ACCESS_COUNT = count;
	}
	public java.util.Vector rank()
	{
        this.scheduleList.removeAllElements ();
		double FreqRatio = (double) Crawler.TIME_UNIT / MIN_HOUR / this.query.size(); 
		for (int i = 0 ; i < this.vUpdateUsers.size (); i ++)
		{
			UpdateUser user = (UpdateUser)this.vUpdateUsers.get(i);
            if (user.getUserType() == CrawlerBase.PAGE_SMART_USER) {
				trace("user 3");
				this.computeSmartScore(user, FreqRatio);
            }
			else if (user.getUserType() == CrawlerBase.PAGE_NORMAL_USER)
			{
				this.computeNormalScore(user);
				trace("user 2");
			}
			else if (user.getUserType() == CrawlerBase.PAGE_ADVANCED_USER)
			{
				this.computeAdvancedSmartScore(user);
				trace("user 4");
			}
			else
			{
				this.computeNaiveScore(user);
				trace("user 1");
			}
		}
        //sort the scores with big score listed first
        boolean bContinue = true;
        while (bContinue) {
            bContinue = false;
            for (int i = 0; i < this.scheduleList.size() - 1; ++i) {
                UserScore score1 = (UserScore)this.scheduleList.get (i);
                UserScore score2 = (UserScore)this.scheduleList.get (i+1);
                if (score1.getScore() < score2.getScore()) {
                    bContinue = true;
                    //swap
                    this.scheduleList.removeElementAt (i);
                    this.scheduleList.insertElementAt (score1, i + 1);
                }
            }
        }
        return this.scheduleList;
	}
	//naive, normal, super, and advanced user schedule
	public UserScore[] scheduleWithinBudget(boolean secondRoundAdjust) throws java.sql.SQLException
	{	
		int timeslot = Crawler.TIME_UNIT / Crawler.BUDGET_UNIT ;
		int position = 0;
		//one crawl period
		for (int i = 0; i < timeslot; i ++)
		{
			//get first element from scheduleList
			int size = scheduleList.size();
			if (size != 0){
				UserScore first = (UserScore)this.scheduleList.get(i % size);
				//if the user type is PAGE_SMART_USER, put it the nearest time slot after its update request
				//else put it sequencially from the beginning within this crawl period
				//if the user type is PAGE_ADVANCED_USER, put it after its latest update request and before its access request (happen in second round pick)
				//else put it sequencially from the beginning within this crawl period
				if ((first.getUser().getUserType() == CrawlerBase.PAGE_SMART_USER) || (first.getUser().getUserType() == CrawlerBase.PAGE_ADVANCED_USER))
				{
					java.util.Calendar endTime = java.util.Calendar.getInstance ();
					endTime.setTime (first.getUpdateTime ());
					boolean stop = false;
					java.util.Calendar endCrawlTime = java.util.Calendar.getInstance ();
					endCrawlTime.setTime (this.crawlStartTime);
					
					while (endCrawlTime.before (endTime))
						endCrawlTime.add (endCrawlTime.MINUTE, Crawler.BUDGET_UNIT); //advance by one time unit
					position = (int)((endCrawlTime.getTimeInMillis() - this.crawlStartTime.getTime()) / CrawlerBase.MS_MINUTE / CrawlerBase.BUDGET_UNIT);
				 }
				int finalPosition = 0;
				for (int j = 0; j < timeslot; j++)
				{
					finalPosition = position % timeslot;
					if ((UserScore)this.finalscheduleList[finalPosition] == null)
						break;
					position++;
				}
				if (finalPosition < timeslot)
				{
					finalscheduleList[finalPosition] = first;
				}			
			}
		}
		if (secondRoundAdjust){
	    //second round scheduling, start from the second time slot, swap the one which is not fresh   
		for (int i = 1; i < timeslot; i++){
			UserScore currentTask = (UserScore)finalscheduleList[i];
			if (currentTask != null){
				UpdateUser user = currentTask.getUser();
				if (user.getUserType() == Crawler.PAGE_ADVANCED_USER)
				{
					//if it is advanced user, check if it is fresh, if not, swap with one earlier time slot

					//current crawl timestamp
					java.util.Calendar timestamp = java.util.Calendar.getInstance();
					timestamp.setTime(this.crawlStartTime);
					timestamp.add(timestamp.MINUTE, Crawler.BUDGET_UNIT * i);

					//last update timestamp
					java.util.Calendar updatestamp = java.util.Calendar.getInstance();
					updatestamp.setTime(getLastUpdateTime(user, timestamp.getTime()));				
					
					//last access timestamp
					java.util.Calendar acessstamp = java.util.Calendar.getInstance();
					acessstamp.setTime(getLastAccessTime(user, timestamp.getTime()));

					if (updatestamp.before(acessstamp))
					{
						timestamp.add(timestamp.MINUTE, -Crawler.BUDGET_UNIT);
						if (updatestamp.before(timestamp) && (acessstamp.after(timestamp)))
						{
							trace("++++++++++++");
							UserScore temp = finalscheduleList[i-1];
							finalscheduleList[i-1] = currentTask;
				            finalscheduleList[i] = temp;
							trace("user previous: " + finalscheduleList[i-1].getUser().getUserId ());
							trace("user next: " + finalscheduleList[i].getUser().getUserId ());
						}
					}					
				}
			}
		}		
		}
		for (int i = 0; i < timeslot; i++){
			if (finalscheduleList[i] != null){
		        java.util.Calendar positionTime = java.util.Calendar.getInstance ();
				positionTime.setTime (this.crawlStartTime);
				positionTime.add(positionTime.MINUTE, Crawler.BUDGET_UNIT * i );		
				finalscheduleList[i].getUser().addCrawl(positionTime.getTime ()); 
			}
		}

		printFinalScheduleList(finalscheduleList);
		return this.finalscheduleList;
	}
	//random uniform schedule
	public java.util.Vector scheduleWithUniformRandom(int userNumbers)
	{
		int timeslot = Crawler.TIME_UNIT / Crawler.BUDGET_UNIT ;		
		java.util.Random random = new java.util.Random (System.currentTimeMillis ());
		for (int i = 0; i < timeslot; i++){
			int userId = random.nextInt(userNumbers);			
			UpdateUser user = (UpdateUser)this.vUpdateUsers.get(userId);
			addUserScore (user, 0);
			java.util.Calendar positionTime = java.util.Calendar.getInstance ();
			positionTime.setTime (this.crawlStartTime);
			positionTime.add(positionTime.MINUTE, Crawler.BUDGET_UNIT * i );					
			user.addCrawl(positionTime.getTime ()); 
		}
		//printList(scheduleList);
		return this.scheduleList;
	}
	
	//update frequency-based random
	public java.util.Vector scheduleWithFrequencyBasedRandom(int userNumbers)
	{
		int timeslot = Crawler.TIME_UNIT / Crawler.BUDGET_UNIT ;		
		int[] freq = new int[userNumbers];		
		int size = this.update.size ();
		int[] seed = new int[size];

		for (int i = 0; i < size; i ++){
			UpdateActivity act = (UpdateActivity)this.update.get(i);
			int userId = act.getUser().getUserId();
			freq[--userId]++;
		}

		int counter = 0;
		for (int i = 0; i < userNumbers; i ++)
		{
			for (int j = 0; j < freq[i]; j++)
			{
				seed[counter++] = i;  					
			}
		}
		
		java.util.Random random = new java.util.Random(System.currentTimeMillis());
		int Userid =0;
		for (int i = 0; i < timeslot; i++){
			if (counter == 0)
				Userid = random.nextInt(userNumbers);
			else Userid = seed[random.nextInt(counter)];
			UpdateUser user = (UpdateUser)this.vUpdateUsers.get(Userid);
			addUserScore (user, 0);
			java.util.Calendar positionTime = java.util.Calendar.getInstance ();
			positionTime.setTime (this.crawlStartTime);
			positionTime.add(positionTime.MINUTE, Crawler.BUDGET_UNIT * i );					
			user.addCrawl(positionTime.getTime ()); 
		}
		//printList(scheduleList);
		return this.scheduleList;
	}
	
	//count-based random, unapplied updates since last crawl on the data item
	public java.util.Vector scheduleWithCountBasedRandom(int userNumbers)
	{
		int timeslot = Crawler.TIME_UNIT / Crawler.BUDGET_UNIT;
		int[] updateCounter = new int[userNumbers];
		int[] seed = new int[TOTAL_UPDATE_COUNT];

		for (int i = 0; i < userNumbers; i++)
		{
			UpdateUser user = (UpdateUser)this.vUpdateUsers.get(i);
			//last crawl time
			java.util.Date timestamp = getLastCrawlTime(user, this.crawlStartTime);			
			updateCounter[i] = getUpdateNumber(timestamp, this.crawlStartTime, user);
		}

		int counter = 0;
		for (int i = 0; i < userNumbers; i++)
		{
			for (int j = 0; j < updateCounter[i]; j++)
			{
				seed[counter++] = i; 
			}
		}
		java.util.Random random = new java.util.Random(System.currentTimeMillis());
		for (int i = 0; i < timeslot; i++)
		{
			int userId = 0;
			if (counter == 0)
				userId = random.nextInt(userNumbers);
			else 
				userId = seed[random.nextInt(counter)];
			UpdateUser user = (UpdateUser)this.vUpdateUsers.get(userId);
			addUserScore(user, 0);
			java.util.Calendar positionTime = java.util.Calendar.getInstance();
			positionTime.setTime(this.crawlStartTime);
			positionTime.add(positionTime.MINUTE, Crawler.BUDGET_UNIT * i);
			user.addCrawl(positionTime.getTime());
		}
		//printList(scheduleList);
		return this.scheduleList;
	}
	
	//count-based determenistic
	public java.util.Vector scheduleWithCountBasedDetermenistic(int userNumbers)
	{
		int timeslot = Crawler.TIME_UNIT / Crawler.BUDGET_UNIT;
		java.util.Vector userscore = new java.util.Vector();
		for (int i = 0; i < userNumbers; i++)
		{
			UpdateUser user = (UpdateUser)this.vUpdateUsers.get(i);
			UserScore score = new UserScore();
			score.setUser(user);
			//last crawl time
			java.util.Date timestamp = getLastCrawlTime(user, this.crawlStartTime);
			score.setScore((int)(getAccessNumber(timestamp, this.crawlStartTime, user)));
			userscore.add(score);
		}
		//sort, rank the user according to update count
		boolean bContinue = true;
		while (bContinue)
		{
			bContinue = false;
			for (int i = 0; i < userscore.size() - 1; ++i)
			{
				UserScore score1 = (UserScore)userscore.get(i);
				UserScore score2 = (UserScore)userscore.get(i + 1);
				if (score1.getScore() < score2.getScore())
				{
					bContinue = true;
					//swap
					userscore.removeElementAt(i);
					userscore.insertElementAt(score1, i + 1);
				}
			}
		}

	
		trace("Now comes the update count");
		for (int i = 0; i < userNumbers; i++)
			trace("" + ((UserScore)userscore.get(i)).getScore());
		trace("Now comes the rank");
		for (int i = 0; i < userNumbers; i++)
		{
			UserScore temp = (UserScore)userscore.get(i);
			UpdateUser user = temp.getUser();
			trace("" + user.getUserId());
		}
	
		
		for (int i = 0; i < timeslot; i++)
		{
			int userId = 0;
			UserScore temp = new UserScore();
			//assign user to time slots, if timeslot is bigger than user numbers, repeat the assign according to rank
			temp = (UserScore)userscore.get(i % (userscore.size()));				
			UpdateUser user = temp.getUser();
			addUserScore(user, 0);
			java.util.Calendar positionTime = java.util.Calendar.getInstance();
			positionTime.setTime(this.crawlStartTime);
			positionTime.add(positionTime.MINUTE, Crawler.BUDGET_UNIT * i);
			user.addCrawl(positionTime.getTime());
		}
		//printList(scheduleList);
		return this.scheduleList;
	}	
	//popularity, change from uniform random to popularity which is the update number of last crawl cycle
	public java.util.Vector scheduleWithPopularity(int userNumbers)
	{
		int timeslot = Crawler.TIME_UNIT / Crawler.BUDGET_UNIT;
		int[] updateCounter = new int[userNumbers];
		int[] seed = new int[MAX_ACCESS_COUNT];

		for (int i = 0; i < userNumbers; i++)
		{
			UpdateUser user = (UpdateUser)this.vUpdateUsers.get(i);
			//update number of last crawl cycle
			java.util.Calendar calendar = java.util.Calendar.getInstance();
			calendar.setTime(this.crawlStartTime);
			calendar.add(calendar.HOUR_OF_DAY, -CrawlerBase.TIME_UNIT /Crawler.MIN_HOUR);
			java.util.Date oneCycleAgo = calendar.getTime();
			updateCounter[i] = getUpdateNumber(oneCycleAgo, this.crawlStartTime, user);
		}

		int counter = 0;
		for (int i = 0; i < userNumbers; i++)
		{
			for (int j = 0; j < updateCounter[i]; j++)
			{
				seed[counter++] = i;
			}
		}
		trace("Now comes the access count");
		for (int i = 0; i < userNumbers; i++)
			trace(""+updateCounter[i]);
		
		java.util.Random random = new java.util.Random(System.currentTimeMillis());
		for (int i = 0; i < timeslot; i++)
		{
			int userId = 0;
			if (counter == 0)
				userId = random.nextInt(userNumbers);
			else
				userId = seed[random.nextInt(counter)];
			UpdateUser user = (UpdateUser)this.vUpdateUsers.get(userId);
			addUserScore(user, 0);
			java.util.Calendar positionTime = java.util.Calendar.getInstance();
			positionTime.setTime(this.crawlStartTime);
			positionTime.add(positionTime.MINUTE, Crawler.BUDGET_UNIT * i);
			user.addCrawl(positionTime.getTime());
		}
		//printList(scheduleList);
		return this.scheduleList;
	}
	private void printList(java.util.Vector list)
	{
		for (int i = 0; i < list.size(); i++)
			{
				UserScore item = (UserScore)list.get(i);
				UpdateUser id = item.getUser();
				int userid = id.getUserId();
				trace("schedule number " + i + ": " + userid);
			}
	}
	private void printFinalScheduleList(UserScore[] list)
	{
		if (list[0] != null){
			for (int i = 0; i < list.length; i++)
			{
				UserScore item = (UserScore)list[i];
				UpdateUser id = item.getUser();
				int userid = id.getUserId();
				System.out.println("schedule number " + i + ": " + userid);
			}
		}
	}
	private void computeSmartScore(UpdateUser user, double ratio)
	{
		java.util.Vector update = user.getUpdates();
		java.util.Vector updateRequest = user.getAllUpdates ();
		java.util.Date  lastCrawTime = user.getLastCrawlTime ();
		
		//optimized
		java.util.Date timeLastPeriod = null;
		double scoreLastPeriod = 0;

		for (int i = 0; i < update.size(); i ++)
		{
            double score = 0.0, pastScore = 0,futureScore = 0;
            double pastAccess = 0, futureAccess = 0;
            double firstAccess = 0, secondAccess = 0;
                    
			java.util.Date startTime = null;
			if (timeLastPeriod == null) {
				pastScore = 0;
				startTime = lastCrawTime;
			}
			else {
				startTime = timeLastPeriod;
				pastScore = scoreLastPeriod;
			}
			if (i % ((int)(1.0 / Crawler.prob)) == 0) {
			UpdateActivity one = (UpdateActivity)update.get (i);
			//compute past score
			/*
			given the time between last crawl on this user to the nearest crawl time slot (after) update request
					
			find all update request between this time period
			starttime = last crawl time;
			endtime = the crawl time slot intemediate after request timestamp;
			while( not after endtime){
				pastAccess += during the time period of startime and timeofupdates, the number of access for this user
				pastScore += (timeofupdates-starttime)*pastAccess
				starttime= timeofupdates;
				}
			*/
				
			java.util.Calendar endTime = java.util.Calendar.getInstance ();
			endTime.setTime (one.getTimestamp());
			
			java.util.Calendar endCrawlTimeCal = java.util.Calendar.getInstance ();
			endCrawlTimeCal.setTime (this.crawlStartTime);
				
			while (endCrawlTimeCal.before (endTime)){
				endCrawlTimeCal.add (endCrawlTimeCal.MINUTE, Crawler.BUDGET_UNIT); //advance by 1 time block				
			}
			java.util.Date endCrawlTime = endCrawlTimeCal.getTime ();
			
			java.util.Date currenttimestamp = startTime;
			for (int j = 0; j < updateRequest.size (); j++){
			        java.util.Date timestamp = ((UpdateActivity)updateRequest.get(j)).getTimestamp();
					if (timestamp.before (endCrawlTime)) {
						if (timestamp.after (startTime)) {
							pastAccess = this.getAccessNumber(currenttimestamp, timestamp, user);
							pastScore += 1.0 * (timestamp.getTime() - lastCrawTime.getTime()) / Crawler.MS_HOUR * pastAccess;
						    trace (pastAccess + " ***" + timestamp + "==="+currenttimestamp);
							currenttimestamp = timestamp;
						}
					}
					else {
						break;
					}
			}
			trace("pastScore = " + pastScore);
			
			//compute future score
			//firstAccess = the number of access during [after the nearest crawl time slot (after) update request, + one crawl unit] 
			//in last crawl period (last day)
			java.util.Calendar calendar = java.util.Calendar.getInstance ();
			calendar.setTime (endCrawlTime);
			calendar.add (calendar.HOUR_OF_DAY, -1*TIME_UNIT/MIN_HOUR);
			java.util.Date oneDayAgo = calendar.getTime ();
			calendar.add(calendar.MINUTE, Crawler.BUDGET_UNIT);
			java.util.Date oneDayAgoUnit = calendar.getTime ();
			firstAccess = this.getAccessNumber(oneDayAgo, oneDayAgoUnit, user);
			trace("firstaccess = " + firstAccess);

			//secondAccess = the number of access during [after the nearest crawl time slot (after) update request, + one crawl unit] 
			//in last 4 crawl period (last 4 day)
			calendar.setTime (endCrawlTime);
			for (int j = 0; j < 4; j++) {
				calendar.add (calendar.HOUR_OF_DAY, (-j-2)*TIME_UNIT/MIN_HOUR);
				java.util.Date DaysAgo = calendar.getTime ();
				calendar.add(calendar.MINUTE, Crawler.BUDGET_UNIT);
				java.util.Date DaysAgoUnit = calendar.getTime ();
				secondAccess += this.getAccessNumber(DaysAgo, DaysAgoUnit, user);
				trace("secondaccess = " + secondAccess);
			}
			//futureAccess = firstAccess + secondAccess/4;
			futureScore = normalize(firstAccess, secondAccess/4, 1, CrawlerBase.alpha );
			//futureScore = normalize(firstAccess, secondAccess/4, futureAccess, CrawlerBase.alpha );
			//futureScore = normalize(firstAccess, secondAccess/4, 1, CrawlerBase.alpha ) * ratio; //don't remember why times ratio??
			//score = normalize(pastScore, futureScore, futureAccess, CrawlerBase.beta ) ;
			score = normalize(pastScore, futureScore, 1, CrawlerBase.beta ) ;
			//trace (pastScore + " , "+ futureScore+"==>score = " + score);
			this.addUserScore (user, score, one.getTimestamp());
			trace("score = " + score);
			
			//optimized, shift to next period
			timeLastPeriod = endCrawlTime;
			scoreLastPeriod = pastScore;
			
			}	
		}
	}
	private void computeNormalScore(UpdateUser user)
	{
        double score = 0.0, pastScore = 0.0;
        double totalAccess = 0, pastAccess = 0;
		
		/*compute past score
		given the time between last crawl on this user to the crawl time slot of the beginning of this crawl period		
		find all update request between this time period 
		*/
		
		java.util.Date startTime = user.getLastCrawlTime ();		
		java.util.Date endTime = this.crawlStartTime;
		java.util.Vector updateRequest = user.getAllUpdates ();
		
        java.util.Date currenttimestamp = startTime;
		for (int i = 0; i < updateRequest.size (); i ++){
			java.util.Date timestamp = ((UpdateActivity)updateRequest.get(i)).getTimestamp();
              
			/*
			 * old algorithm
			 * 
			if (timestamp.after (startTime) && timestamp.before (endTime)) { 
                pastAccess += this.getAccessNumber (startTime, timestamp, user);
			    pastScore += 1.0 * (timestamp.getTime() - currenttimestamp.getTime()) / Crawler.MS_HOUR * pastAccess;
			    currenttimestamp = timestamp;
			}
			 * */
			if (timestamp.before(endTime) && timestamp.after(startTime))
			{
				pastAccess = this.getAccessNumber(currenttimestamp, timestamp, user);
				pastScore += 1.0 * (timestamp.getTime() - startTime.getTime()) / Crawler.MS_HOUR * pastAccess;
				currenttimestamp = timestamp;
			} else if (timestamp.after(endTime)) break;			
		}
		
		//normalize
		score = pastScore;
		trace ("score =" + score);
        this.addUserScore (user, score);
	}
	
	private void computeNaiveScore(UpdateUser user)
	{
            double score = 0.0;
            //endtime = the crawl time slot of the beginning of this crawl period
            java.util.Date lastCrawTime = user.getLastCrawlTime ();
            //score = 0.5 * (endtime - last-crawl-on-this-user)
            //time in hour 
			score = 0.5 * (this.crawlStartTime.getTime() - lastCrawTime.getTime()) / MS_HOUR * this.getAccessNumber(lastCrawTime, this.crawlStartTime, user);
            this.addUserScore (user, score);
	}
	//History of access, history of update, future update, future access
	private void computeAdvancedSmartScore(UpdateUser user)
	{
		java.util.Vector update = user.getUpdates();
		java.util.Vector updateRequest = user.getAllUpdates ();
		java.util.Date  lastCrawTime = user.getLastCrawlTime ();
		
		//optimized
		java.util.Date timeLastPeriod = null;
		double scoreLastPeriod = 0;
		
		for (int i = 0; i < update.size(); i++)
		{
			double score = 0.0, pastScore = 0, futureScore = 0;
			double pastAccess = 0, futureAccess = 0;
			double firstAccess = 0, secondAccess = 0;

			java.util.Date startTime = null;
			if (timeLastPeriod == null) {
				pastScore = 0;
				startTime = lastCrawTime;
			}
			else {
				startTime = timeLastPeriod;
				pastScore = scoreLastPeriod;
			}
			
			if (i % ((int)(1.0 / Crawler.prob)) == 0)
			{
				UpdateActivity one = (UpdateActivity)update.get(i);
				java.util.Calendar endTime = java.util.Calendar.getInstance ();
				endTime.setTime (one.getTimestamp());
			
				java.util.Calendar endCrawlTimeCal = java.util.Calendar.getInstance ();
				endCrawlTimeCal.setTime (this.crawlStartTime);
				
				while (endCrawlTimeCal.before (endTime)){
					endCrawlTimeCal.add (endCrawlTimeCal.MINUTE, Crawler.BUDGET_UNIT); //advance by 1 time block				
				}
				java.util.Date endCrawlTime = endCrawlTimeCal.getTime ();
			
				java.util.Date currenttimestamp = startTime;
				for (int j = 0; j < updateRequest.size (); j++){
				        java.util.Date timestamp = ((UpdateActivity)updateRequest.get(j)).getTimestamp();
						if (timestamp.before (endCrawlTime)) {
							if (timestamp.after (startTime)) {
								pastAccess = this.getAccessNumber(currenttimestamp, timestamp, user);
								pastScore += 1.0 * (timestamp.getTime() - lastCrawTime.getTime()) / Crawler.MS_HOUR * pastAccess;
							    trace (pastAccess + " ***" + timestamp + "==="+currenttimestamp);
								currenttimestamp = timestamp;
							}
						}
						else {
							break;
						}
				}
				//compute future score
				//future access = the number of access of next crawl cycle

				java.util.Calendar calendar = java.util.Calendar.getInstance();
				calendar.setTime (endCrawlTime);
				calendar.add(calendar.HOUR_OF_DAY, Crawler.TIME_UNIT/Crawler.MIN_HOUR);
				java.util.Date oneCralwUnit = calendar.getTime();
				futureAccess = this.getAccessNumber(endCrawlTime, oneCralwUnit, user);
				futureScore = 0.5 * (oneCralwUnit.getTime() - endCrawlTime.getTime()) / Crawler.MS_HOUR * futureAccess;
				
				score = normalize(pastScore, futureScore, 1, CrawlerBase.delta);
				//trace(pastScore + " , " + futureScore + "==>score = " + score);
				this.addUserScore(user, score, one.getTimestamp());
				//optimized, shift to next period
				timeLastPeriod = endCrawlTime;
				scoreLastPeriod = pastScore;
			}
		}
	}
	private void addUserScore (UpdateUser user, double score)
    {
        UserScore userScore = new UserScore ();
        userScore.setUser (user);
        userScore.setScore (score);
        userScore.setCrawlStartTime (this.crawlStartTime);

        this.scheduleList.add (userScore);
    }
	private void addUserScore (UpdateUser user, double score, java.util.Date update)
    {
        UserScore userScore = new UserScore ();
        userScore.setUser (user);
        userScore.setScore (score);
        userScore.setCrawlStartTime (this.crawlStartTime);
		userScore.setUpdateTime(update);

        this.scheduleList.add (userScore);
    }
	//find each page access in this period, if its crawl time is not later than its update time, then it is fresh
	public int computeFreshness()		
	{
		int freshness = 0;
		/*
		boolean fresh;			 
		
		trace("query size = " + this.query.size());
		if (this.query.size() > 0){
			for (int i = 0; i < this.query.size(); i ++)
			{
				QueryActivity qryAct = (QueryActivity)this.query.get(i);
				java.util.Vector pageAccess = qryAct.getAccess ();
				int count = 0;
				//fresh = false;
				for (int j = 0; j < pageAccess.size(); j ++)
				{
					AccessActivity accAct = (AccessActivity)pageAccess.get(j);
                    int userId = accAct.getWebPageId ();
                    UpdateUser user = (UpdateUser)this.vUpdateUsers.get (userId - 1);
					//last crawl timestamp
					java.util.Calendar crawlstamp = java.util.Calendar.getInstance ();
					crawlstamp.setTime (getLastCrawlTime (user, accAct.getTimestamp ()));
					trace(accAct.getTimestamp() + ", " + user.getUserId());
					trace(""+crawlstamp.getTime());
					//last update timestamp
					java.util.Calendar updatestamp = java.util.Calendar.getInstance ();
					updatestamp.setTime (getLastUpdateTime (user, accAct.getTimestamp ()));
					trace(""+updatestamp.getTime ());
										
					if (updatestamp.before(crawlstamp)){
						count ++;
						//fresh = true;
						//break;
					}
					else {
						//fresh = false;
						//break;
					}
				}
				//if (fresh)
				//	freshness ++;
				freshness += 100 * count / pageAccess.size(); 
			}
		}*/
		
		/*		
		trace("query size = " + this.query.size());
		double percentage = 0;
		
		if (this.query.size() > 0){
			for (int i = 0; i < this.query.size(); i ++)
			{
				QueryActivity qryAct = (QueryActivity)this.query.get(i);
				java.util.Vector pageAccess = qryAct.getAccess ();
				int count = 0;
				for (int j = 0; j < pageAccess.size(); j ++)
				{
					AccessActivity accAct = (AccessActivity)pageAccess.get(j);
                    int userId = accAct.getWebPageId ();
                    UpdateUser user = (UpdateUser)this.vUpdateUsers.get (userId - 1);
					//last crawl timestamp
					java.util.Calendar crawlstamp = java.util.Calendar.getInstance ();
					crawlstamp.setTime (getLastCrawlTime (user, accAct.getTimestamp ()));
					trace(accAct.getTimestamp() + ", " + user.getUserId());
					trace(""+crawlstamp.getTime());
					//last update timestamp
					java.util.Calendar updatestamp = java.util.Calendar.getInstance ();
					updatestamp.setTime (getLastUpdateTime (user, accAct.getTimestamp ()));
					trace(""+updatestamp.getTime ());
										
					if (updatestamp.before(crawlstamp)){
						count ++;
					}
				}
				if (pageAccess.size () > 0)
					percentage += 1.0 * count / pageAccess.size ();
			}
			
			freshness = (int)(percentage / query.size () * 100);
		}*/
		
		double ratio = 0.7;
		if (this.query.size() > 0){
			for (int i = 0; i < this.query.size(); i ++)
			{
				QueryActivity qryAct = (QueryActivity)this.query.get(i);
				java.util.Vector pageAccess = qryAct.getAccess ();
				int count = 0;
				for (int j = 0; j < pageAccess.size(); j ++)
				{
					AccessActivity accAct = (AccessActivity)pageAccess.get(j);
                    int userId = accAct.getWebPageId ();
                    UpdateUser user = (UpdateUser)this.vUpdateUsers.get (userId - 1);
					//last crawl timestamp
					java.util.Calendar crawlstamp = java.util.Calendar.getInstance ();
					crawlstamp.setTime (getLastCrawlTime (user, accAct.getTimestamp ()));
					trace(accAct.getTimestamp() + ", " + user.getUserId());
					trace(""+crawlstamp.getTime());
					//last update timestamp
					java.util.Calendar updatestamp = java.util.Calendar.getInstance ();
					updatestamp.setTime (getLastUpdateTime (user, accAct.getTimestamp ()));
					trace(""+updatestamp.getTime ());
										
					if (updatestamp.before(crawlstamp)){
						count ++;
					}					
				}
				if ( 1.0*count/pageAccess.size() > ratio)
						freshness ++;
			}
			freshness = (int)(100 * freshness / query.size ());
		}
		return freshness;
	}
	private java.util.Date getLastCrawlTime (UpdateUser user, java.util.Date timestamp){
		java.util.Vector crawl = user.getCrawl();
		java.util.Date timeOk = null;
		for (int i = 0; i < crawl.size(); i++)
		{
			java.util.Date time = (java.util.Date)crawl.get(i);
			if (time.before (timestamp))
				timeOk = time;
			else
				break;
		}
		return (timeOk==null)?this.firstCrawlTime:timeOk;
		
	}
	private java.util.Date getLastUpdateTime (UpdateUser user, java.util.Date timestamp){
		java.util.Vector update = user.getAllUpdates ();
		java.util.Date timeOK = null;
		for (int i = 0; i < update.size(); i++){
			java.util.Date time = ((UpdateActivity)update.get(i)).getTimestamp ();
			if (time.before (timestamp))
				timeOK = time;
			else
				break;
		}
		java.util.Date display = new java.util.Date ();
		if (timeOK==null)
			display = this.firstCrawlTime;
				else display = timeOK;
		trace("return update time " + display);
		
		return (timeOK==null)?this.firstCrawlTime:timeOK;
	}
	private java.util.Date getLastAccessTime(UpdateUser user, java.util.Date timestamp) throws java.sql.SQLException
	{
		java.util.Date time = new java.util.Date(); 
		java.util.Date timeOk = null;
		String qry = "select * from Accesses where WebPageID=" + user.getUserId () + " ORDER BY AccessTime ASC";
		Statement qryStmt = this.connection.createStatement();
		ResultSet rs = qryStmt.executeQuery(qry);
		while (rs.next())
		{
			time = rs.getTimestamp(4);
			if (time.before (timestamp))
				timeOk = time;
			else
				break;
		}			
		rs.close();
		qryStmt.close();			
		return (timeOk==null)?this.firstCrawlTime:timeOk;
	}
	//count query in percentage  
	private double getAccessNumber(java.util.Date startTime, java.util.Date endTime, UpdateUser user)
	{
		double countAll = 0;

		try {
			String sql = "select QueryUID, COUNT(WebPageID) from Accesses WHERE WebPageID=? AND AccessTime >= ? AND AccessTime <= ? GROUP BY QueryUID";
			java.sql.PreparedStatement stmt = this.connection.prepareStatement (sql);
			stmt.setInt(1, user.getUserId ());
			stmt.setTimestamp (2, super.toSqlTimestamp (startTime));
			stmt.setTimestamp (3, super.toSqlTimestamp (endTime));
			java.sql.ResultSet rs = stmt.executeQuery ();
			while (rs.next ()) {
				int queryUid = rs.getInt (1);
				int count    = rs.getInt (2);
				//total access in this query
				String qry = "select TotalAccess from Queries where QueryUID=" + queryUid;
				int totalAccess = 0;
				java.sql.Statement qryStmt = this.connection.createStatement ();
				java.sql.ResultSet qryRs = qryStmt.executeQuery (qry);
				if (qryRs.next ()) {
				    totalAccess = qryRs.getInt (1);
				}
				qryRs.close ();
				qryStmt.close ();
				
				countAll += 1.0 * count / totalAccess;
			}
			rs.close ();
			stmt.close ();
		}
		catch (java.sql.SQLException e) {
			e.printStackTrace ();
		}
/*		
		if (startTime.before(endTime))
		{
			int userid = user.getUserId();
			for (int i = 0; i < this.vAllQueries.size(); i++)
			{
				QueryActivity qryAct = (QueryActivity)this.vAllQueries.get(i);
				java.util.Vector pageAccess = qryAct.getAccess();
				int size = pageAccess.size();
				if (size > 0)
				{
					int count = 0;
					for (int j = 0; j < size; j++)
					{
						AccessActivity accAct = (AccessActivity)pageAccess.get(j);
						if (userid == accAct.getWebPageId())
						{
							java.util.Date timestamp = accAct.getTimestamp();
							if (timestamp.after(startTime) && timestamp.before(endTime))
							{
								count++;
							}
							else if (timestamp.after(endTime))
								break;
						}
					}
					countAll += 1.0 * count / size;
				}
			}
		}
		else errorReport("Time is wrong");	
		System.out.println("COUNT=" + countAll);
		trace ("COUNT="+countAll);                
*/		
		return countAll;
	}
	//count if there is one page in the query
	private int getUniformAccessNumber(java.util.Date startTime, java.util.Date endTime, UpdateUser user)
	{
		int count = 0;
		int userid = user.getUserId();
		if (startTime.before(endTime))
		{
			for (int i = 0; i < this.vAllQueries.size(); i++)
			{
				QueryActivity qryAct = (QueryActivity)this.vAllQueries.get(i);
				java.util.Vector pageAccess = qryAct.getAccess();
				for (int j = 0; j < pageAccess.size(); j++)
				{
					AccessActivity accAct = (AccessActivity)pageAccess.get(j);
					if (userid == accAct.getWebPageId())
					{
						java.util.Date timestamp = accAct.getTimestamp();
						if (timestamp.after(startTime) && timestamp.before(endTime))
						{
							count++;
							break;
						}
						else if (timestamp.after(endTime))
							break;						
					}
				}
			}
		}
		else errorReport("Time is wrong");	
		return count;
	}
	//count every page access
	private int getAllAccessNumber(java.util.Date startTime, java.util.Date endTime, UpdateUser user)
	{
		int count = 0;
		int userid = user.getUserId();
		if (startTime.before(endTime))
		{
			for (int i = 0; i < this.vAllQueries.size(); i++)
			{
				QueryActivity qryAct = (QueryActivity)this.vAllQueries.get(i);
				java.util.Vector pageAccess = qryAct.getAccess();
				for (int j = 0; j < pageAccess.size(); j++)
				{
					AccessActivity accAct = (AccessActivity)pageAccess.get(j);
					if (userid == accAct.getWebPageId())
					{
						java.util.Date timestamp = accAct.getTimestamp();
						if (timestamp.after(startTime) && timestamp.before(endTime))
						{
							count++;
						}
						else if (timestamp.after(endTime))
							break;						
					}
				}
			}
		}
		else errorReport("Time is wrong");	
		return count;
	}
	private int getUpdateNumber(java.util.Date startTime, java.util.Date endTime, UpdateUser user)
	{
		java.util.Vector updateRequest = user.getAllUpdates();
		int count = 0;
		for (int i = 0; i < updateRequest.size(); i++)
		{
			java.util.Date timestamp = ((UpdateActivity)updateRequest.get(i)).getTimestamp();
			if (timestamp.after(startTime) && timestamp.before(endTime))
				count++;
		}
		return count;
	}
	private double normalize (double score1, double score2, double factor, double percentage)
	{
		if (factor == 0)
			return 0;
		else return (score1 * percentage + (1 - percentage) * score2)/factor;	
	}
	private void errorReport(String str)
	{
		trace(str);		
	}
	
}
