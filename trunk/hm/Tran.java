package hm;

/*
 * Tran.java
 *
 * Created on 27 September 2005, 13:19
 *
 * system transactions info.
 */

/*
 * @author Huiming Qu
 */

import java.util.LinkedList;
import java.util.Vector;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Comparator;

public class Tran {
    public Data data;
    public Controller controller;
    public User users;

    public PriorityQueue<TranItem> accessQ; //query queue, access and query are used both for query
    public PriorityQueue<TranItem> updateQ;

    public LinkedList<TranItem> submitQ;
    public LinkedList<TranItem> terminateQ;
    public ReadyQueue readyQ;
    public LinkedList<TranItem> blockQ;

    public LinkedList<TranItem> rejectQ;
    public LinkedList<TranItem> abortQ;
    public LinkedList<TranItem> query_fresh_commitQ;
    public LinkedList<TranItem> query_stale_commitQ;
    public LinkedList<TranItem> query_terminateQ;
    public LinkedList<TranItem> query_submitQ;

    public LinkedList<TranItem> update_rejectQ;
    public LinkedList<TranItem> update_abortQ;
    public LinkedList<TranItem> update_commitQ;

    public TranItem currentTran;

    public int num_access_submit;
    public int num_update_submit;
    public int num_tran_submit;

    //testing
    public int test_access_trylock;
    public int test_access_earlydrop;

    /*************************** function begin **********************************/
    public Tran(Data d, Controller c, User users) {
        controller = c;
        data = d;
        this.users = users;

        accessQ = new PriorityQueue<TranItem>();
        updateQ = new PriorityQueue<TranItem>();

        rejectQ = new LinkedList<TranItem>();
        abortQ = new LinkedList<TranItem>();
        query_fresh_commitQ = new LinkedList<TranItem>();
        query_stale_commitQ = new LinkedList<TranItem>();
        update_rejectQ = new LinkedList<TranItem>();
        update_abortQ = new LinkedList<TranItem>();
        update_commitQ = new LinkedList<TranItem>();
        query_terminateQ = new LinkedList<TranItem>();
        query_submitQ = new LinkedList<TranItem>();
        terminateQ = new LinkedList<TranItem>();

        submitQ = new LinkedList<TranItem>();
        blockQ = new LinkedList<TranItem>();

        switch (Config.sched_mode.charAt(2)){
            case '0': //FIFO
                //System.out.println("-------in FIFO--------");
                readyQ = new PQueue_UNIFY();
                break;
            case '1': //FIFO-UH
                //System.out.println("-------in UH--------");
                readyQ = new PQueue_UH();
                break;
            case '2': //FIFO-UL
                //System.out.println("-------in UL--------");
                readyQ = new PQueue_UL();
                break;
            case '3':  //QUTS
                //System.out.println("-------in QUTS--------");
                readyQ = new ReadyQueue();
                break;
            default:
                //System.out.println("-------in default--------");
                readyQ = new ReadyQueue();
        }

        num_update_submit = 0;
        num_access_submit = 0;
        num_tran_submit = 0;

        test_access_trylock = 0;
        test_access_earlydrop = 0;

        currentTran = null;
    }



    /* ------------------------------------------------------------------------
     * add tran to accessQ
     */
    public int addAccess(int arrT, Vector baseD, int estET, UserItem user, int relD, int nUnapplied, int qos_max, int qod_max, int qos_min, int qod_min){
        num_tran_submit ++;
        accessQ.add(new TranItem(num_tran_submit, TranItem.ACCESS, arrT, baseD, estET, user, relD, nUnapplied, qos_max, qod_max, qos_min, qod_min));
        return num_tran_submit;
    }
    public int addUpdate(int arrT, int baseD, int estET){
        num_tran_submit ++;
        updateQ.add(new TranItem(num_tran_submit, TranItem.UPDATE, arrT, baseD, estET));
        return num_tran_submit;
    }


