/*
* UserScore.java 
* Util of UserScore
*/
package webdb;

public class UserScore implements java.io.Serializable {
    private UpdateUser user;
    //the score is calculated for this crawl start time

    public void setUser (UpdateUser _user)
    {
        this.user = _user;
    }
    public UpdateUser getUser ()
    {
        return this.user;
    }
    {
        this.score = _score;
    }
    public double getScore ()
    {
        return this.score;
    }
    public void setCrawlStartTime (java.util.Date time)
    {
        this.crawlStartTime = time;
    }
    public java.util.Date getCrawlStartTime ()
    {
        return this.crawlStartTime;
    }
	public void setUpdateTime (java.util.Date time)
    {
        this.updateTime = time;
    }
    public java.util.Date getUpdateTime ()
    {
        return this.updateTime;
    }
}