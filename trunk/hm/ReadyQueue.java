package hm;

/*
 * ReadyQueue.java
 *
 * Created on 23 November 2005, 19:42
 *
 */

//            System.out.println("scheMode = hundres tens ones");
//            System.out.println("[1]: within queryQ, FIFO:0, V:1, VoD:2, VoR:3");
//            System.out.println("[2]: within updateQ, FIFO:0, V:1, VoU:2");
//            System.out.println("[3]: between queryQ and updateQ, FIFO:0, UH:1, QH:2, QUTS:3");
//            System.out.println("000: FIFO");
//            System.out.println("211: VoD-V-UH 212: VoD-V-QH 213: VoD-V-QUTS");
//            System.out.println("001: FIFO-FIFO-UH 002: FIFO-FIFO-QH 003: V-QUTS-VoD");
//
//    003: QUTS-FIFO -> within each queue, result could be not FIFO due to the concurrency control,
//                      for example, there is unfinished update in the previous atom time

/*
 * @author Huiming Qu
 */
import java.util.Iterator;
import java.util.Random;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Comparator;

public class ReadyQueue{

    public static int atomTime = 5;
    public static double rho = 0.5; //query percentage, probability of query queue has higher priority
    public static int samplingPeriod = 100;
    public static double alpha = 0.01;
    public static double beta = 0.1;
    public String currentState;
    public int currentStateStartAt;
    public TranItem currentTran;

    double qod_remained = 0.0;
    double qos_remained = 0.0;

    Random ran;
//    String[] ran_test = {"0.66", "0.48", "0.69", "0.32", "0.91", "0.61", "0.26", "0.03", "0.87", "0.02"};
//    int ran_test_index;

    public PriorityQueue<TranItem> accessQ; //EDF, etc...
    public PriorityQueue<TranItem> updateQ;    //FIFO

    public double expectedQos = 0;
    public double expectedQod = 0;
    public double expectedQ = 0;
    public double lastGainedQ = 0.0;

    public static boolean no_update = false;

    public Comparator<TranItem> query_comparator;
    public Comparator<TranItem> update_comparator;


    public ReadyQueue(){
        currentState = TranItem.ACCESS; //initialization, query takes the fist turn.
        currentStateStartAt = sys.gClock;

        query_comparator = new Comparator<TranItem>(){
                                public int compare(TranItem t1, TranItem t2){
                                    int diff = diffToInt(getQueryPriority(t1) - getQueryPriority(t2));
                                    if(diff == 0)
                                        diff = t1.global_id - t2.global_id;
                                    return diff;
                                }
                                public boolean higher(TranItem t1, TranItem t2){
                                    if (compare(t1, t2)<0)
                                        return true;
                                    else
                                        return false;
                                }
                           };
        update_comparator = new Comparator<TranItem>(){
                                public int compare(TranItem t1, TranItem t2){
                                    int diff = diffToInt(getUpdatePriority(t1) - getUpdatePriority(t2));
                                    if(diff == 0)
                                        diff = t1.global_id - t2.global_id;
                                    return diff;
                                }
                                public boolean higher(TranItem t1, TranItem t2){
                                    if (compare(t1, t2)<0)
                                        return true;
                                    else
                                        return false;
                                }
                           };

        accessQ = new PriorityQueue<TranItem>(20, query_comparator);
        updateQ = new PriorityQueue<TranItem>(20, update_comparator);

        ran = new Random();
        ran.setSeed(123456789);
//        ran_test_index = 0;

    }

    /* -------------------------------------------------------------------------
     *   0.3 -> 1
     *  -0.3 -> -1
     */
    public int diffToInt(double diff){
            if(diff > 0){
                return (int)Math.ceil(diff);
            }else
                return (int)Math.floor(diff);
    }

    public double getQueryPriority(TranItem t){
        double p = 0.0;

        switch (Config.sched_mode.charAt(0)) {
            case '0': //FIFO
                p = t.arrTime;
                break;
            case '1': //V
                p = 1/t.getQMax(sys.gClock);
                break;
            case '2': //VoD
                p = t.relDeadline/t.qc.getQosMax();
                break;
            case '3': //VoD_STAR
                p = t.relDeadline/(t.qc.getQosMax() - t.qc.getQoSMin());
                break;
            default:
                return t.arrTime;
        }//switch
//        System.out.println("******* query arrives at "+t.arrTime+" has p= "+p);

        return p;
    }