    /* -------------------------------------------------------------------------
     * check to see if there comes any job in accessQ at the particular simulation time gClock
     * put the job in the submitQ
     */
    public void transArriving(){
        TranItem newTran;
        //fetch from accessQ
        newTran = accessQ.peek();
        while(newTran!=null && newTran.arrTime <= sys.gClock){//this transaction should be arriving already
            newTran = accessQ.remove();
            tran_submit(newTran);
            newTran = accessQ.peek();
            num_access_submit ++;
        }
        //fetch from updateQ
        newTran = updateQ.peek();
        while(newTran!=null && newTran.arrTime <= sys.gClock){//this transaction should be arriving already
            newTran = updateQ.remove();
            //newTran.setAccessIndex(num_access_submit);
            tran_submit(newTran);
            newTran = updateQ.peek();
            num_update_submit ++;
        }
    }

    /* -------------------------------------------------------------------------
     * if any update arriving in updateQ
     * if any query arriving from each user
     * put the job in the submitQ
     */
    public void transArrivingFromUser(User user){
        TranItem newTran;

        //fetch from each user
        for(int i=0; i<user.size(); i++){
            newTran = user.getUser(i).tranArriving();
            while(newTran!=null){
                tran_submit(newTran);
                newTran = user.getUser(i).tranArriving();
                num_access_submit ++;
            }
        }

        //fetch from updateQ
        newTran = updateQ.peek();
        while(newTran!=null && newTran.arrTime <= sys.gClock){//this transaction should be arriving already
            newTran = updateQ.remove();
            //newTran.setAccessIndex(num_access_submit);
            tran_submit(newTran);
            newTran = updateQ.peek();
            num_update_submit ++;
        }
    }

    /* ------------------------------------------------------------------------
     * admission control
     */
    public void admissionCtrl(){
        while ( !(submitQ.isEmpty()) ){
            TranItem ti = submitQ.poll();
            if(controller.admissionCtrl(ti, readyQ))
                tran_admit(ti);
            else
                tran_reject(ti);
        }
    }

    public void tran_admit(TranItem ti){
        if(ti!=null){
            if(ti.isAccess()){
                //controller.increaseSubmit();
            }
            else if(ti.isUpdate()){ //invalidate the current update on the same id
                readyQ.invalidateUpdate(ti);
            }else{
                Config.Error_TranType("Tran.admit()");
            }
            readyQ.offer(ti);
        }
    }

    public void tran_reject(TranItem ti){
        if(ti!=null)
            if(ti.isUpdate())
                update_rejectQ.offer(ti);
            else if(ti.isAccess()){
                rejectQ.offer(ti);
                ti.setRejectStatus();
                ti.user.receiveResult(ti);
            }
            else{
                Config.Error_TranType("Tran.reject()");
            }
    }



    /* ------------------------------------------------------------------------
     * executing
     *  //add new transaction if none is running
     *  //preempt the current if the readyQ's head tran(which has highest priority in ready queue) has higher priority than current running tran
     *  //abort tardy transactions
     *  //freshness check + 2PL-HP
     *  //step run
     */
    public void execute(){
        //1. add new transaction if none is running
        if (currentTran == null) {
            if ( readyQ.isEmpty() ){
                readyQ.currentStateStartAt = sys.gClock;
                return;
            }else{
                getNewCurrentTran();
            }
        }

        //2. preempt the current if the readyQ's head tran(which has highest priority in ready queue) has higher priority than current running tran
        //current deadline is later(lower priority) than head tran in readyQ, double check!!
        if( readyQ.headHasHigherPriority(currentTran) ){
            preempt();
        }

        //3. abort tardy transactions
        abortTardy();
        if (currentTran == null)
            return;

        //4. transaction can begin with putting on some info only in Multiversions, but no freshness check and 2PL_HP
        if ( !currentTran.hasStarted() )
            tran_begin();

        //5. step run
        if ((currentTran!=null)) {
            stepRun();
        }
    }

    /* currentTran should be part of the readyQ for the divine design
     * if not, readyQ need to get informed of a new currentTran
     */
    public void getNewCurrentTran(){
        currentTran = readyQ.poll();
        readyQ.currentTran = currentTran;
    }

