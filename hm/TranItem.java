package hm;

/*
 * TranItem.java
 *
 * Created on 27 September 2005, 13:19
 *
 * single transaction info.
 */

/*
 * @author Huiming Qu
 */
import java.util.Vector;

public class TranItem implements Comparable{

    public int global_id;
    public String type;

    public int arrTime;
    public Vector baseDataSet;
    public int estExeTime;

    public UserItem user;
    public int relDeadline;
    public int deadline;
    public int remainExeTime;

    public double freshness;
    public int staleness;

    public QualityContract qc;

    public static final String UPDATE = "UPDATE";
    public static final String ACCESS = "ACCESS";
    public static final int UNSTARTED = -1;
    public static final int ABORTED = -2;

    public int startTime;  // the latest time when transaction get the lock
    public int finishTime;  // equals to UNSIGNED(-1), ABORTED(-2), or it finishes at good time during <arrT, deadline>

    public String status;
    public TranItem onCalledBy;

    public int[] using_version;
//    public int access_index;

    /*************************** function begin **********************************/
    public TranItem(){}
    public TranItem(int id, String t, int arrT, Vector baseD, int estET,
            UserItem user, int relD, int nUnapplied, int qos_max, int qod_max, int qos_min, int qod_min) {
        this.global_id = id;
        type = t;

        arrTime = arrT;
        baseDataSet = baseD;
        estExeTime = estET;

        this.user = user;
        relDeadline = relD;
        deadline = arrTime + relDeadline;
        remainExeTime = estExeTime;

        if(Config.linearfunction)
            qc = new QC_linearStepPenalty(arrTime, relDeadline, nUnapplied, qos_max, qod_max, qos_min, qod_min);
        else
            qc = new QC_stepPositive(arrTime, qos_max, qod_max, relDeadline, nUnapplied);
        user.addTran(this);

        freshness = 0.0;
        finishTime = UNSTARTED;
        startTime = UNSTARTED;
        status = new String("NON_STARTED");
        using_version = new int[baseDataSet.size()];

    }

    public TranItem(int id, String t, int arrT, int dataID, int estET) { //for updates only since the deadline is not required
        this.global_id = id;
        type = t;

        Integer int_temp;
        Vector<Integer> bDataSet = new Vector<Integer>();
        int_temp = new Integer(dataID);
        bDataSet.add(int_temp);

        arrTime = arrT;
        baseDataSet = bDataSet;
        estExeTime = estET;
        relDeadline = Config.MAX_TIME; //update has no deadlines
        deadline = arrTime + relDeadline;
        remainExeTime = estExeTime;


        status = new String("NON_STARTED");
        finishTime = UNSTARTED;

        using_version = new int[baseDataSet.size()];
    }

    public boolean equals(Object ti){
        TranItem tran = (TranItem) ti;
        //System.out.println("==========i'm in equals=========");
        if(this.global_id == tran.global_id)
            return true;
        else
            return false;
    }

    public void setVersionTime(int index, int version_time){
        using_version[index] = version_time;
    }

    public int getVersionTime(int index){
        return using_version[index];
    }

    public void setFreshness(double f){
        freshness = f;
    }
    public double getFreshness(){
        return freshness;
    }
    public int getStaleness(){
        return staleness;
    }
    public int getResponsetime(){
        return finishTime - arrTime;
    }
    public void setStaleness(int s){
        staleness  = s;
    }
    public void setOnCalledBy(TranItem ti){
        onCalledBy = ti;
    }

    public void setQualityContract(double reject, double fmiss, double fstale){
        qc.setCost(reject, fmiss, fstale);
    }
    public void setFinishTime(){
        finishTime = sys.gClock;
    }
    public void setRejectStatus(){
        status = "REJECT";
    }
    public void setFinishStatus(){
        setFinishTime();

        if (remainExeTime > 0){
            status = "ABORT";
        }
        else {
            if (isOnTime()){
                status += "_ONTIME";
            }
            if (isFresh())
                status += "_FRESH";
        }
    }

    public void start(){
        status = "STARTED";
        startTime = sys.gClock;
    }
    public void end(){
        status = "FINISHED";
    }

    public boolean isFresh(){
        return qc.isFresh(freshness);
    }

    public double getUrgency(){
        return estExeTime/(double)relDeadline;
    }

    public boolean stepRun(){
        if(remainExeTime == estExeTime)
            start();
        remainExeTime--;
        if(status=="STARTED" && remainExeTime<=0 ){
            end();
            return false;
        }
        return true;
    }


    public boolean isOnTime(){
        return (remainExeTime <= 0) && (finishTime <= deadline);
    }
    public boolean dataFreshnessCheck(double data_f){
        return qc.isFresh(data_f);
    }
    public boolean hasStarted(){
        return (remainExeTime < estExeTime);
    }

    public boolean hasFinished(){
        return (remainExeTime <= 0);
    }

