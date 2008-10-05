package hm;

/*
 * Monitor.java
 *
 * Created on 27 September 2005, 10:50
 *
 * monitoring system statistics.
 */

/**
 * @author Huiming Qu
 */
import java.io.*;
import java.util.LinkedList;
import java.util.Vector;
import java.text.DecimalFormat;

public class Monitor {
    Tran trans;
    Data data;

    public static DataAccess da;

    public static int update_running_workload;
    public static int update_require_workload;
    public static int access_running_workload;
    public static int access_require_workload;

    public static double sum_qos;
    public static double sum_qos_expected;
    public static double sum_qod;
    public static double sum_qod_expected;
    public static double sum_qos_feasible;
    public static double sum_qod_feasible;
    public static AccessPriority ap;

    double avgWaitingTime;
    double avgResponseTime;
    double avgFreshness;
    int maxWaitingTime;

    int[] sum_ver_length;
    int[] max_ver_length;
    int ver_length_alltimedata_2orGreater; // for each time unit, each data, if version > 2, ++

    public static int samplingPeriod;
    public static double sample_gainedQos;
    public static double sample_expectedQos;
    public static double sample_gainedQod;
    public static double sample_expectedQod;

    public int num_meetdeadline;

    public static boolean print_ontime_qosqod = false;

    public static boolean print_on_samplingPeriod = true;
    public static boolean print_versioninfo = false;
    public static boolean print_on_tick = false;
    public static Vector<String> ontime_info = new Vector<String>();
    public static Vector<String> atomTimeState_onEachTimeStick = new Vector<String>();


    /*************************** function begin **********************************/
    public Monitor(Tran t, Data d) {
        trans = t;
        data = d;
        ap = new AccessPriority(d.num_data);
        da = new DataAccess(data);

        //initialization
        sum_qos = 0.0;
        sum_qos_expected = 0.0;
        sum_qos_feasible = 0.0;
        sum_qod = 0.0;
        sum_qod_expected = 0.0;
        sum_qod_feasible = 0.0;

        avgWaitingTime = 0.0;
        avgResponseTime = 0.0;
        maxWaitingTime = 0;

        sum_ver_length = new int[Config.MAX_NUM_BASE_DATA];
        max_ver_length = new int[Config.MAX_NUM_BASE_DATA];
        ver_length_alltimedata_2orGreater = 0;

        num_meetdeadline = 0;

        sample_gainedQos = 0.0;
        sample_gainedQod = 0.0;
        sample_expectedQos = 0.0;
        sample_expectedQod = 0.0;

    }

    public static double get_money_submitted_on(int index){
        return da.get_money_submitted_on(index);
    }
    public static double get_money_over_time(int index){
        return da.get_money_over_time(index);
    }

    // called in Tran.tran_submit()
    public static void on_tran_submit(TranItem ti){
        if(ti.isAccess()){
            sample_increaseMaxExpectedQosQod(ti);
            da.increase_money_submitted(ti);
            da.increase_money_submitted_histo(ti);
        }else
            da.increase_time_spent(ti);

        increase_require_workload(ti);

    }
    // called in Tran.tran_terminate()
    public static void on_tran_terminate(TranItem ti){
        if(ti.isAccess()){
            sample_increaseGainedQoSQod(ti);
            da.decrease_money_submitted(ti);
        }//else
            //da.increase_time_spent(ti);
    }



    // called in Tran.tran_terminate() for query only
    public static void sample_increaseGainedQoSQod(TranItem ti){
        sample_gainedQos += ti.getQos();
        sample_gainedQod += ti.getQod();
        sum_qos += ti.getQos();
        sum_qod += ti.getQod();
//        System.out.println("++++++++++ -> tran finish " + ti.arrTime + " qos = " + sum_qos
//                );
        if(sum_qos > sum_qos_expected){
            System.out.println("---QoS Overflow: "+sys.gClock);
        }
        ap.decreaseAccessPriority(ti);
    }
    // called in Tran.tran_submit() for query only
    public static void sample_increaseMaxExpectedQosQod(TranItem ti){
        sample_expectedQos += ti.getQosMax();
        sample_expectedQod += ti.getQodMax();
        sum_qos_expected += ti.getQosMax();
        sum_qod_expected += ti.getQodMax();
        sum_qos_feasible += ti.getQosFeasible();//(ti.getQMax(ti.estExeTime) - ti.getQodMax());
//        System.out.println("OOOOOOOOOOO -> tran arrive " + ti.arrTime + " qos_feasible = " + sum_qos_feasible
//                + " added by " + ti.getQosFeasible()
//                + " qos_expected " + sum_qos_expected + " added by " + ti.getQosMax()
//                );
        sum_qod_feasible += ti.getQodMax();

        if(sum_qos > sum_qos_expected){
            //System.out.println("QoS Overflow: "+sys.gClock);
        }
        ap.increaseAccessPriority(ti);
    }
    public static double getPriorityOnData_qod(int index){
        return ap.getPriority_qod(index);
    }
    public static double getPriorityOnData_qod_uu(int index){
        return ap.getPriority_qod_uu(index);
    }