    /* Abort transactions reading data[index] from both blockQ and readyQ
     * 1. reset remainExeTime of tran accessing index
     * 2. reset read-lock of data index
     * (error is read-lock should be reset also for those data other than index which is accessed by the aborted access)
     */
//    public void abortUserTran(int index){
//        TranItem ti = new TranItem();
//        Integer d_i = new Integer(index);
//        int j;
//
//        Iterator<TranItem> tran_iterator = readyQ.accessQ.iterator();
//        for(int i=0; i<readyQ.accessQ.size(); i++){
//            ti = tran_iterator.next();
//            if (ti.baseDataSet.contains(d_i)){
//                // reset remainExeTime
//                ti.reset();
//                // reset read-lock(error: only reset the data causing problem)
//                data.bData[index].readRelease();
//            }
//        }
//
//        tran_iterator = blockQ.iterator();
//        for(int i=0; i<blockQ.size(); i++){
//            ti = tran_iterator.next();
//            if (ti.baseDataSet.contains(d_i)){
//                // reset remainExeTime
//                ti.reset();
//                // reset read-lock(error: only reset the data causing problem)
//                data.bData[index].readRelease();
//            }
//        }
//    }

    /* restart transactions reading data[index] from both blockQ and readyQ
     * 1. reset remainExeTime of tran accessing index
     * 2. reset read-lock of data involved in that transactions
     */
    public void restartAccess(int index){
        TranItem ti = new TranItem();
        Integer d_i = new Integer(index);
        int j;

        Iterator<TranItem> tran_iterator = readyQ.accessQ.iterator();
        for(int i=0; i<readyQ.accessQ.size(); i++){
            ti = tran_iterator.next();
            if (ti.baseDataSet.contains(d_i)){
                tran_restart(ti);
            }
        }

        tran_iterator = blockQ.iterator();
        for(int i=0; i<blockQ.size(); i++){
            ti = tran_iterator.next();
            if (ti.baseDataSet.contains(d_i)){
                tran_restart(ti);
            }
        }
    }
    /* restart the update working on index, but only the first relative update in the readyQ
     * ERROR: dont understand why the first one and why even restart higher priority update
     */
    public void restartUpdate(int index){
        TranItem ti = new TranItem();
        Integer d_i = new Integer(index);
        Iterator<TranItem> tran_iterator = readyQ.updateQ.iterator();
        for(int i=0; i<readyQ.updateQ.size(); i++){
            ti = tran_iterator.next();
            if (ti.baseDataSet.contains(d_i)){
                tran_restart(ti);
                return;
            }
        }
    }

    public void tran_submit(TranItem ti){
        Monitor.increase_require_workload(ti);
        TranItem invalidated_update = null;
        //----- update -----
        if(ti.isUpdate()){
            invalidated_update = data.bData[ti.getDataIndex(0)].staleness.update_arrival(ti);
            if (invalidated_update !=null){
                if (currentTran == invalidated_update){
                    tran_abort(currentTran);
                    currentTran = null;
                }else{
                    //readyQ.invalidateUpdate(invalidated_update);
                    tran_abort(invalidated_update);
                    readyQ.remove(invalidated_update);
                }
            }
            data.bData[ti.getDataIndex(0)].versions.increaseStaleness();
        //----- access -----
        }else{
            Monitor.on_tran_submit(ti);
            query_submitQ.add(ti);
        }
        submitQ.offer(ti);
    }

    public void tran_restart(TranItem ti){
        // reset remainExeTime
        ti.reset();
        // reset read-lock/write-lock on all data involved with ti
        tran_release_unblock(ti);
    }

    public void tran_abort(TranItem ti){
        if(ti.isAccess())
            abortQ.offer(ti);
        else if(ti.isUpdate())
            update_abortQ.offer(ti);
        else{
            Config.Error_TranType("Tran.tran_commit()");
        }
        tran_terminate(ti);
    }

