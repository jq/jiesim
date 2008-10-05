package hm;


/*
 * Controller_USM.java
 *
 * Created on 28 September 2005, 00:46
 *
 * @author Huiming Qu
 */
import java.util.Random;
import java.util.Iterator;

public class Controller_USM extends Controller{
    public Monitor monitor;
    public Data data;

    public Random random;
    public double lag_percent;
    public double prevSR;
    public double prevFresh;

    public double sample_totalOnTime;// sample is reset every sampling
    public double sample_totalSubmit;
    public double sample_totalFreshOnTime;// sample is reset every sampling
    public double sample_busyTime;
//        successRate = totalOnTime/totalSubmit;
//        freshness = totalFreshOnTime/totalOnTime;

    public final int R = 0;
    public final int FM = 1;
    public final int FS = 2;
    public final int SACRIFICE_USER = 0;
    public final int SACRIFICE_UPDATE = 1;
    public final int SACRIFICE_BOTH = 2;
    public final int RECOVER = 3;

    public static int sampling_period;
    public static double FORGETTING_FACTOR = 0.8;
    public static double K_DEGRADE = 2;
    public static int DEGRADE_BOUND = 100;
    public final double DROP_THRESHOLD = 0.01;
    public final double TIGHTEN_ADMIT_STEP = 0.2;
    public final double LOOSE_ADMIT_STEP = 0.1;
    public static int P_RECOVER_STEP = 4;
    public final int NUM_ONDEMAND_PER_ROUND = 10;
    public boolean WEIGHTED_TICKETS = false;

    //-----------UNIT added----------------
    public double[] update_ondemand_prob; // probability of its update to be on-demand, value range [0,1], size is # of data item

    //-------------------------------------

    public Controller_USM() {
        random = new Random(System.currentTimeMillis());
        sampling_period = 100;
        lag_percent = -1; // -1 takes no consideration of preveious transaction, only to see it this one can finish
        prevSR = 1.0;
        prevFresh = 1.0;

        sample_busyTime = 1.0;
    }

    public void LoadBalancing(){
        if(sys.gClock % sampling_period == 0){
            if(Config.cost_reject==0.0 && Config.cost_fmiss==0.0 && Config.cost_fstale==0.0)
                USM_naive();
            else
                USM_weighted();
            sampleReset();
        }
    }

    public void link(Monitor mon, Data d){
        monitor = mon;
        data = d;
        update_ondemand_prob = new double[data.size()];
        for(int i=0; i<data.size();i++){
            update_ondemand_prob[i] = 0.0; // initialize to be immediate update (1.0 is on demand update)
        }
    }

    /* onDemand should come with a % of onDemand
     */
//    public boolean onDemandUpdate(TranItem ti, int index){
//        double freshness = data.bData[index].getFreshness(); //check if this data's freshness meets quality contract of that tran
//        if ( !ti.dataFreshnessCheck(freshness) && data.bData[index].isOnDemand() )
//            return true;
//        else
//            return false;
//    }


    public boolean admissionCtrl(TranItem ti, ReadyQueue readyQ){
        double rc = Config.cost_reject;
        double fc = Config.cost_fmiss;
        double fsc = Config.cost_fstale;
        int earliest_time = 0;
	int eat = 0; // transaction has to be executed before this time at earliest

        // reject those update on data-on-demand, and admit all others.
        if(ti.isUpdate()){
            int index = ti.getDataIndex(0);
            if ( data.bData[index].isOnDemand() )
                return false;
            else
                return true;
        }
        // if reject cost is bigger than fail, we do not reject
        // important change, we have to reject something unpromising and harmful even though rejection cost is higher
	if(rc > fc && fc!=0 && rc !=0) {
            return true;
        }
	if(readyQ.isEmpty()){
            return true;
        }


        Iterator<TranItem> tran_iterator = readyQ.accessQ.iterator();
        TranItem tran;
        int qsize = readyQ.accessQ.size(); //must have it set, because it might be changing
        int dangerT = 0;
        eat = sys.gClock + ti.estExeTime; //initialize to avoid eat=0 when no tran in readyQ
        for(int i=0; i<qsize; i++){
            tran = tran_iterator.next();
            // include all tran with higher priority, to see if it has chance to finish, stop if found it will miss deadline
            if(!ti.higherP_twoPriorityQ(tran)){
                earliest_time += tran.estExeTime;
                eat = sys.gClock + (int)(earliest_time*(1+lag_percent)) + ti.estExeTime;
                if( eat > ti.deadline ) {return false;}
            }
        }
        tran_iterator = readyQ.accessQ.iterator();
        for(int i=0; i<qsize; i++){
            tran = tran_iterator.next();
            if(ti.higherP_twoPriorityQ(tran)){
                // see if by allowing this transaction, how much cost need to be paid.
                if( eat + tran.estExeTime > tran.deadline) //loose check, take all pushed-back transactions equally.
                    dangerT ++;
            }
        }
        // rejct t if fail cost is higher
        if (fc == 0 && rc == 0 && dangerT > 1) return false;
	if (dangerT * fc > 1 * rc) return false;

        return true;
    }