    /* -------------------------------------------------------------------------
     * expected QoS in the sampling period =
     *      left_readyQ_in_last_period + new_arrival_in_this_period
     */
    public static double sample_getGainedQosPercent(){
        double qosPercent;
        if(sample_expectedQos == 0)
            qosPercent = 1.0;
        else
            qosPercent = sample_gainedQos / sample_expectedQos;
        return qosPercent;
    }

    public static double sample_getGainedQodPercent(){
        double qodPercent;
        if(sample_expectedQod == 0)
            qodPercent = 1.0;
        else
            qodPercent = sample_gainedQod / sample_expectedQod;
        return qodPercent;
    }

    public static double sample_getLostQod(){
        return sample_expectedQod - sample_gainedQod;
    }
    public static double sample_getLostQos(){
        return sample_expectedQos - sample_gainedQos;
    }
    public static double sample_expectedQ(){
        return sample_expectedQod + sample_expectedQos;
    }
    public static double sample_expectedQod(){
        return sample_expectedQod;
    }
    public static double sample_expectedQos(){
        return sample_expectedQos;
    }
    public static void sampling_reset(){
        sample_gainedQos = 0.0;
        sample_gainedQod = 0.0;
    }

    /*********************** statistic report begin ***************************/

    public void detailStat(){
        System.out.println("--------detail statistics------------");
        qos();
        System.out.println(statUpdate() + statAccess()
                        //+ statWorkload()
                        + statQosqod()
                        //+ statVersionLength()
                        );

        System.out.println("SUCCESS RATE: " + ratio_meetdeadline_total());
        finalStat();
        printOntimeStat();// details info will be printed with -onSp

        //detailVersionInfo();


    }

    public void finalStat(){
        String qu = "qos, qod, q:\t" + ratio_qos_max()
            + "\t" + ratio_qod_max() + "\t" + ratio_q_max()
            + "\nFEASIBLE:\t" + ratio_qos_feasible()
            + "\t" + ratio_qod_feasible() + "\t" + ratio_q_feasible()
            + "\n" + tradtionalStat() ;

        System.out.println("--------Final Stat---------");
        System.out.println(qu+"\n");
    }

    public String statQosqod(){
        String qos_stat = new String("QoS: ");
        qos_stat += "qos_gained: " + sum_qos
                + " qos_max: " + sum_qos_expected
                + " qos_feasible: " + sum_qos_feasible
                + " qod_gained: " + sum_qod
                + " qod_max: " + sum_qod_expected
                + " qod_feasible: " + sum_qod_feasible
                + "\n";
        return qos_stat;
    }
    public String tradtionalStat(){
        double max_response = 0.0, min_response = 0.0;
        double min_freshness = 0.0, max_freshness = 0.0;
        double avg_response = 0.0, sum_response = 0.0;
        double avg_freshness = 0.0, sum_freshness = 0.0;
        double avg_staleness = 0.0, sum_staleness = 0.0;
        double response = 0.0, freshness = 0.0, staleness = 0.0;
        int num = 0, num_stale_tran = 0;
        double perc_stale_tran = 0.0;

        TranItem ti = null;
        for(int i=0; i<trans.query_terminateQ.size(); i++){
            ti = trans.query_terminateQ.get(i);
            response = ti.finishTime - ti.arrTime;
            freshness = ti.freshness;
            staleness = ti.staleness;
            //respones time stat
            if (response <= 0){
                System.out.println("Response time is less than zero!");
                return "";
            }
            if(response > max_response)
                max_response = response;
            if(response < min_response)
                min_response = response;
            sum_response += response;
            num++;
            //freshness stat
            sum_freshness += freshness;
            sum_staleness += staleness;
            if (staleness > 0)
                num_stale_tran ++;
        }
        avg_response = sum_response / num;
        avg_freshness = sum_freshness / num;
        avg_staleness = sum_staleness / num;
        perc_stale_tran = (double)num_stale_tran / num;

        String stat = "Response: avg = " + avg_response
                + "\t Freshness: avg = " + avg_freshness
                + "\t Staleness: avg = " + avg_staleness
               //+ "\t percentage_of_stale_tran = " + perc_stale_tran
               ;

        String f_stat = "avg response, staleness, freshness:\t " + avg_response + "\t" + avg_staleness + "\t" + avg_freshness;

        return f_stat;
    }

