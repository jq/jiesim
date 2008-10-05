package hm;

/*
 * UserAgent_E.java
 *
 * Created on 1 December 2006, 11:17
 *
 * UserAgent_E (Expectation)
 *
 * @author Huiming Qu
 */

import java.util.LinkedList;
import java.util.PriorityQueue;

class UserAgent_E extends UserAgent{

    // historical data
    public LinkedList<PaymentHistory> history;

    // parameters
    public int time_window;

    public UserAgent_E(UserItem user){
        super(user);

        history = new LinkedList<PaymentHistory>();

        // parameter setting
        time_window = Config.window;

    }
    public String type(){
        return "EXPT";
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
    }

    // get a new try out sum of max profit according to the history
    public double newBid(){
        double newbid = 0.0;

        int earliest_time = 0;
        if(sys.gClock > time_window)
            earliest_time = sys.gClock - time_window;

        LinkedList<PaymentHistory> tmp = new LinkedList<PaymentHistory>();
    	PaymentHistory ph;

        int num_paid_qos = 0;
        int num_paid_qod = 0;
        double sum_rt = 0.0;
        while(!history.isEmpty()) {
            ph = history.removeFirst();
            if(ph.receiveTime >= earliest_time){
                tmp.add(ph); // keep it in the history
                if(ph.qos_paid > 0){
                    num_paid_qos ++;
                    sum_rt += ph.response_time;
                }
                if(ph.qod_paid > 0)
                    num_paid_qod ++;
            }
    	}
    	history = tmp;
        int num_tran = history.size();
        double future_avg = getFutureAvg();
        double s = 0.0;
        double d = 0.0;
        double a1 = 0.0, a2 = 0.0;
        double b1 = 0.0;
        double rt = initialQC.getRDMax();
        if(num_tran > 10){
            a1 = (double)num_paid_qos/num_tran;
            a2 = (double)sum_rt/(num_tran * rt);
            s = (a1 - a2) * qos_percent;
            b1 = (double)num_paid_qod/num_tran;
            d = b1 * (1 - qos_percent);
            if((s + d) > 0){
                newbid = future_avg / (s + d);
            }else{
                newbid = owner.getBudget();
            }
        }else{
            newbid = future_avg;
        }
//        System.out.println("E_: future avg="+future_avg + " newbid=" + newbid +" s=" + s + " d=" +d +
//                " #qos="+num_paid_qos+" sum_rt="+sum_rt+" #total="+num_tran+" rt="+rt+" a1="+a1+" a2="+a2);
        return newbid;
    }

    public double getFutureAvg(){
        double future_avg = 0;
        int num = owner.getNumUnSent() + 1; //plus the current sending query
        double leftmoney = owner.getBudget();
        if(num > 0)
            future_avg = leftmoney/num;
        else
            future_avg = leftmoney;

        return future_avg;
    }

}


class UserAgent_Intel_E extends UserAgent_Intel_pro{

    public UserAgent_Intel_E(UserItem user){
        super(user);
    }

    public String type(){
        return "Intel_EXPT";
    }

    public double newBid(){
        double lastbid = currentQC.getQMax();
        double newbid;

        if( failureQ.size() > 0 ){
            newbid = increaseBid(lastbid); //use expected future avg
        }else{
            newbid = decreaseBid(lastbid); //decrease because of succeed
            //System.out.println("Intel_EXPT: decrease");
        }
        //System.out.println("============time: " + sys.gClock +" newbid: " + newbid +" lastQC: " + lastbid);
        return newbid;
    }

