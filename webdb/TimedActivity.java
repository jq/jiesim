/*
* Basic activity information
*/
package webdb;

public abstract class TimedActivity extends ActivityBase {
    private java.util.Date timestamp = null;
	public void setTimestamp (java.util.Date _timestamp)
	{
		this.timestamp = _timestamp;
	}
	public java.util.Date getTimestamp ()
	{
		return this.timestamp;
	}
}
