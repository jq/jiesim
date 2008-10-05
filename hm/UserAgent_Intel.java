package hm;

/*
 * UserAgent_Intel.java
 *
 * Created on 30 October 2005, 10:10
 *
 * UserAgent_Intel PaymentHistory
 *
 * @author Huiming Qu
 */

import java.util.LinkedList;
import java.util.PriorityQueue;

class UserAgent_Intel extends UserAgent{

    // historical data
    public LinkedList<PaymentHistory> history;
    public LinkedList<PaymentHistory> successQ;
    public LinkedList<PaymentHistory> failureQ;
    public PriorityQueue<QualityContract> candidate; //ordered by the total sum

    // parameters
    public int time_window;
    public double down_step;
    public double up_step;
    public double down_percent;


    public UserAgent_Intel(UserItem user){
        super(user);

        history = new LinkedList<PaymentHistory>();
        successQ = new LinkedList<PaymentHistory>();
        failureQ = new LinkedList<PaymentHistory>();


        // parameter setting
        time_window = Config.window;
        down_step = Config.down_step;
        up_step = Config.up_step;
        down_percent = Config.down_percent;
    }
    public String type(){
        return "INTEL";
    }
    public QualityContract newQC(TranItem ti){
        double bid = newBid();
        return createQCfromBid(ti, bid);
    }
    public void addHistory(TranItem tran){
        int num = history.size();
        num++;
        PaymentHistory ph = new PaymentHistory(num, tran);
        history.add(ph);

        double paid = tran.getQ();
        if(paid > 0){
            successQ.add(ph);
            clear_previous_failures(ph);
        }else{
            failureQ.add(ph);
        }
    }



    // get a new try out sum of max profit according to the history
    public double newBid(){
        double lastbid = currentQC.getQMax();
        double newbid;

        if( failureQ.size() > 0 ){
            newbid = increaseBid(lastbid);
        }else
            newbid = decreaseBid(lastbid);

        //System.out.println("============time: " + sys.gClock +" newbid: " + newbid +" lastQC: " + lastbid);

        return newbid;
    }

    /* take the most recent successful bid, sucBid
     * reduce from sucBid proportionally
     * down_step_range = 0.1
     * down_step = down_step_range * paid/maximum
     */
    public double decreaseBid(double lastbid){
        double newbid = lastbid * ( 1 - down_percent);
        if(newbid < 0)
            newbid = 0;

        return newbid;
    }

    /* in successQ
     * take median in range (current bid, max bid]
     * within time_window
     * if 0 in the range, go up with up_step
     */
    public double increaseBid(double lastbid){
        double bid = lastbid;
        PriorityQueue<Double> cand = new PriorityQueue<Double>();

        //create the candidate bid within time_window
        int earliest_time = 0;
        if(sys.gClock > time_window)
            earliest_time = sys.gClock - time_window;

        PaymentHistory ph = null;
        int size = successQ.size();
        LinkedList<PaymentHistory> removeset = new LinkedList<PaymentHistory>();
        for(int i=0; i<successQ.size(); i++){
            ph = successQ.get(i);
            if(ph.submitTime <earliest_time){
                removeset.offer(ph);
            }
            else if(ph.q_max > lastbid)
                cand.offer(ph.q_max);
        }

        //remove those paymenthistory in successQ with the index in removeset
        int removeindex = removeset.size();
        for(int j=0; j<removeindex; j++){
            successQ.remove(removeset.poll()); //adjust the successQ to only keep useful info
        }

        //find the median in candidate
        int index = (int)Math.ceil((double)cand.size()/2);
        if(index == 0) //current bid is the max historically
            bid = lastbid + up_step;
        else{
            while(index > 1){
                index --;
                cand.poll();
            }
            bid = cand.poll();
        }
        return bid;
    }


    //clera the previous failure history: failure.arrTime < newcoming.arrTime
    //failureQ.clear(); cannot be used because the delayed notification from system
    private void clear_previous_failures(PaymentHistory ph) {
    	LinkedList<PaymentHistory> tmp = new LinkedList<PaymentHistory>();
    	PaymentHistory after_ph;

    	while(!failureQ.isEmpty()) {
    		after_ph = failureQ.removeFirst();
    		if(after_ph.submitTime > ph.submitTime)//keep those that are submitted after ph
    			tmp.add(after_ph);
    	}
    	failureQ = tmp;
    }


}

class PaymentHistory implements Comparable{
    public int id;
    public int submitTime;
    public int receiveTime;
    public QualityContract qc;
    public double q_max;

    public int response_time;
    public double qos_paid;
    public int staleness;
    public double qod_paid;

    public PaymentHistory(int id, TranItem tran){
        this.id = id;
        submitTime = tran.arrTime;
        receiveTime = tran.finishTime;
        qc = tran.qc;
        q_max = qc.getQMax();

        response_time = tran.getResponseTime();
        qos_paid = tran.getQos();
        staleness = tran.getStaleness();
        qod_paid = tran.getQod();
    }

    //sorted on submit time
    public int compareTo(Object o) {
    	PaymentHistory ph = (PaymentHistory)o;
    	int diff = this.submitTime - ph.submitTime;
    	if (diff == 0)
    		return this.id - ph.id;
    	else
    		return diff;
    }

//    public boolean equals(Object o){
//        PaymentHistory ph = (PaymentHistory)o;
//        if(ph.id == this.id)
//            return true;
//        else
//            return false;
//    }

}