    public double increaseBid(double lastbid){
        //return getFutureAvg()*factor;
        double newbid = 0.0;

        int earliest_time = 0;
        if(sys.gClock > time_window)
            earliest_time = sys.gClock - time_window;

        LinkedList<PaymentHistory> tmp = new LinkedList<PaymentHistory>();
    	PaymentHistory ph;

        int num_paid_qos = 0;
        int num_paid_qod = 0;
        double sum_rt = 0.0;
        while(!history.isEmpty()) {
            ph = history.removeFirst();
            if(ph.receiveTime >= earliest_time){
                tmp.add(ph); // keep it in the history
                if(ph.qos_paid > 0){
                    num_paid_qos ++;
                    sum_rt += ph.response_time;
                }
                if(ph.qod_paid > 0)
                    num_paid_qod ++;
            }
    	}
    	history = tmp;
        int num_tran = history.size();
        double future_avg = getFutureAvg()* Config.futureavg_factor; //default is 1
        double s = 0.0;
        double d = 0.0;
        double a1 = 0.0, a2 = 0.0;
        double b1 = 0.0;
        double rt = initialQC.getRDMax();
        if(num_tran > 10){
            a1 = (double)num_paid_qos/num_tran;
            a2 = (double)sum_rt/(num_tran * rt);
            s = (a1 - a2) * qos_percent;
            b1 = (double)num_paid_qod/num_tran;
            d = b1 * (1 - qos_percent);
            if((s + d) > 0){
                newbid = future_avg / (s + d);
            }else{
                newbid = future_avg;
            }
        }else{
            newbid = future_avg;
        }
//        System.out.println("IntelE_: future avg="+future_avg + " newbid=" + newbid +" s=" + s + " d=" +d +
//                " #qos="+num_paid_qos+" sum_rt="+sum_rt+" #total="+num_tran+" rt="+rt+" a1="+a1+" a2="+a2);
        return newbid;
    }

    public double decreaseBid(double lastbid){
        double newbid = decreaseBidbyWeightedPercent(lastbid);
        //System.out.println("PRO new bid=" + newbid);
        return newbid;
    }

    public double decreaseBidbyPercent(double lastbid){
        double newbid = lastbid * ( 1 - down_percent);
        if(newbid < 0)
            newbid = 0;
        return newbid;
    }

    public double decreaseBidbyWeightedPercent(double lastbid){
        double newbid = lastbid;
        double down_step_range = down_step;
        PaymentHistory ph = null;
        int size = successQ.size();
        LinkedList<PaymentHistory> removeset = new LinkedList<PaymentHistory>();

        //----find the most recent successful QC----------------------------
        int most_recent_time = 0;
        double most_recent_bid = 0;
        double most_recent_paid = 0;
        int earliest_time = 0;
        if(sys.gClock > time_window)
            earliest_time = sys.gClock - time_window;

        for(int i=0; i<successQ.size(); i++){
            ph = successQ.get(i);
            if(ph.receiveTime <earliest_time){
                removeset.offer(ph);
            }
            else{
                if(ph.submitTime > most_recent_time){
                    most_recent_time = ph.submitTime;
                    most_recent_bid = (double)ph.q_max;
                    most_recent_paid = ph.qos_paid + ph.qod_paid;
                }
            }
        }

        //remove those paymenthistory in successQ with the index in removeset
        int removeindex = removeset.size();
        for(int j=0; j<removeindex; j++){
            successQ.remove(removeset.poll()); //adjust the successQ to only keep useful info
        }
        //----find the most recent successful QC----------------------------


        //----find the highest failed bid in the window---------------------
        double highest_failure_q = 0;
        removeset.clear();
        for(int i=0; i<failureQ.size(); i++){
            ph = failureQ.get(i);
            if(ph.receiveTime <earliest_time){
                removeset.offer(ph);
            }
            else{
                if(ph.q_max > highest_failure_q){
                    highest_failure_q = ph.q_max;
                }
            }
        }

        //remove those paymenthistory in successQ with the index in removeset
        removeindex = removeset.size();
        for(int j=0; j<removeindex; j++){
            failureQ.remove(removeset.poll()); //adjust the successQ to only keep useful info
        }
        //----find the highest failed bid in the window---------------------


        //decrease from that QC: the small you paid compared to the maximal, the small change you should have
        if(most_recent_bid != 0)
            newbid = most_recent_bid * (1 - down_percent * most_recent_paid/most_recent_bid);
        else
            newbid = lastbid;
        //newbid = future_avg - down_step * most_recent_paid/most_recent_bid;
        return newbid;
    }
}