    public boolean deadlineMissed(){
        return deadline < sys.gClock;
    }

    public int getDataIndex(int i){
        return ((Integer)baseDataSet.elementAt(i)).intValue();
    }

    public int getDataSize(){
        return baseDataSet.size();
    }

    public String toString(){
        String s = new String("" + type);
        if(isAccess()){
//            s = s + "\t" + "accID = " + access_index;
        }
        s = s + "\t id = " + global_id + "\t";
        s = s + "arrTime = " + arrTime + "\t";
//        s = s + "startTime = " + startTime + "\t";
        s = s + "finishTime = " + finishTime + "\t";
//        s = s + "\t" + "data = ";
//        for(int i=0; i<baseDataSet.size(); i++){
//            s = s + baseDataSet.elementAt(i) + "\t";
//        }
//        s = s + "estExeTime = " + estExeTime + "\t";
//        s = s + "deadline = " + deadline + "\t";
//        s = s + "remainExeTime = " + remainExeTime + "\t";
//        s = s + "status = " + status + "\t";
        if(isAccess()){
//            s = s + "rd = " + qc.getRDMax() + "\t";
//            s = s + "uu = " + qc.getUUMax() + "\t";
            s = s + "mqos = " + Config.digit1.format(qc.getQosMax()) + "\t";
            s = s + "mqod = " + Config.digit1.format(qc.getQodMax()) + "\t";
//            s = s + "response_time = " + getResponsetime() + "\t";
//            s = s + "staleness = " + staleness + "\t";
//            s = s + "feasible_qos = " + Config.digit1.format(getQosFeasible()) + "\t";
            s = s + "qos = " + Config.digit1.format(getQos()) + "\t";
            s = s + "qod = " + Config.digit1.format(getQod()) + "\t";
        }

        s += "\n";
        return s;
    }

    public boolean isUpdate(){
        return type.equals("UPDATE");
    }

    public boolean isAccess(){
        return type.equals("ACCESS");
    }

    public boolean hasSameType(TranItem ti){
        return this.type == ti.type;
    }

    public void reset(){
        status = "NON_STARTED";
        remainExeTime = estExeTime;
    }

    //estimated maximal QoS, assuming the transaction gets executed immediately
    public double getEstQos(){
        return qc.getQos(sys.gClock + remainExeTime);
    }
    //estimated maximal QoS, delayed amount because of execute ti before this tran item
    public double getEstQosLoss(TranItem ti){
        return getEstQos() - qc.getQos(sys.gClock + remainExeTime + ti.remainExeTime);
    }

    public double getQosMax(){
        return qc.getQosMax();
    }
    public double getQodMax(){
        return qc.getQodMax();
    }
    public double getQos(){
        if(hasStarted()) //handle the case if transaction is rejected
            return qc.getQos(finishTime - arrTime);
        else
            return 0.0;
    }
    public double getQod(){
        if(hasStarted() && getQos()>0) //handle the case if transaction is rejected
            return qc.getQod(staleness);
        else
            return 0.0;
    }
    public double getQ(){
        if(hasStarted()) //handle the case if transaction is rejected
            return getQos() + getQod();
        else
            return 0.0;
    }
    public double getQMax(int time){
        return qc.getQMax(time - arrTime);
    }
    public double getQosFeasible(){
        return qc.getQosFeasible(estExeTime);
    }
    public int getResponseTime(){
        return finishTime - arrTime;
    }
    public double getWaitingTime(){
        return finishTime - arrTime - estExeTime;
    }
//   * used in Controller_USM before integration, no longer used in priority is put in readyQ
    public boolean higherP_twoPriorityQ(TranItem ti){
        if( this.isUpdate() )
            if( ti.isUpdate() )
                return this.deadline < ti.deadline;
            else if( ti.isAccess() )
                return true;
            else
                Config.Error_TranType("TranItem.higherP_twoPriorityQ()");
        else if( this.isAccess() )
            if( ti.isUpdate() )
                return false;
            else if( ti.isAccess() )
                return this.deadline < ti.deadline;
            else
                Config.Error_TranType("TranItem.higherP_twoPriorityQ()");
        else Config.Error_TranType("TranItem.higherP_twoPriorityQ()");
        return false;
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

    /* -------------------------------------------------------------------------
     * Check to compare priorities between (this, o)
     * if (this.deadline - ti.deadline > 0)
     */
    public int compareTo(Object o){
        TranItem ti = (TranItem) o;
        return diffToInt(this.arrTime - ti.arrTime);
    }


    public static void main(String[] args){
            TranItem tran =  new TranItem();
            System.out.println("testing diffToInt----------------using 0.3 and -0.3");
            System.out.println(tran.diffToInt(0.3) + "\t" + tran.diffToInt(0) + "\t" + tran.diffToInt(-0.3));
    }


}
