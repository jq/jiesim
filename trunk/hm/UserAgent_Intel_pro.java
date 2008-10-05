package hm;


/*
 * UserAgent_Intel_pro.java
 *
 * Created on 7 November 2005, 11:10
 *
 * UserAgent_Intel_pro UserAgent_Intel_sta
 *
 * @author Huiming Qu
 */

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Random;

class UserAgent_Intel_pro extends UserAgent_Intel{

    double factor = 1.2;

    public UserAgent_Intel_pro(UserItem user){
        super(user);

        //factor += (double)owner.userID % 100 / 100;

    }
    public String type(){
        return "INTEL_PRO";
    }

    /*----------private functions--------------------------*/

    /* take the most recent successful bid, sucBid
     * reduce from sucBid proportionally
     * down_step_range = 0.1
     * down_step = down_step_range * paid/maximum
     * the highest failure bid is the lower bound
     */
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


    public double decreaseBidbyWeightedDegree(double lastbid){
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
            newbid = most_recent_bid - down_step * most_recent_paid/most_recent_bid;
        else
            newbid = lastbid;
        //newbid = future_avg - down_step * most_recent_paid/most_recent_bid;
        return newbid;
    }

    public double increaseBid(double lastbid){
        return getFutureAvg()*factor;
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



class UserAgent_Intel_sta extends UserAgent_Intel{

    Random ran = new Random();

    public UserAgent_Intel_sta(UserItem user){
        super(user);
    }
    public String type(){
        return "INTEL_STA";
    }
    /*----------private functions--------------------------*/

    // get a new try out sum of max profit according to the history
    public double newBid(){
        double lastbid = currentQC.getQMax();
        double newbid = lastbid;
        int num_failed = failureQ.size();

        //----find the future avg-------------------------------------------
        double future_avg = 0;
        int num = owner.getNumUnSent() + 1; //plus the current sending query
        double leftmoney = owner.getBudget();
        if(num > 0)
            future_avg = leftmoney/num;
        //----find the future avg-------------------------------------------


        //----increase w/ failure percentage, decrease w/ success percentage, no change if diff < 10 perc
        successQ_trim();
        failureQ_trim();
        int fail_size = failureQ.size();
        int succ_size = successQ.size();
        int sum = fail_size + succ_size;
        double incr_perc = 0, decr_perc = 0;
        if(sum > 0){
            incr_perc = (double)fail_size/sum;
            decr_perc = 1 - incr_perc;
        }
        if(Math.abs(decr_perc - incr_perc) < 0.1) // no changes, just future avg
            newbid = future_avg;
        else{
            if(ran.nextDouble() < incr_perc) // increase
                newbid = future_avg + up_step;
            else
                newbid = future_avg - down_step;
        }

        if(newbid > owner.getBudget()){
            newbid = owner.getBudget();
        }

//        System.out.println("STA new bid = " + newbid + " favg= " + future_avg + " lastbid= " + lastbid);

        return newbid;
    }



    public void failureQ_trim(){
        PaymentHistory ph = null;
        LinkedList<PaymentHistory> removeset = new LinkedList<PaymentHistory>();
        int removeindex;

        int earliest_time = 0;
        if(sys.gClock > time_window)
            earliest_time = sys.gClock - time_window;

        //----remove those ph out site of window in failure QC-----------
        for(int i=0; i<failureQ.size(); i++){
            ph = failureQ.get(i);
            if(ph.receiveTime <earliest_time){
                removeset.offer(ph);
            }
        }

        //remove those paymenthistory in successQ with the index in removeset
        removeindex = removeset.size();
        for(int j=0; j<removeindex; j++){
            failureQ.remove(removeset.poll()); //adjust the successQ to only keep useful info
        }
        //----remove those ph out site of window in failure QC-----------


    }

    public void successQ_trim(){
        //----remove those ph out site of window in successful QC-----------
        PaymentHistory ph = null;
        LinkedList<PaymentHistory> removeset = new LinkedList<PaymentHistory>();
        int removeindex;

        int earliest_time = 0;
        if(sys.gClock > time_window)
            earliest_time = sys.gClock - time_window;

        for(int i=0; i<successQ.size(); i++){
            ph = successQ.get(i);
            if(ph.receiveTime <earliest_time){
                removeset.offer(ph);
            }
        }

        //remove those paymenthistory in successQ with the index in removeset
        removeindex = removeset.size();
        for(int j=0; j<removeindex; j++){
            successQ.remove(removeset.poll()); //adjust the successQ to only keep useful info
        }
        removeset.clear();
        //----remove those ph out site of window in successful QC-----------
    }


}