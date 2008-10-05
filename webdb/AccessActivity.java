/*
* This class defines page access request information
*/
package webdb;

public class AccessActivity extends TimedActivity
{
    /**
     * The query activity that this access activity belongs to
     */
    private QueryActivity query;
    private int webPageId;

    public void setQuery (QueryActivity act)
    {
        this.query = act;
    }
    public QueryActivity getQuery ()
    {
        return this.query;
    }
    public void setWebPageId (int id)
    {
        this.webPageId = id;
    }
    public int getWebPageId ()
    {
        return this.webPageId;
    }
}
