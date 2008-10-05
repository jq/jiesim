package hm;

/*
 * DataItem.java
 *
 * Created on 27 September 2005, 13:18
 *
 * single data info.
 */

/*
 * @author Huiming Qu
 */
import java.util.Vector;
import java.lang.Math;
import java.util.LinkedList;

public class DataItem {

    public int index;
    public PeriodicUPD p_update;

    public int nAccess;
    public int nUpdate;
    public double nTickets;
    public double nHistoryTicket;
    public double nWeightedTicket;
    public double nWeightedHistoryTicket;

    public boolean updateOnCall;

    public Multiversion versions; //need to be changed
    public twoPL locks;             //need to be changed
    public concurrencyControl cc;

    public static final int UNOCCUPIED = 0;
    public static final int OCCUPIED_READ = 1;
    public static final int OCCUPIED_WRITE = 2;

    public LinkedList<TranItem> blockQ;

    public Staleness staleness;

    /*************************** basic functions*******************************/
    public DataItem(int i) {
        index = i;

        nAccess = 0;
        nUpdate = 0;
        nTickets = 0.0;
        nHistoryTicket = 0.0;
        nWeightedTicket = 0.0;
        nWeightedHistoryTicket = 0.0;

        updateOnCall = false;

        versions = new Multiversion(); // need to be changed
        locks = new twoPL(); //need to be changed
        cc = new twoPL(); // if statement on Config.concur......

        blockQ = new LinkedList<TranItem>();

        staleness = new Staleness();
    }

    public String toString(){
        return "" + index;
    }

    /*************************** unified priority *****************************/
    public double get_query_priority(){
        double p = 0.0;
        return p;
    }

    /*************************** two phase lock *******************************/
    public int get_reading_lock(){
        return cc.read_lock;
    }
    public TranItem get_reading_tran(int index){
        return cc.reading_tran.get(index);
    }
    public void remove_reading_tran(TranItem ti){
        if (cc.reading_tran.remove(ti))
            cc.read_lock --;
    }
    public void remove_all_reading_tran(){
        cc.reading_tran = new Vector<TranItem>();
        cc.read_lock = 0;
    }
    public TranItem get_writing_tran(){
        return cc.writing_tran;
    }
    public void remove_writing_tran(){
        cc.writing_tran = null;
        cc.write_lock = 0;
    }
    /*************************** multiversion *********************************/
    public int num_versions(){
        return versions.versions.size();
    }

    /*************************** miscellious functions ************************/

    public Vector<TranItem> getUsingTranItem(){
        Vector<TranItem> v = cc.reading_tran;
        v.add(cc.writing_tran);
        return v;
    }

    public void setLastUpdate(){
        p_update.last_update = sys.gClock;
    }

    public boolean isFresh(){
	if (getFreshness() == 1.0) return true;
	else return false;
    }

    public double getFreshness(){
        //checking freshness by missed counts of updates
        int time = 0;
	int count = 0;
//	if(p_update.last_update<0) count =1; // first time access this data
//	else if(p_update.last_update > sys.gClock)count =0; // update on demand
//        else {
//            int no_update_period = p_update.last_update - p_update.last_update % p_update.original_period;
//            count = (sys.gClock - no_update_period)/p_update.original_period;
//        }
//
//        return ( 1/(1 + (double)count) );
        if(p_update.last_update<0) count =1; // first time access this data
	else while(time<sys.gClock)
	{
		if(time>p_update.last_update){count++;}
		time += p_update.original_period;
	}
	return ( 1/(1 + (double)count) );
    }


    /*************************** UNIT & Periodic UPD **************************/
    public void setTickets(TranItem ti){
        if(ti.isAccess()){
            nAccess ++;
            nTickets -= 1.0;
            nHistoryTicket = nHistoryTicket * Controller_USM.FORGETTING_FACTOR - 1.0;
            nWeightedHistoryTicket = nWeightedHistoryTicket * Controller_USM.FORGETTING_FACTOR - ti.getUrgency();
        }else if(ti.isUpdate()){
            nUpdate ++;
            nTickets += 1.0;
            nHistoryTicket = nHistoryTicket * Controller_USM.FORGETTING_FACTOR + 1.0;
            nWeightedHistoryTicket = nWeightedHistoryTicket * Controller_USM.FORGETTING_FACTOR + getUpdateTimeNorm();
        }else{
            Config.Error_TranType("DataItem.setTicket()");
        }

    }

