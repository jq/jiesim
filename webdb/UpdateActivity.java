/*
* This class defines a update activity information
*/
package webdb;

public class UpdateActivity extends TimedActivity
{
    private String page;
    private int orgSize, size;
	private UpdateUser user;
    
	public void setPage (String _page)
	{
		this.page = _page;
	}
	public String getPage ()
	{
		return this.page;
	}
	public void setOrgSize (int _orgSize)
	{
		this.orgSize = _orgSize;
	}
	public int getOrgSize ()
	{
		return this.orgSize;
	}
	public void setSize (int _size)
	{
		this.size = _size;
	}
	public int getSize ()
	{
		return this.size;
	}
    public int getPageSizeDiff ()
    {
        return this.size - this.orgSize;
    }
	public void setUser (UpdateUser _user)
	{
		this.user = _user;
	}
	public UpdateUser getUser ()
	{
		return this.user;
	}
}
