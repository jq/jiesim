/*
* Basic activity information
*/
package webdb;

public abstract class ActivityBase implements java.io.Serializable {
    //primary key value
    private int uid;

	public void setUid(int id)
    {
        this.uid = id;
    }
    public int getUid ()
    {
        return this.uid;
    }
}
