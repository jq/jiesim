/*
* User.java 
* user definition
*/
package webdb;

public abstract class UserBase implements java.io.Serializable {
    private int usertype;
    private int userId;	

    public void setUserId (int id)
    {
            this.userId = id;
    }
    public int getUserId ()
    {
            return this.userId;
    }
    public void setUserType (int _type)
    {
            this.usertype = _type;
    }
    public int getUserType ()
    {
            return this.usertype;
    }
}