    public static void ontimeStat(){
        if(print_ontime_qosqod){
            String ontime = sys.gClock + " qos/qod/q: ";
            ontime += ratio_qos_max() + "\t" + ratio_qod_max() + "\t" + ratio_q_max();
            System.out.println(ontime);
        }
    }
    public static void AddOntimeStat(String info, double newQosEx, double newQodEx, double ex){
        //"qChance  uChance  qosLoss  qodLoss  expectedQ  newlyExpectedQ  qGain qosGain  qodGain  qos  qod  q qOverExQ qOverNewExQ "
        if(print_on_samplingPeriod){
            double newEx = newQosEx + newQodEx;
            double period_gain = sample_gainedQos + sample_gainedQod;
            double qOverExQ = period_gain/newEx;
            double qOverNewExQ = period_gain/ex;
            info +=  // qChance  uChance  qosLoss  qodLoss  expectedQ  newlyExpectedQ
            	"\t" + Config.digit1.format(sample_expectedQos) + "\t" + Config.digit1.format(sample_expectedQod) + //qosEx qodEx
                "\t" + Config.digit1.format(period_gain) + "\t" + Config.digit1.format(sample_gainedQos) + "\t" + Config.digit1.format(sample_gainedQod) + //qGain qosGain qodGain
                "\t" + Config.digit1.format(ratio_qos_max()) + "\t" + Config.digit1.format(ratio_qod_max()) + "\t" + Config.digit1.format(ratio_q_max()) + //qos  qod  q
                "\t" + Config.digit1.format(qOverExQ) + "\t" + Config.digit1.format(qOverNewExQ); //qOverExQ qOverNewExQ
            ontime_info.add(info);
        }
    }
    public static void printOntimeStat(){
        if (print_on_samplingPeriod){
            System.out.println("On Every Sampling Time output ------------");
            System.out.println("qChance  uChance  qosLoss  qodLoss  expectedQ  newlyExpectedQ  newQosEx	newQodEx qosEx qodEx qGain  qosGain  qodGain  qos  qod  q  qOverExQ qOverNewExQ");
            for(int i=0; i<ontime_info.size(); i++){
                System.out.println(ontime_info.get(i));
            }
        }
        //System.out.println();
        if (print_on_tick){
            System.out.println("Detail (every time tick) Atom Time Status Info------------");
            for(int i=0; i<atomTimeState_onEachTimeStick.size(); i++){
                System.out.println(atomTimeState_onEachTimeStick.get(i));
            }
        }
    }
    public void detailVersionInfo(){
        String max_printout = "";
        double max_max = 0;
        double ver_sum = 0;
        for(int i=0; i<Config.MAX_NUM_BASE_DATA; i++){
            if(max_ver_length[i] > 1){
                ver_sum = sum_ver_length[i] - Config.MAX_TIME;
                max_printout += "data_" + i + "\t" + max_ver_length[i] + "\t" + ver_sum + "\n";
            }
            if (max_max < max_ver_length[i]){
                max_max = max_ver_length[i];
            }
        }
        System.out.println(max_printout);
    }

    public String statVersionLength(){
        // sum_ver_length is the sum of version # of each data for all time ticks
        // max_ver_length is the max of version # of each data for all time ticks
        double avg_sum = 0.0;
        double max_sum = 0.0;
        for(int i=0; i<Config.MAX_NUM_BASE_DATA; i++){
            avg_sum += (double)sum_ver_length[i]/Config.MAX_TIME;
            max_sum += max_ver_length[i];
        }
        double avg_avg = avg_sum/Config.MAX_NUM_BASE_DATA;
        double max_avg = max_sum/Config.MAX_NUM_BASE_DATA;

        String printout = "Data Avg Versions Length: avgerage is " + avg_avg
                + ", avg of max is " + max_avg
                + "  alltimedata_2orGreater is " +  ver_length_alltimedata_2orGreater
                + "\n";
        return printout;
    }

    public String statUpdate(){
        String update_stat = new String("UPDATE: ");
        update_stat += " submitted: " + num_update_submit() + " commit: " + num_update_commit() + "\n";
        return update_stat;
    }

