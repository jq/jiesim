/*
* UpdateUser.java 
* user definition
*/
package webdb;

public class UpdateUser extends UserBase {
    private java.util.Date lastCrawlTime;
	//update record in one crawl period
    private java.util.Vector vUpdates = new java.util.Vector ();
	//real update times
	private java.util.Vector vAllUpdates = new java.util.Vector();    
	//crawl sequence
    private java.util.Vector vCrawl = new java.util.Vector ();

    public java.util.Vector getUpdates ()
    {
        return this.vUpdates;
    }
    public java.util.Vector getAllUpdates()
	{
		return this.vAllUpdates;
	}
	public java.util.Vector getCrawl()
	{
		return this.vCrawl;
	}
	public void addUpdate(UpdateActivity act)
    {
        act.setUser (this);
        this.vUpdates.add (act);
    }
    public void addAllUpdate(UpdateActivity act)
	{
		act.setUser(this);
		this.vAllUpdates.add(act);
	}
	public void addCrawl(java.util.Date _time)
    {
		this.vCrawl.add(_time);
		this.setLastCrawlTime (_time);
    }
    //this is the begin time of last crawl
    public java.util.Date getLastCrawlTime ()
    {
        return this.lastCrawlTime;
    }
    public void setLastCrawlTime (java.util.Date _time)
    {
        this.lastCrawlTime = _time;
    }
}