    public void tran_commit(TranItem ti){
        int index, ver_time;
        double freshness = 0.0;
        int data_staleness, staleness = 0;

        if(ti.isAccess()){
            query_fresh_commitQ.offer(ti);
            /*//compute freshness/staleness for MV
            for(int i=0; i<ti.getDataSize(); i++){
                index = ti.getDataIndex(0);
                ver_time = ti.getVersionTime(i);
                data_staleness = data.bData[index].versions.getVersionStaleness(ver_time);
                if (data_staleness > staleness)
                    staleness = data_staleness;
            }*/

            //compute freshness/staleness for FIFO and others
            for(int i=0; i<ti.getDataSize(); i++){
                index = ti.getDataIndex(0);
                ver_time = ti.getVersionTime(i);
                data_staleness = data.bData[index].staleness.staleness;
                if (data_staleness > staleness)
                    staleness = data_staleness;
            }

            if (staleness == 0)
                freshness = 1.0;
            else
                freshness = 1/(double)(staleness+1);

            ti.setFreshness(freshness);
            ti.setStaleness(staleness);
            //System.out.println(sys.gClock + "  freshness: "+ freshness + "\t" + ti);
        }
        else if(ti.isUpdate()){
            index = ti.getDataIndex(0);
            data.bData[index].versions.addNewVersion();
            data.bData[index].staleness.update_commit();
            update_commitQ.offer(ti);
        }
        else{
            Config.Error_TranType("Tran.tran_commit()");
        }
        tran_terminate(ti);
    }


    /* ------------------------------------------------------------------------
     * change the state of multiversion, for concurrency control
     * check for the read/write lock, restart the low priority trans
     */
    public void tran_begin(){
        int index;
        int version_time;
        int data_usage;
        DataItem di = null;
        TranItem ti =  null;
        Vector<TranItem> conflict_trans = null;
        /* if (access)
         *      if (write_locked)
         *          compare with writing tran, and restart it if it has lower priority, try to read again
         * if (write)
         *      if (write_locked)
         *          restart the lower priority tran, try write again with higher priority one
         *      if (read_locked)
         *          there maybe MORE THAN ONE reading trans.
         *          writing tran's priority should be higher than all the reading trans to preempt those all,
         *          that is, all reading tran restarts, and writing tran begins.
         *
         * how to wake up? a.k.a. maintain the block queue...
         *      when a reading tran finishes, wake all those trans in block queue which need THOSE data
         *      when a writing tran finishes, wake all those trans which need THE data
         *
         *      every DataItem has a blockQ, everytime a tran finishes
         *      all its dataset's blockedQ will be dumped into readyQ
         */
        for(int i=0; i<currentTran.getDataSize(); i++){
                index = currentTran.getDataIndex(i);
                currentTran.setVersionTime(i, sys.gClock);
                di = data.bData[index];
                if( currentTran.isAccess() ){
                    data_usage = di.cc.read_start(currentTran);
                    // if write_locked
                    if (data_usage == DataItem.OCCUPIED_WRITE){
                        ti = di.cc.writing_tran;
                        // if currentTran has higher priority, restart the other, start the current
                        if (readyQ.hasHigherPriority(currentTran, ti)){
                            di.remove_writing_tran();
                            tran_preempted(ti);
                            data_usage = di.cc.read_start(currentTran);
                        }
                        // else block the current, and get a new current
                        // when it is woken, it will be sent back to readyQ
                        else{
                            tran_blocked(currentTran);
                            getNewCurrentTran();
                        }
                    }
                }else if (currentTran.isUpdate()){
                    data_usage = di.cc.write_start(currentTran);
                    // if write_locked
                    if (data_usage == DataItem.OCCUPIED_WRITE){
                        ti = di.cc.writing_tran;
                        // if currentTran has higher priority, restart the other, start the current
                        // actually it should compare the arriving time of updates,
                        // the old update should be invalidated(removed) anyway
                        if (currentTran.arrTime > ti.arrTime){//readyQ.compare(currentTran, ti)){
                            di.remove_writing_tran();
                            di.blockQ.remove(ti);//tran_preempted(ti);
                            data_usage = di.cc.write_start(currentTran);
                        }
                        // else block the current, and get a new current
                        // when it is woken, it will be sent back to readyQ
                        else{
                            //tran_blocked(currentTran);
                            getNewCurrentTran();
                        }
                    // if read_locked
                    }else if (data_usage == DataItem.OCCUPIED_READ){
                        boolean higher = true;
                        for(int k =0; k<di.get_reading_lock(); k++){
                            ti = di.get_reading_tran(k);
                            if (!readyQ.hasHigherPriority(currentTran, ti)){
                                higher = false; // if only one ti has lower priority
                                break;
                            }
                        }
                        // restart all if writing tran is higher than all
                        if(higher){
                            for(int k =0; k<di.get_reading_lock(); k++){
                                ti = di.get_reading_tran(k);
                                tran_preempted(ti);
                            }
                            di.remove_all_reading_tran();
                            data_usage = di.cc.write_start(currentTran);
                        }
                        // else block the current, and get a new current
                        // when it is woken, it will be sent back to readyQ
                        else{
                            tran_blocked(currentTran);
                            getNewCurrentTran();
                        }
                    }
                }else
                    Config.Error_TranType("Tran::tran_begin");

        }
    }