    public String statAccess(){
        //DecimalFormat formatter = new DecimalFormat(".00");
        String access_stat = new String("ACCESS: ");
        access_stat += "commit: " + num_freshCommit() //because freshness bar is 0.0
                        + " terminated: " + num_terminate()
                        + " terminateQ: " + trans.terminateQ.size()
                        + " submitted: " + num_submit() + " unfinished: " + num_unfinished()
                        + " avg_freshness: " + avgFreshness//formatter.format(avgFreshness)
                        + "\n";
        return access_stat;
    }

    public String statWorkload(){
        String workload = new String("WORKLOAD: ");
        workload += "reuqire workload = " + "update_" + percent_update_require_workload() + "%\t"
                    + "access_" + percent_access_require_workload() + "%\t"
                    + "running workload = " + "update_" + percent_update_running_workload() + "%\t"
                    + "access_" + percent_access_running_workload() + "%\t"
                    + "\n";
        return workload;
    }

    public void statTran(){
        System.out.println("-----all terminated trans --- Total: " + trans.terminateQ.size() + " --- MAX_TIME : " + Config.MAX_TIME + "---");
        for(int i=0; i<trans.terminateQ.size(); i++){
            System.out.println(trans.terminateQ.get(i));
        }
        System.out.println("-----Status of other queues, rejectQ, accessQ, updateQ, readyQ, listed if empty ----- \n");
        if (trans.rejectQ.size() > 0)
            trans.listRejectQ();
        if (trans.accessQ.size() > 0)
            trans.listAccessQ();
        if (trans.updateQ.size() > 0)
            trans.listUpdateQ();
        if (trans.readyQ.size() > 0)
            trans.readyQ.list();
    }

    public void collectEveryTimeTick(){
        int tmp;
        for(int i=0; i<Config.MAX_NUM_BASE_DATA; i++){
            tmp = data.bData[i].num_versions();
            //System.out.print(tmp+"\t");
            sum_ver_length[i] += tmp;
            if( tmp > max_ver_length[i] )
                max_ver_length[i] = tmp;
            if( tmp >= 2 )
                 ver_length_alltimedata_2orGreater ++;
        }

        //atom time status
        if(trans.readyQ.runningQuery())
            atomTimeState_onEachTimeStick.add("1");
        else
            atomTimeState_onEachTimeStick.add("0");
    }

    public int percent_update_require_workload(){
        return (int)((update_require_workload/(double)Config.MAX_TIME) * 100);
    }
    public int percent_update_running_workload(){
        return (int)((update_running_workload/(double)Config.MAX_TIME) * 100);
    }
    public int percent_access_require_workload(){
        return (int)((access_require_workload/(double)Config.MAX_TIME) * 100);
    }
    public int percent_access_running_workload(){
        return (int)((access_running_workload/(double)Config.MAX_TIME) * 100);
    }

    public int num_access(){
        return trans.accessQ.size();
    }
    public int num_submit(){
        //only counting access
//        int submit = trans.submitQ.size() +  trans.readyQ.accessQ.size() + trans.blockQ.size() + num_terminate();
//        if (trans.currentTran != null)
//            if(trans.currentTran.isAccess())
//                submit ++;
//        return submit;
        return trans.num_access_submit;
    }
    public int num_unfinished(){
        int in = trans.submitQ.size() +  trans.readyQ.accessQ.size();// + trans.blockQ.size();
        if (trans.currentTran != null)
            if(trans.currentTran.isAccess())
                in ++;
        return in;
    }
    public int num_terminate(){
        int total = trans.rejectQ.size() + trans.query_fresh_commitQ.size() + trans.query_stale_commitQ.size() + trans.abortQ.size();
        return total;
    }
    public int num_commit(){
        return trans.query_fresh_commitQ.size() + trans.query_stale_commitQ.size();
    }
    public int num_freshCommit(){
        return trans.query_fresh_commitQ.size();
    }
    public int num_staleCommit(){
        return trans.query_stale_commitQ.size();
    }
    public int num_reject(){
        return trans.rejectQ.size();
    }

    public int num_fmiss(){
        return trans.abortQ.size();
    }

    public int num_fstale(){
        return trans.query_stale_commitQ.size();
    }

    public double ratio_reject(){
        if(num_terminate()!=0)
            return num_reject()/(double)num_terminate();
        else
            return 0;
    }