    public double getUpdatePriority(TranItem t){
        double p = 0.0;

        switch (Config.sched_mode.charAt(1)) {
            case '0': //FIFO
                p = t.arrTime;
                break;
            case '1': //V
                p = Monitor.getPriorityOnData_qod(t.getDataIndex(0));
                if (p!=0)
                    p = 1/p;
                else
                    p = Config.MAX_INT; //just a big number
                break;
            case '2': //VoU
                p = Monitor.getPriorityOnData_qod_uu(t.getDataIndex(0));
                if (p!=0)
                    p = 1/p;
                else
                    p = Config.MAX_INT; //just a big number
                break;
            default:
                return t.arrTime;
        }//switch
//        System.out.println("******* update arrives at "+t.arrTime+" has p= "+p);

        return p;
    }

    public Comparator<TranItem> getQueryComparator(){
        return query_comparator;
    }

    public double getLowLevelQueuePriority(TranItem t){
        if (t!=null){
            if (t.isUpdate())
                return getUpdatePriority(t);
            else if (t.isAccess())
                return getQueryPriority(t);
        }
        return -1;
    }
    /* -------------------------------------------------------------------------
     * adjust the time share of update and access
     *
     * new_rho = min(0.5 * S / D + 0.5, 1)
     * rho = (1 - beta) * rho + beta * new_rho; beta = 0.1
     */
    public void adjustTimeShare(){

        if(sys.gClock != 0 && (sys.gClock % samplingPeriod == 0)){

            //changing beta according to the dynamic enviroment
            //bigger difference, bigger beta
            double qosLost = Monitor.sample_getLostQos();
            double qodLost = Monitor.sample_getLostQod();
            double expectedQ = Monitor.sample_expectedQ();
            double max_beta = 0.1;
            beta = Math.abs(qodLost - qosLost)/expectedQ * max_beta;
            //or a fixed value
            beta = 0.1;

            //gradiant descent
            double s = Monitor.sample_expectedQos;
            double d = Monitor.sample_expectedQod;
            double new_rho;

            if(d > 0)
                new_rho = s/(2*d) + 0.5;
            else
                new_rho = 0.99;

            if (new_rho > 1)
                new_rho = 1;
            rho = (1 - beta) * rho + beta * new_rho;
            set_rho(rho);

            //print ontime info
            double newQosEx = Monitor.sample_expectedQos - qos_remained;
            double newQodEx = Monitor.sample_expectedQod - qod_remained;
            double newly_arrively_expectedQ = newQosEx + newQodEx;

            String info = "" + Config.digit2.format(rho) + "\t" + Config.digit2.format(1 - rho)
                        + "\t" + Config.digit1.format(qosLost) + "\t" + Config.digit1.format(qodLost)
                        + "\t" + Config.digit1.format(expectedQ) + "\t" + Config.digit1.format(newly_arrively_expectedQ)
                        + "\t" + Config.digit1.format(newQosEx) + "\t" + Config.digit1.format(newQodEx);
            Monitor.AddOntimeStat(info, newQosEx, newQodEx, expectedQ);

            //initialization for the next sampling period
            initializeMaxExpectedQosQod();
            Monitor.sampling_reset();
        }
    }

    //check if rho is in [0, 1]
    public void set_rho(double ro){
        if(ro > 1.0)
            rho = 1.0;
        else if (ro < 0)
            rho = 0.0;
        else
            rho = ro;
    }

    public void initializeMaxExpectedQosQod(){
        Iterator it = accessQ.iterator();
        TranItem tran;
        qod_remained = 0.0;
        qos_remained = 0.0;
        while(it.hasNext()){
            tran = (TranItem)it.next();
            qod_remained += tran.getQodMax();
            qos_remained += tran.getQosMax();
        }
        if(currentTran!=null && currentTran.isAccess()){
            qod_remained += currentTran.getQodMax();
            qos_remained += currentTran.getQosMax();
        }
        Monitor.sample_expectedQod = qod_remained ;
        Monitor.sample_expectedQos = qos_remained ;
    }

    /* -------------------------------------------------------------------------
     * change the state according to the qChance and uChance
     *
     * stateChange could happen when
     *      1. running execute()'s step 1: in poll(), the current queue has no trans to return
     *      2. running execute()'s step 2: in headHasHigherPriority(), if the current state times up
     * return
     *      0: when both queues are empty
     *      1: successfully "changed" the state, could be same with last time
     */
    public int stateChange(){
        double token;
        boolean chooseAgain = true;
        int index;
        if(!no_update){
            // to avoid infinite choosing from two empty queue
            // and the case one queue is empty but always choose that queue, say qChance = 1
            if(isEmpty())// | (accessQ.isEmpty() && uChance == 0) | (updateQ.isEmpty() && qChance == 0))
                return 0;
            else if (accessQ.isEmpty())
                currentState = TranItem.UPDATE;
            else if (updateQ.isEmpty())
                currentState = TranItem.ACCESS;
            else{
                currentStateStartAt = sys.gClock; //change the start time ONLY WHEN it is not for empty queue
                token = ran.nextDouble();    //get a double between 0-1
                //            token = Double.parseDouble(ran_test[ran_test_index % 10]);
                //            ran_test_index++;
                if(token < rho)
                    currentState = TranItem.ACCESS;
                else
                    currentState = TranItem.UPDATE;
            }
            return 1;
        }
        return 0;
    }