    double getUpdateTimeNorm(){
        double w = 0;
        int middle = (Config.HIGH_UPDATE_EXE_TIME - Config.LOW_UPDATE_EXE_TIME)/2;
        w = 1/(1+Math.exp((double)(0 - p_update.estExeTime + middle)));
	return w;
    }

    public boolean degraded(){
        return p_update.period > p_update.original_period;
    }

    public double getWeightedHistoryTicket(){
        return nWeightedHistoryTicket;
    }

    public void setPeriodicUPD(int p, int t){
        p_update = new PeriodicUPD(index, p, t);
    }

        public void increasePeriod(){
        p_update.increasePeriod();
    }

    public void doOnDemand(){
        p_update.on_demand = true;
    }

    public void undoOnDemand(){
        p_update.on_demand = false;
    }

    public boolean isOnDemand(){
        return p_update.on_demand;
    }


}//class DataItem

/*
 * Staleness table : modified by updates arrival and commits
 *  ENTRY: staleness(int), pending_update(TranItem)
 *      update arrival  - add staleness (+1) to data, substitute arrived update with the original pending update and return the invalidated update
 *                      - invalidate the relative update in the ready queue (done in Tran level)
 *                      - abort the current running relative update (done in Tran level)
 *
 *      update commit   - reset staleness to 0 and reset pending_update
 *      update terminate    - reset pending_update
 */
class Staleness{
    public int staleness; //number of unapplied updates
    public TranItem pending_update;

    public Staleness(){
        staleness = 0;
        pending_update = null;
    }

    // return the pending_update which could be in the ready queue or current running
    public TranItem update_arrival(TranItem arrived_update){
        staleness ++;
        TranItem invalidated_update = pending_update;
        pending_update = arrived_update;
        return invalidated_update;
    }

    public void update_commit(){
        staleness = 0;
        update_terminate();
    }

    public void update_terminate(){
        pending_update = null;
    }
}

//end staleness table


/*
 * Multiversion Concurrency Control
 *
 * for each data, keep those successful updates for versions
 * <time        readlock        staleness>
 *  100         2               7
 *  101         0               6       -> should already be cleaned
 *  105         0               2       -> should already be cleaned
 *  107         1               0
 * ... details see Multiversion class in DataItem class
 *
 * 1. cleanUp process happens in
 *          addNewVersion() - readlock never >0, no chance to be read
 *          decreaseReadlock() - readlock reaches 0 again, no chance to be read again
 * 2. query keeps Version.time, but not the index in version set, because it might be changing with adding and removing elements.
 * 3. functioning method and statistic method, for details, see comments above the methods
 *
 */
class Version{
    public int time;
    public int readlock;
    public int staleness;

    public Version(){
        time = sys.gClock;
        readlock = 0;
        staleness = 0;
    }

    public String toString(){
        return time + "\t" + readlock + "\t" + staleness + "\n";
    }
}
class Multiversion{
    public Vector<Version> versions;

    public Multiversion(){
        versions = new Vector<Version>();
        addNewVersion(); //the original version
    }

    /* -------------------------------------------------------------------------
     * whenever an update finishes, new version is added with timestamp gClock
     * clean up(delete) the older versoion that has no read
     */
    public void addNewVersion(){
        if( !(versions.isEmpty()) ){
            Version last = versions.lastElement();
            if (last.readlock==0)
                versions.remove(last);
        }
        versions.add(new Version());
    }

    /* -------------------------------------------------------------------------
     * assuming all reads tends to read the most up to date data
     * return the index(version number)
     */
    public int increaseReadlock(){
        versions.lastElement().readlock ++;
        return versions.lastElement().time;
    }

    /* -------------------------------------------------------------------------------
     * whenever an update arrives, all existing versions' staleness are increased by 1
     */
    public void increaseStaleness(){
        for(int i=0; i<versions.size(); i++){
            versions.elementAt(i).staleness ++;
        }
    }