    public double ratio_fmiss(){
        if(num_terminate()!=0)
            return num_fmiss()/(double)num_terminate();
        else
            return 0;
    }

    public double ratio_fstale(){
        if(num_terminate()!=0)
            return num_fstale()/(double)num_terminate();
        else
            return 0;
    }

    public double ratio_commit_total(){
        if(num_submit()!=0)
            return num_commit()/(double)num_submit();
        else
            return 0;
    }

    public double ratio_meetdeadline_total(){
        if(num_submit()!=0)
            return num_meetdeadline/(double)num_submit();
        else
            return 0;
    }

    public double ratio_fresh_commit(){
        if(num_commit()!=0)
            return num_freshCommit()/(double)num_commit();
        else
            return 0;
    }

    public double avg_freshness(){
        double freshness = 0.0;
        int fresh = trans.query_fresh_commitQ.size();
        int stale = trans.query_stale_commitQ.size();
        for(int i=0; i<fresh; i++){
            freshness += trans.query_fresh_commitQ.get(i).freshness;
        }
        for(int j=0; j<stale; j++){
            freshness += trans.query_stale_commitQ.get(j).freshness;
        }
        freshness = freshness/(fresh + stale);
        return freshness;
    }

    public static void increase_require_workload(TranItem ti){ //called in Tran.tran_submit
        if(ti.isAccess())
            access_require_workload += ti.estExeTime;
        else if(ti.isUpdate())
            update_require_workload += ti.estExeTime;
        else
            Config.Error_TranType("Monitor.increase_require_workload");
    }
    public static void increase_running_workload(TranItem ti){ //called in Tran.stepRun
        if(ti.isAccess())
            access_running_workload ++;
        else if(ti.isUpdate())
            update_running_workload ++;
        else
            Config.Error_TranType("Monitor.increase_running_workload");
    }

    public void qos(){
        double waitingT;
        double totalWaitingTime = 0.0;
        double totalResponseTime = 0.0;
        double totalFreshness = 0.0;
        int num_terminate = trans.query_terminateQ.size();
        int num_commit = trans.query_fresh_commitQ.size();

        TranItem ti;
//        for(int i=0; i<num_terminate; i++){
////            System.out.println(trans.query_terminateQ.get(i));
//            sum_qos += trans.query_terminateQ.get(i).getQos();
//            sum_qos_expected += trans.query_terminateQ.get(i).getQosMax();
//        }
        for(int j=0; j<num_commit; j++){
            ti = trans.query_fresh_commitQ.get(j);
            waitingT = ti.getWaitingTime();
            totalWaitingTime += waitingT;
            totalResponseTime += ti.getResponseTime();
            totalFreshness += ti.getFreshness();
            if(ti.isOnTime())
                num_meetdeadline ++;
            if (waitingT > maxWaitingTime)
                maxWaitingTime = (int)waitingT;
        }
        avgWaitingTime = totalWaitingTime / num_terminate;
        avgResponseTime = totalResponseTime / num_terminate;
        avgFreshness = totalFreshness / num_commit;
    }

    public static double ratio_qos_max(){
        if (sum_qos_expected == 0)
            return 0;
        else
            return sum_qos/sum_qos_expected;
    }
    public static double ratio_qod_max(){
        if (sum_qod_expected == 0)
            return 0;
        else
            return sum_qod/sum_qod_expected;
    }
    public static double ratio_q_max(){
        double total = sum_qos_expected + sum_qod_expected;
        if (total == 0)
            return 0;
        else
            return (sum_qod + sum_qos)/total;
    }
    public static double ratio_qos_feasible(){
        if (sum_qos_feasible == 0)
            return 0;
        else
            return sum_qos/sum_qos_feasible;
    }
    public static double ratio_qod_feasible(){
        if (sum_qod_feasible == 0)
            return 0;
        else
            return sum_qod/sum_qod_feasible;
    }
    public static double ratio_q_feasible(){
        double total = sum_qos_feasible + sum_qod_feasible;
        if (total == 0)
            return 0;
        else
            return (sum_qod + sum_qos)/total;
    }
    /*--------------------------------------------------------------------------
     *  UPDATE
     */
    public int num_update_reject(){
        return trans.update_rejectQ.size();
    }
    public int num_update_abort(){
        return trans.update_abortQ.size();
    }
    public int num_update_commit(){
        return trans.update_commitQ.size();
    }
    public int num_update_submit(){
        return trans.num_update_submit;
    }
    //    public void detailUpdateOnDemand(){
//        LinkedList<TranItem>[] onCall = new LinkedList[trans.num_access_submit];
//        for(int i=0; i<trans.num_access_submit; i++){
//            onCall[i] = new LinkedList();
//        }
//        TranItem update, access;
//        for(int i=0; i<trans.update_commitQ.size(); i++){
//            update = trans.update_commitQ.get(i);
//            access = update.onCalledBy;
//            if(access != null){
//                onCall[access.access_index].offer(update);
//                System.out.println(i + " - " + access.access_index + " - " + onCall[access.access_index].size());
//            }
//            else
//                System.out.println(i + " is not a on call update!");
//        }
//        System.out.println("ON-DEMAND-DETAILS: ");
//        for(int i=0; i<trans.num_access_submit; i++){
//            System.out.println(i + " - " + onCall[i].size()+"\t");
//        }
//    }

}


