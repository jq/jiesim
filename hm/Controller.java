package hm;

/*
 * Controller.java
 *
 * Created on 27 September 2005, 18:12
 *
 * system controller to adjust the admission control on access and shedding on updates.
 */

/**
 * @author Huiming Qu
 */

import java.util.Iterator;

public class Controller{

    public Controller(){
    }

    public boolean admissionCtrl(TranItem ti, ReadyQueue readyQ){
        if (ti.arrTime + ti.estExeTime < ti.deadline)
            return true;
        else
            return false;
    }

    public void LoadBalancing(){
    }

    public void link(Monitor mon, Data d){
    }
}

class Controller_UNIT extends Controller{

    public Monitor monitor;
    public Data data;

    public double lag_percent = 1.0;
    public static int sampling_period = 1000;

    public Controller_UNIT(){
    }

    public void link(Monitor mon, Data d){
        monitor = mon;
        data = d;
    }

    public void LoadBalancing(){
        boolean yes = true;
        if(sys.gClock % sampling_period == 0){
            if(yes)
                reduceAccessAllowed();
            else
                increaseAccessAllowed();
        }
    }

    public int increaseAccessAllowed()
    {   // eat = sys.gClock + (int)(earliest_time*(1+lag_percent)) + ti.estExeTime;
        // originally lag_percent = -1(smallest); the larger it is, the more strict it is
	// admission control recovering
	//	set the threshold higher
        int increased = 1;
//	lag_percent -= LOOSE_ADMIT_STEP;
	if (lag_percent < -1) {
            increased = 0;
            lag_percent = -1;
        }
        return increased; // returning if lag_percent is changed.
    }

    public void reduceAccessAllowed()
    {   // eat = sys.gClock + (int)(earliest_time*(1+lag_percent)) + ti.estExeTime;
        // originally lag_percent = -1(smallest); the larger it is, the more strict it is
//	lag_percent += TIGHTEN_ADMIT_STEP;
    }

    public boolean admissionCtrl(TranItem ti, ReadyQueue readyQ){

        int previous_exeTime_sum = 0;
        int candidate_est_responseTime = ti.estExeTime;  //initialize as if no waiting time applies
        double candidate_est_profit = 0;

        // admit if system is idle
        if(readyQ.isEmpty()){
            return true;
        }

        // updates::: let scheduling do the job since no penalties on failing update itself
        if(ti.isUpdate()){
            return true;
        }

        // queries:::
        /* Two-step checking to see (1) if earning of ti > 0, then (2) earning of ti > loss of ti's followers
         * Heuristics
         *      ignores readyQ.updateQ, data concurrency uncertainties
         *      considering all ti's followers as immediate followers
         *      no rejection cost for now
         *
         * [Step 1] check if ti can finished before deadline and have positive profit
         * scan those transactions with higher/queal priorities
         * only accessQ is considered! be conservative then
         */
        Iterator<TranItem> tran_iterator = readyQ.accessQ.iterator();
        TranItem tran;
        int qsize = readyQ.accessQ.size(); //must have it set, because it might be changing
        int dangerT = 0;
        for(int i=0; i<qsize; i++){
            tran = tran_iterator.next();
            // wait time increases if tran has higher or equal priority than/as ti
            if(!readyQ.hasHigherPriority(ti, tran)){
                previous_exeTime_sum += tran.estExeTime;
                candidate_est_responseTime = (int)(previous_exeTime_sum*(1+lag_percent)) + ti.estExeTime; //wait time + execution time
                // stop if found it will miss deadline, which mean no gains and even penalty will apply
                if( candidate_est_responseTime > ti.relDeadline ) {
                    return false;
                }
            }
        }

        /* [Step 2] check if ti's positive profit can compensate the loss of other transactions (if any)
         * scan those transactions with lower priorities
         */
        candidate_est_profit = ti.qc.getQos(candidate_est_responseTime);
        int following_est_responseTime;
        double following_est_profit_loss = 0.0;

        tran_iterator = readyQ.accessQ.iterator();
        for(int i=0; i<qsize; i++){
            tran = tran_iterator.next();
            if(readyQ.hasHigherPriority(ti, tran)){
                // loose check, take all pushed-back transactions equally as if it is immediately after ti
                following_est_responseTime = candidate_est_responseTime + tran.estExeTime;
                // see if by allowing this transaction, what is the decrease of profit (before adding ti - after adding ti)
                following_est_profit_loss += tran.qc.getQos(following_est_responseTime - ti.estExeTime) - tran.qc.getQos(following_est_responseTime);
                // if loss increases to be greater than earning, reject
                if( candidate_est_profit < following_est_profit_loss)
                    return false;
            }
        }

        //otherwise, admit
        return true;

    }

}