    public void USM_weighted(){
        // find the max of the three and try to reduce it.
	switch(maxCost())
	{
	  case R:
	    increaseAccessAllowed();
	    break;

	  case FM:
	    if (Config.cost_reject < Config.cost_fmiss)
                reduceAccessAllowed();
	    if (Config.cost_fstale < Config.cost_fmiss)
	    	reduceUpdateAllowed();
	    else
	    	onDemandUpd();
	    break;

	  case FS:
	    increaseUpdateAllowed();
	    break;

	  default:
	    System.out.println("Controller_USM.USM_weighted:: Wrong cost attempt to reduce!\n");
	    System.exit(0);
	}
    }

    public void sampleReset(){
        sample_totalOnTime = 0.0;
        sample_totalSubmit = 0.0;
        sample_totalFreshOnTime = 0.0;

        sample_busyTime = 0;
    }
    public void increaseFreshOnTime(){
        sample_totalFreshOnTime += 1.0;
    }
    public void increaseSubmit(){
        sample_totalSubmit += 1.0;
    }
    public void increaseOnTime(){
        sample_totalOnTime += 1.0;
    }
    public void increaseBusyTime(){
        sample_busyTime += 1.0;
    }
    public double getUtil(){
        return sample_busyTime/sampling_period;
    }
    public double getSR(){
        if(sample_totalSubmit == 0)
            return 0;
        else
            return sample_totalOnTime/sample_totalSubmit;
    }
    public double getFreshness(){
        if(sample_totalOnTime == 0)
            return 0;
        else
            return sample_totalFreshOnTime/sample_totalOnTime;
    }
    public void USM_naive(){
//        successRate = totalOnTime/totalSubmit;
//        freshness = totalFreshOnTime/totalOnTime;
        double SR = getSR();
	double freshness = getFreshness();
	int loser;
	int ctrl_decision = ctrlSig(SR, freshness);
	double r;
        int count;

	switch(ctrl_decision){
            case SACRIFICE_UPDATE:
		r=1-SR;
		count = 0;
		while (r>0 ){
		    loser = degradeLottery();

		    if( count < (NUM_ONDEMAND_PER_ROUND) ){
		     	data.bData[loser].doOnDemand();
		    }
		    else{
		    	data.bData[loser].p_update.increasePeriod();
		    }
		    r -= 0.01;
		    count++;
		}
		break;

            case SACRIFICE_USER:
		r = 1-freshness;
		while(r>0){
			reduceAccessAllowed();
			r -= 0.01;
		}
                break;

            case SACRIFICE_BOTH:
		r=1-SR;
		count = 0;
		while (r>0 ){
		    loser = degradeLottery();
		    if( count < (NUM_ONDEMAND_PER_ROUND) ){
		     	data.bData[loser].doOnDemand();
		    }
		    else{
		    	data.bData[loser].p_update.increasePeriod();
		    }
		    r -= 0.01;
		    count++;
		}
		reduceAccessAllowed();
                break;

            case RECOVER:
                increaseAccessAllowed();
                int nUpgrade = increaseUpdateAllowed();
                break;

             default:
                return;
        }
        sampleReset();
    }

    public int ctrlSig(double measureSR, double measureFresh){
        double deltaSR = prevSR - measureSR;
        double deltaFresh = prevFresh - measureFresh;
        int decision = RECOVER;

        if(overload()){
            if ( deltaSR > DROP_THRESHOLD && deltaFresh > DROP_THRESHOLD)
                decision =  SACRIFICE_BOTH;
            else if(measureSR >= measureFresh)
                decision =  SACRIFICE_UPDATE;
            else
                decision =  SACRIFICE_USER;
        }
//        System.out.println("decision = " + decision
//                + "\tprevSR, measureSR, deltaSR: " + prevSR + ", " + measureSR +", "+ deltaSR
//                + "\tprevFresh, measureFresh, deltaFresh: " + prevFresh +", "+ measureFresh+", "+ deltaFresh
//                );
        prevSR = measureSR;
        prevFresh = measureFresh;
        return decision;
    }

