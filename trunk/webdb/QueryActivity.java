/*
* This class defines a query activity information which includes the additional page access request information
*/
package webdb;

public class QueryActivity extends TimedActivity
{
    private java.util.Vector vAccess = new java.util.Vector ();
	private QueryUser user;

    /**
     * Add one page access request
     */
    public void addAccess (AccessActivity req)
    {
        this.vAccess.add (req);
    }
    /**
     * Return all page access requests
     */
    public java.util.Vector getAccess ()
    {
        return this.vAccess;
    }
    
	public void setUser (QueryUser _user)
	{
		this.user = _user;
	}
	public QueryUser getUser ()
	{
		return this.user;
	}
}