    /* -------------------------------------------------------------------------
     * whenver an access finishes, readlock should be released
     * clean up(delete) the versoion if that has no read(readlock decreases to 0)
     * make sure you dont delete the most up to date one
     */
    public void decreaseReadlock(int ver_time){
        Version v_cur;
        for(int i=0; i<versions.size(); i++){
            v_cur = versions.elementAt(i);
            if(v_cur.time == ver_time){
                v_cur.readlock --;
                if( v_cur.readlock == 0  &&  v_cur != versions.lastElement() )
                    versions.remove(v_cur);
                break;
            }
        }
    }

    /* -------------------------------------------------------------------------
     * get the staleness of the data according to the version's time
     */
    public int getVersionStaleness(int ver_time){
        Version v_cur;
        for(int i=0; i<versions.size(); i++){
            v_cur = versions.elementAt(i);
            if(v_cur.time == ver_time){
                return v_cur.staleness;
            }
        }
        return 0;
    }


}//class Multiversion

class PeriodicUPD{
    public int index;
    public int period;
    public int estExeTime;
    public int last_update_generated;

    public int last_update;
    public boolean on_demand;
    public int original_period;
    public int last_degrade_timestamp;

    public PeriodicUPD(int i, int p, int t){
        index = i;
        original_period = period = p;
        estExeTime = t;
        last_update_generated = 0 - period; // to ensure the data get updated at the beginning.
        last_update = 0;
        last_degrade_timestamp = 0;
        on_demand = false;
    }

    public TranItem generateUPD(){
        //generate an update if the data has not been updated longer than its period.
        TranItem new_update = new TranItem();

        if ( (sys.gClock - last_update_generated) >= period ){
            new_update = new_update();
            last_update_generated = sys.gClock;
            return new_update;
        }
        else return null;
    }

    public TranItem new_update(){
        System.out.println("generating a new update!!??????");
        return null;
        //return new TranItem(sys.gClock, index, estExeTime, TranItem.UPDATE);
    }

    public void increasePeriod(){
        int newP = (int)((1.0 + Controller_USM.K_DEGRADE) * period);
        last_degrade_timestamp = sys.gClock;
        if (newP <= Controller_USM.DEGRADE_BOUND * original_period){
            period = newP;
        }
    }

    public String toString(){
        return "" + index +
                "\tperiod=" + period +
                "\testExeTime=" + estExeTime +
                "\n";
    }
}//class PeriodicUPD

class concurrencyControl{
    public int read_lock;
    public int write_lock;
    public Vector<TranItem> reading_tran;
    public TranItem writing_tran;

    public concurrencyControl(){
        read_lock = 0;
        write_lock = 0;
        reading_tran = new Vector<TranItem>();
        writing_tran = null;
    }
    public int read_start(TranItem ti){return 0;}   // returns from [ UNOCCUPIED, OCCUPIED_READ, OCCUPIED_WRITE ]
    public int write_start(TranItem ti){return 0;}  // returns true [ UNOCCUPIED, OCCUPIED_READ, OCCUPIED_WRITE ]
    public void read_finish(TranItem ti){}
    public void write_finish(TranItem ti){}

}

class twoPL extends concurrencyControl{

    public twoPL(){
        super();
    }

    public int read_start(TranItem ti){
        if (isWriteLocked()) {
            //not comparing them here, if only you can, you cannot restart the failed transactions.
            return DataItem.OCCUPIED_WRITE;
        }else{
            reading_tran.add(ti);
            read_lock ++;
            return DataItem.UNOCCUPIED;
        }
    }

    public int write_start(TranItem ti){
        if (isWriteLocked()){
            return DataItem.OCCUPIED_WRITE;
        }
        else if (isReadLocked()){
            return DataItem.OCCUPIED_READ;
        }
        else{
            writing_tran = ti;
            write_lock = 1;
            return DataItem.UNOCCUPIED;
        }
    }

    public void read_finish(TranItem ti){
        reading_tran.remove(ti);
        read_lock --;
    }
    public void write_finish(TranItem ti){
        writing_tran = null;
        write_lock = 0;
    }

    /*
     **************************************************************************
     *      private functions
     ***************************************************************************
     */

    public boolean isReadLocked(){
        return read_lock>0;
    }

    public boolean isWriteLocked(){
        return write_lock>0;
    }
}