    public boolean timeOut(){
        return sys.gClock - currentStateStartAt >= atomTime;
    }


    public boolean runningUpdate(){
        return currentState.equals(TranItem.UPDATE);
    }
    public boolean runningQuery(){
        return currentState.equals(TranItem.ACCESS);
    }

    public boolean headHasHigherPriority(TranItem cur){
        //check if state should be switched
        if(timeOut())
            stateChange();
        //pick the highest priority trans
        TranItem head = peek();
        if (head!=null && hasHigherPriority(head, cur))
            return true;
        else
            return false;
    }

    /* -------------------------------------------------------------------------
     * return true only if t1 has higher priority than t2, not even ==
     */
    public boolean hasHigherPriority(TranItem t1, TranItem t2){
        int t1_minus_t2 = 0;

        if (t1 == null) // t1 has lower priority, could be both null
            return false;
        else if (t2 == null) // t1 has higher priority
            return true;

        // use query comparator if both query
        if(t1.isAccess() && t2.isAccess())
            t1_minus_t2 = query_comparator.compare(t1, t2);
        // use update comparator if both update
        else if(t1.isUpdate() && t2.isUpdate())
            t1_minus_t2 = update_comparator.compare(t1, t2);
        // if t1's type equals currentState favorates, t1 - t2 should be <0, thus -1
        else if(t1.type.equals(currentState))
            t1_minus_t2 = -1;
        // otherwise, t2's type equals currentState favorates, t1 - t2 should be >0, thus 1
        else
            t1_minus_t2 = 1;

        if (t1_minus_t2 < 0)
            return true;
        else
            return false;
    }

    public TranItem peek(){
        TranItem ti;
        if(runningQuery())
            ti = accessQ.peek();
        else
            ti = updateQ.peek(); //FIFO now
        return ti;
    }

    public TranItem poll(){
        TranItem ti = null;
        TranItem tp = null;

        if(ti==null && runningUpdate()){
            ti = updateQ.poll();
            if(ti == null){
                if(stateChange()==1){
                    ti = poll();
                }
            }
        }
        if(ti==null && runningQuery()){
            ti = accessQ.poll();
            if(ti == null)
                if(stateChange()==1){
                    ti = poll();
                }
        }
        return ti;
    }


    public void offer(TranItem ti){
        if(ti.isAccess()){
            accessQ.offer(ti);
        }
        else if(ti.isUpdate())
            updateQ.offer(ti);
        else
            Config.Error_TranType("ReadyQueue.offer()");
    }

    public void increaseExpectation(TranItem ti){
        expectedQos += ti.getQosMax();
        expectedQod += ti.getQodMax();
        expectedQ += ti.getQMax(sys.gClock);
    }
    public void decreaseExpectation(){

    }

    public boolean isEmpty(){
        return (accessQ.isEmpty() && updateQ.isEmpty());
    }

    public int size(){
        return accessQ.size() + updateQ.size();
    }

    public void listAccess(){
        System.out.println("---------- readyQ.accessQ-------------");
        Iterator it = accessQ.iterator();
        TranItem t;
        for(int i=0; i<accessQ.size(); i++){
            t = (TranItem)it.next();
            System.out.println(" p = " + getQueryPriority(t) + " " + t.toString());
        }
    }
    public void listUpdate(){
        System.out.println("---------- readyQ.updateQ-------------");
        Iterator it = updateQ.iterator();
        TranItem t;
        for(int i=0; i<updateQ.size(); i++){
            t = (TranItem)it.next();
            System.out.println(" p = " + getUpdatePriority(t) + " " + t.toString());
        }
    }
    public void list(){
        if(accessQ.size()>0)
            listAccess();
        if(updateQ.size()>0)
            listUpdate();
        if(currentTran != null)
            System.out.println(currentTran);
    }
    public void invalidateUpdate(TranItem ti){
        Iterator it = updateQ.iterator();
        TranItem ti_in = new TranItem();
        for(int i=0; i<updateQ.size(); i++){
            ti_in = (TranItem)it.next();
            if (ti_in.getDataIndex(0) == ti.getDataIndex(0)){ // this update should be dropped since the newest update has come.
                updateQ.remove(ti_in);
                break;
            }
        }
    }