    public void tran_preempted(TranItem ti){
        tran_restart(ti);
        tran_blocked(ti);
        readyQ.remove(ti);
    }

    public void tran_blocked(TranItem ti){
        DataItem di = null;
        for(int i=0; i<ti.getDataSize(); i++){
            di = data.bData[ti.getDataIndex(i)];
            di.blockQ.add(ti);
        }
    }


    public void stepRun(){
        //reduce the remainingExeTime
        //check if terminated at the same time: TranItem.stepRun return true if stil running, false if finished.
        //if finished, call commit and call tran_terminate
        if(currentTran == null) return;
        boolean running = currentTran.stepRun();
        Monitor.increase_running_workload(currentTran);
//        controller.increaseBusyTime();

        if(!running){
            tran_commit(currentTran);
            getNewCurrentTran();

        }
    }

    /* using multiversion, update version state and transaction state
     */
    public void tran_terminate(TranItem tran){
        // for access:
        //   release read lock in multiversion
        // for update:
        //   append a new entry to multiversion, but done in tran_commit()

        int index;
        int ver_time;

        tran_release_unblock(tran);

        if(tran.isAccess()){
            //set status
            tran.setFinishStatus();
            query_terminateQ.offer(tran);
            //inform user the query is done
            tran.user.receiveResult(tran);
        }else{
            data.bData[tran.getDataIndex(0)].staleness.update_terminate();
        }

        //monitor qos, qod
        Monitor.on_tran_terminate(tran);

        tran.setFinishTime();
        terminateQ.add(tran);

    }

    public void tran_release_unblock(TranItem tran){
        int index;
        for(int i=0; i<tran.getDataSize(); i++){
            index = tran.getDataIndex(i);
            if(tran.hasStarted()){
                if(tran.isAccess())
                    data.bData[index].cc.read_finish(tran);
                else if(tran.isUpdate())
                    data.bData[index].cc.write_finish(tran);
                else
                    Config.Error_TranType("Tran::tran_release_data");
            }
            wakenBlocked(index);
        }
    }

    public void wakenBlocked(int index){
        LinkedList<TranItem> tempQ = data.bData[index].blockQ;
        while(!tempQ.isEmpty()){
            readyQ.offer(tempQ.poll());
        }
    }

    public void preempt(){
        // we can insert the currentTran back in readyQ and dont worry it will be fetched back again, we've checked already(no tie)
        // even it is checked out again, it has the highest priority, the tie situation wont pass the check procedure in execute().
        readyQ.offer(currentTran);
        getNewCurrentTran();
    }

