/*
* User.java 
* user definition
*/
package webdb;

public class QueryUser extends UserBase {
    //access record
    private java.util.Vector vQuery = new java.util.Vector ();
    
    public java.util.Vector getQuery ()
    {
        return this.vQuery;
    }
    public void addQuery (QueryActivity act)
    {
        act.setUser (this);
        this.vQuery.add (act);
    }
}