class AccessPriority{
    double[] pr;
    int num;

    double[] pr_qod;
    double[] pr_qod_uu;

    public AccessPriority(int num_data){
        num = num_data;
        pr = new double[num_data];
        pr_qod = new double[num_data];
        pr_qod_uu = new double[num_data];
    }

    public double getPriority_qod(int index){
        return pr_qod[index];
    }

    public double getPriority_qod_uu(int index){
        return pr_qod_uu[index];
    }

    public void increaseAccessPriority(TranItem ti){
        int index;
        if (ti != null)
            for(int i=0; i<ti.getDataSize(); i++){
                index = ti.getDataIndex(i);
                pr_qod[index] += ti.getQodMax();
                pr_qod_uu[index] += ti.getQodMax()/ti.qc.getUUMax();
            }
    }

    public void decreaseAccessPriority(TranItem ti){
        int index;
        if (ti != null)
            for(int i=0; i<ti.getDataSize(); i++){
                index = ti.getDataIndex(i);
                pr_qod[index] -= ti.getQodMax();
                pr_qod_uu[index] -= ti.getQodMax()/ti.qc.getUUMax();
            }
    }
}

class DataAccess{
        int num_data;
        double[] money_submitted;
        double[] money_submitted_histo;
        double[] time_spent_histo;
        double forgetting_factor = 0.9;

        public DataAccess(Data d){
            num_data = d.size();
            money_submitted = new double[num_data];
            money_submitted_histo = new double[num_data];
            time_spent_histo = new double[num_data];
        }
        /*
         * the update benifit vs cost.
         */
        public double get_money_over_time(int index){
            double value = 0.0;
            if(time_spent_histo[index] > 0){
                value = money_submitted_histo[index]/time_spent_histo[index];
            }
            return value;
        }
        /*
         * the update pressure in terms of money submitted
         */
        public double get_money_submitted_on(int index){
            return money_submitted[index];
        }
        public void increase_time_spent(TranItem ti){ // added when tran is finished.
            if(ti.isUpdate()){
                int index = ti.getDataIndex(0);
                time_spent_histo[index] = forgetting_factor * time_spent_histo[index] + ti.estExeTime;
            }else
                Config.ErrorAt("Monitor:increase_time_spent! -- need an update transaction!");
        }
        public void increase_money_submitted_histo(TranItem ti){
            int index;
            int dataset_size = ti.getDataSize();
            double money = 0.0;
            if(ti.isAccess()){
                for(int i=0; i<dataset_size; i++){
                    index = ti.getDataIndex(i);
                    money = ti.getQodMax();
                    //for histo, only increase, decrease is taken care by forgetting_factor
                    money_submitted_histo[index] = forgetting_factor * money_submitted_histo[index] + money;
                }
            }else{
                System.out.println("Error: A Query is required here! ");
                System.exit(0);
            }
        }

        public void increase_money_submitted(TranItem ti){
            int index;
            int dataset_size = ti.getDataSize();
            double money = 0.0;
            if(ti.isAccess()){
                for(int i=0; i<dataset_size; i++){
                    index = ti.getDataIndex(i);
                    money = ti.getQodMax();
                    money_submitted[index] += (double)money/dataset_size;
                }
            }else{
                System.out.println("Error: A Query is required here! ");
                System.exit(0);
            }
        }

        public void decrease_money_submitted(TranItem ti){
            int index;
            int size = ti.getDataSize();
            if(ti.isAccess()){
                for(int i=0; i<size; i++){
                    index = ti.getDataIndex(i);
                    money_submitted[index] -= (double)ti.getQodMax()/size;
                }
            }else{
                System.out.println("Error: A Query is required here! ");
                System.exit(0);
            }
        }


    }