    public boolean remove(TranItem ti){
        boolean has;
        if(ti.isAccess()){
            has = accessQ.remove(ti);
        }else{
            has = updateQ.remove(ti);
        }
        return has;
    }

//    * dont understand why not simply use PriorityQueue.remove(), thus remove the following
//    public boolean remove(TranItem ti){
//        boolean has = false;
//        TranItem tran = null;
//        PriorityQueue<TranItem> afterDeletion_access = new PriorityQueue<TranItem>(20, query_comparator);
//        PriorityQueue<TranItem> afterDeletion_update = new PriorityQueue<TranItem>(20, update_comparator);
//
//        Iterator<TranItem> tran_iterator;
//
//        if(ti.isAccess()){
//            tran_iterator = accessQ.iterator();
//            for(int i=0; i<accessQ.size(); i++){
//                tran = tran_iterator.next();
//                if (tran.global_id != ti.global_id){
//                    afterDeletion_access.add(tran);
//                }else
//                    has = true;
//            }
//            accessQ = afterDeletion_access;
//        }else{
//            tran_iterator = updateQ.iterator();
//            for(int i=0; i<updateQ.size(); i++){
//                tran = tran_iterator.next();
//                if (tran.global_id != ti.global_id){
//                    afterDeletion_update.add(tran);
//                }else
//                    has = true;
//            }
//            updateQ = afterDeletion_update;
//        }
//        return has;
//    }



}


//update always has lower priority
class PQueue_UL extends ReadyQueue{

    public PQueue_UL(){
        super();
        currentState = TranItem.ACCESS;
    }

    public boolean headHasHigherPriority(TranItem cur){
        TranItem head = peek();
        if (head!=null && hasHigherPriority(head, cur))
            return true;
        else
            return false;
    }

    public TranItem peek(){
        TranItem ti = accessQ.peek();
        if(ti == null)
            ti = updateQ.peek();
        return ti;
    }

    public TranItem poll(){
        TranItem ti = accessQ.poll();
        if(ti == null)
            ti = updateQ.poll();
        return ti;
    }

}

//update always has higher priority
class PQueue_UH extends ReadyQueue{

    public PQueue_UH(){
        super();
        currentState = TranItem.UPDATE;
        //System.out.println(">>>>>>>>>>>>>>>>>>>>>> currentState is "+ currentState);
    }

    public boolean headHasHigherPriority(TranItem cur){
        //System.out.println("------ currentState is "+ currentState);
        TranItem head = peek();
        if (head!=null && hasHigherPriority(head, cur)){
            return true;
        }
        else{
            return false;
        }

    }

    public TranItem peek(){
        TranItem ti = updateQ.peek();
        if(ti == null)
            ti = accessQ.peek();
        return ti;
    }

    public TranItem poll(){
        TranItem ti = updateQ.poll();
        if(ti == null)
            ti = accessQ.poll();
        return ti;
    }

}

class PQueue_TEST extends ReadyQueue{

}

/* Unified priority queue with both queries and updates ordered by arrival time
 */
class PQueue_UNIFY extends ReadyQueue{
    public PriorityQueue<TranItem> readyTran;

    public PQueue_UNIFY(){
        super();
        readyTran = new PriorityQueue<TranItem>();
    }

    public boolean headHasHigherPriority(TranItem cur){
        TranItem head = peek();
        if ( head!=null && (head.compareTo(cur)<0 ) )
            return true;
        else
            return false;
    }

    /* -------------------------------------------------------------------------
     * return true only if t1 has higher priority than t2, not even ==
     */
    public boolean hasHigherPriority(TranItem t1, TranItem t2){
        return t1.compareTo(t2) < 0;
    }

    public TranItem peek(){
        TranItem ti = readyTran.peek();
        return ti;
    }

    public TranItem poll(){
        TranItem ti = readyTran.poll();
        return ti;
    }

    public void offer(TranItem ti){
        readyTran.offer(ti);
    }

    public boolean isEmpty(){
        return readyTran.isEmpty();
    }

    public int size(){
        return readyTran.size();
    }

    public boolean remove(TranItem ti){
        return readyTran.remove(ti);
    }

//    * dont understand why not simply use PriorityQueue.remove(), thus remove the following
//    public boolean remove(TranItem ti){
//        boolean has = false;
////        has = readyTran.remove(ti);
//        TranItem tran = null;
//        PriorityQueue<TranItem> afterDeletion = new PriorityQueue<TranItem>();
//        Iterator<TranItem> tran_iterator = readyTran.iterator();
//        for(int i=0; i<readyTran.size(); i++){
//            tran = tran_iterator.next();
//            if (tran.global_id != ti.global_id){
//                afterDeletion.add(tran);
//            }else
//                has = true;
//        }
//
//        readyTran = afterDeletion;
//
//        return has;
//    }

}