    public boolean overload(){
        return getUtil()>=1.0;
    }
    public int maxCost(){
	double costR, costFM, costFS;
	double weightR, weightFM, weightFS;

	weightR = Config.cost_reject;
	weightFM = Config.cost_fmiss;
	weightFS = Config.cost_fstale;

	// use the final USM calculation, not the signal calculation every samplingperiod.
	// allow enough time so that all the transactions finish.
	costR = weightR * monitor.ratio_reject();
	costFM = weightFM * monitor.ratio_fmiss();
	costFS = weightFS * monitor.ratio_fstale();

//	//if both weight and cost are highest, return it. checking order R, FM, FS.
//	if (costR>=costFM && costR>=costFS) return R;
//	if (costFM>=costR && costFM>=costFS) return FM;
//	if (costFS>=costR && costFS>=costFM) return FS;

        //if both weight and cost are highest, return it. checking order R, FM, FS.
	if (weightR>=weightFM && weightR>=weightFS) return R;
	if (weightFM>=weightR && weightFM>=weightFS) return FM;
	return FS;

    }

    public int increaseUpdateAllowed(){
	int round_past = 0;
        int nUpgrade = 0;
	for (int i=0; i<data.num_data; i++){
            if(data.bData[i].degraded()){
                if(data.bData[i].p_update.original_period != 0){
                    round_past = (sys.gClock - data.bData[i].p_update.last_degrade_timestamp) / data.bData[i].p_update.original_period;
		}
		if(round_past >= P_RECOVER_STEP){
                    data.bData[i].p_update.period = data.bData[i].p_update.original_period;
                    nUpgrade ++;
		}
            }
	}
        return nUpgrade;
    }


    public void reduceUpdateAllowed(){
	int loser;
	int count = 0;
	while (count<10 )
	{
	    loser = degradeLottery();
	    data.bData[loser].p_update.increasePeriod();
	    count++;
	}

    }
    //WEIGHTED_TICKETS
    public double getTicketSum(){
        double sum = 0.0;
        for(int i=0; i<data.num_data; i++){
            if(WEIGHTED_TICKETS)
                sum += data.bData[i].nWeightedHistoryTicket;
            else
                sum += data.bData[i].nHistoryTicket;
        }
        return sum;
    }
    public double getTicketMin(){
        double min = 0.0, cur = 0.0;
        if(data.num_data > 0)
            if(WEIGHTED_TICKETS)
                min = data.bData[0].nWeightedHistoryTicket;
            else
                min = data.bData[0].nHistoryTicket;
        for(int i=1; i<data.num_data; i++){
            if(WEIGHTED_TICKETS)
                cur = data.bData[i].nWeightedHistoryTicket;
            else
                cur = data.bData[i].nHistoryTicket;
            if(cur < min)
                min = cur;
        }
        return min;
    }
    public int degradeLottery(){
        int loser = 0;
        int i, cnt = 0;

        int round = 0;
        double picked = 0, sum = 0;
        double ticket_min = getTicketMin();
        double shifted_ticket_sum = getTicketSum() - ticket_min * data.num_data;
        double shifted_ticket;

	//Huiming picking by lottery scheme, plain count of access and updates

	picked = random.nextDouble()*shifted_ticket_sum;

	  // find the owner of the picked ticket
	  for(loser = 0; loser<data.num_data; loser++)
	  {
	  	// using data size
                if(WEIGHTED_TICKETS)
                    shifted_ticket = data.bData[loser].nWeightedHistoryTicket - ticket_min;
                else
                    shifted_ticket = data.bData[loser].nHistoryTicket - ticket_min;
	  	sum += shifted_ticket;

	  	// using merely update exe time which is ~ data size
	  	// sum+=workload[i]->avgExeTime;
	  	if(sum>picked)
	  	  break;
	  }
	  return loser;
    }

    public void onDemandUpd()
    {
	int loser;
	int count = 0;
	while (count<10 )
	{
	    loser = degradeLottery();
	    data.bData[loser].doOnDemand();
	    count++;
	}
    }

    public int increaseAccessAllowed()
    {   // eat = sys.gClock + (int)(earliest_time*(1+lag_percent)) + ti.estExeTime;
        // originally lag_percent = -1(smallest); the larger it is, the more strict it is
	// admission control recovering
	//	set the threshold higher
        int increased = 1;
	lag_percent -= LOOSE_ADMIT_STEP;
	if (lag_percent < -1) {
            increased = 0;
            lag_percent = -1;
        }
        return increased; // returning if lag_percent is changed.
    }

    public void reduceAccessAllowed()
    {   // eat = sys.gClock + (int)(earliest_time*(1+lag_percent)) + ti.estExeTime;
        // originally lag_percent = -1(smallest); the larger it is, the more strict it is
	lag_percent += TIGHTEN_ADMIT_STEP;
    }

}