    /* delete those transactions missed deadlines in readyQ and blockQ
     * change currentTran to next non-missed tran, it might become null
     */
    public void abortTardy(){
        TranItem tran;
        Iterator it;
        PriorityQueue<TranItem> tempQ = new PriorityQueue<TranItem>(20, readyQ.getQueryComparator());
        //readyQ
        while(!readyQ.accessQ.isEmpty()){
            tran = readyQ.accessQ.poll();;
            if( tran.deadlineMissed() ) {
                tran_abort(tran);
                test_access_earlydrop ++;
            }else
                tempQ.offer(tran);
        }
        readyQ.accessQ = tempQ;

        //currentTran
        if(currentTran!=null && currentTran.deadlineMissed()){
            tran_abort(currentTran);
            getNewCurrentTran();
        }


    }

    /*
     */
    public void adjustTimeShare(){
        readyQ.adjustTimeShare();
    }


    /* ------------------------------------------------------------------------
     * Print status and listing info for queues
     */

    public void sysSnapshot(){
        System.out.println("=================== Time Tick " + sys.gClock + "======================");
        listSubmitQ();
        listReadyQ();
        listCommitQ();
        listAbortQ();
        listUpdateCommitQ();
        listUpdateAbortQ();
        listCurrentTran();
    }

    public void listSubmitQ(){
        System.out.println("--------Submit Queue------------");
        for(int i=0; i<submitQ.size(); i++){
            System.out.println(submitQ.get(i));
        }
    }
    public void listReadyQ(){ // can list everything, but not ordered.
        System.out.println("--------Ready Queue------------");
        readyQ.listAccess();
        readyQ.listUpdate();
    }
    public void listAccessQ(){
        System.out.println("--------Access Queue w/ NO ORDER ------Total is "+ accessQ.size() + "------");
        Iterator<TranItem> tran_iterator = accessQ.iterator();
        for(int i=0; i<accessQ.size(); i++){
            TranItem ti = tran_iterator.next();
            System.out.println(ti);
        }
//        //The following is to check if queries are ordered well in accessQ
//        int num = accessQ.size();
//        for(int i=0; i<num; i++){
//            TranItem ti = accessQ.remove();
//            System.out.println(ti);
//        }
    }
    public void listUpdateQ(){
        System.out.println("--------Update Queue w/ NO ORDER ------Total is "+ updateQ.size() + "------");
        Iterator<TranItem> tran_iterator = updateQ.iterator();
        for(int i=0; i<updateQ.size(); i++){
            TranItem ti = tran_iterator.next();
            System.out.println(ti);
        }
//        //The following is to check if updates are ordered well in updateQ
//        int num = updateQ.size();
//        for(int i=0; i<num; i++){
//            TranItem ti = updateQ.remove();
//            System.out.println(ti);
//        }
    }
    public void listRejectQ(){
        System.out.println("--------Reject Queue------------");
        for(int i=0; i<rejectQ.size(); i++){
            System.out.println(rejectQ.get(i));
        }
    }
    public void listAbortQ(){
        System.out.println("--------Abort Queue------------");
        for(int i=0; i<abortQ.size(); i++){
            System.out.println(abortQ.get(i));
        }
    }
    public void listCommitQ(){
        System.out.println("--------Fresh Commit Queue------------");
        for(int i=0; i<query_fresh_commitQ.size(); i++){
            System.out.println(query_fresh_commitQ.get(i));
        }
        System.out.println("--------Stale Commit Queue------------");
        for(int i=0; i<query_stale_commitQ.size(); i++){
            System.out.println(query_stale_commitQ.get(i));
        }
    }
    public void listUpdateAbortQ(){
        System.out.println("--------Update Abort Queue------------");
        for(int i=0; i<update_abortQ.size(); i++){
            System.out.println(update_abortQ.get(i));
        }
    }
    public void listUpdateCommitQ(){
        System.out.println("--------Update Commit Queue------------");
        for(int i=0; i<update_commitQ.size(); i++){
            System.out.println(update_commitQ.get(i));
        }
    }
    public void listCurrentTran(){
        System.out.println("--------Current Running Tran------------");
        System.out.println(currentTran);
    }


}

