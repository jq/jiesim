package hm;

/*
 * UserAgent.java
 *
 * Created on 25 October 2005, 12:10
 *
 * UserAgent UserAgent_Fixed UserAgent_Random
 *
 * @author Huiming Qu
 */

import java.util.LinkedList;
import java.util.Random;


/* User know by when the result should return and with what staleness the most
 * User do not know how much money should be put on QC
 */
public class UserAgent{
    public QualityContract initialQC;
    public QualityContract currentQC;
    public UserItem owner;
    public double qos_percent; // = max_qos / (max_qos+max_qod)

    public UserAgent(){}

    public UserAgent(UserItem user){
        owner = user;
        initialQC = user.getInitialQC();
        currentQC = initialQC;
        qos_percent = (double)initialQC.getQosMax()/initialQC.getQMax();

    }

    /*************** functions rewrite by child class *********************/
    public void addHistory(TranItem tran){
    }
    public String type(){
        return "ORIGI";
    }
    public QualityContract newQC(TranItem ti){
        return initialQC; //user with fixedQc
    }

    /*************** functions used by child class *********************/
    public QualityContract createQC(TranItem ti, double max_qos, double max_qod, double min_qos, double min_qod, int rd, int uu){
        QualityContract qc = null;
        if(Config.linearfunction)
            qc = new QC_linearStepPenalty(ti.arrTime, rd, uu, max_qos, max_qod, min_qos, min_qod);
        else
            qc = new QC_stepPositive(ti.arrTime, max_qos, max_qod, rd, uu);
        return qc;
    }
    public QualityContract createQCfromBid(TranItem ti, double bid){
        update_currentQC(bid);
        QualityContract qc = null;
        if(Config.linearfunction)
            qc = new QC_linearStepPenalty(ti.arrTime, (QC_linearStepPenalty)currentQC);
        else
            qc = new QC_stepPositive(ti.arrTime, (QC_stepPositive)currentQC);

        return qc;

    }
    // set up a new QC with the max profit, given the ratio between qos and qod, rd, uu are fixed
    public void update_currentQC(double bid){
        if(bid > owner.getBudget()){
            bid = owner.getBudget();
        }
        if(bid < 0){
            bid = 0;
        }
        double qos_max = bid * qos_percent;
        double qod_max = bid - qos_max;
        currentQC.setQosMax(qos_max);
        currentQC.setQodMax(qod_max);

    }
}



class UserAgent_Fixed extends UserAgent{
    public UserAgent_Fixed(UserItem user){
        super(user);
    }
    public String type(){
        return "FIXED";
    }
    public QualityContract newQC(TranItem ti){
        double max_q = initialQC.getQMax();
        return createQCfromBid(ti, max_q);
    }
}

class UserAgent_Random extends UserAgent{

    Random ran = new Random();
    double max_q_min;
    double max_q_range;
//    int rd, uu;

    public UserAgent_Random(UserItem user){
        super(user);

        max_q_min = 1;
        max_q_range = ( initialQC.getQMax()- max_q_min ) * 2 ;

//        rd = initialQC.getRDMax();
//        uu = initialQC.getUUMax();

    }

    //QC_linearStepPenalty(int arr, int relDeadline, int max_unapplied_updates, double max_qos, double max_qod, double min_qos, double min_qod){
    public QualityContract newQC(TranItem ti){
        double max_q = max_q_min + ran.nextDouble() * max_q_range;
        return createQCfromBid(ti, max_q);
    }

    public String type(){
        return "RANDO";
    }
}

class UserAgent_RandomAdjust extends UserAgent{

    Random ran = new Random();

    public double step;
    public int rd, uu;

    public UserAgent_RandomAdjust(UserItem user){
        super(user);

        rd = initialQC.getRDMax();
        uu = initialQC.getUUMax();

        step = 1;

    }

    //QC_linearStepPenalty(int arr, int relDeadline, int max_unapplied_updates, double max_qos, double max_qod, double min_qos, double min_qod){
    public QualityContract newQC(TranItem ti){
        int choice = ran.nextInt(3);
        double max_q = currentQC.getQMax();
        switch (choice){
            case 0:
                max_q += step;
                break;
            case 1:
                max_q -= step;
                break;
            case 2:
                break;
            default:
                break;
        }

        return createQCfromBid(ti, max_q);
    }

    public String type(){
        return "R_ADJ";
    }

}

class UserAgent_BudgetWatch extends UserAgent{

    double factor = 1.0;

    public UserAgent_BudgetWatch(UserItem user){
        super(user);

        //WATCH user id starts from 400
        //factor is 1.0 to 1.99 depending on the user id
        factor += (double)owner.userID % 100 / 100;
    }

    public void addHistory(TranItem tran){
    }

    public String type(){
        return "WATCH";
    }

    public QualityContract newQC(TranItem ti){
        double max_q = 0.0;

        //take avg
        int num = owner.getNumUnSent() + 1; //plus the current sending query
        double leftmoney = owner.getBudget();
        if(num > 0)
            max_q = factor * (leftmoney/num);
        else
            max_q = leftmoney;

        //System.out.println("time: "+ sys.gClock + " max_q: " + max_q + " leftmoney: " + leftmoney + " num: " + num);
        return createQCfromBid(ti, max_q);
    }

}


class UserAgent_RandomWatch extends UserAgent{

    double factor = 1.0;
    double max_q_min = 2.0;
    Random ran = new Random();

    public UserAgent_RandomWatch(UserItem user){
        super(user);

        //WATCH user id starts from 400
        //factor is 1.0 to 1.99 depending on the user id
        factor += (double)owner.userID % 100 / 100;
    }

    public String type(){
        return "WATCH_RAN";
    }

    public QualityContract newQC(TranItem ti){
        double future_avg = 0.0;
        double max_q = 0.0;

        //take avg
        int num = owner.getNumUnSent() + 1; //plus the current sending query
        double leftmoney = owner.getBudget();
        if(num > 0)
            future_avg = factor * (leftmoney/num);
        else
            future_avg = leftmoney;

        double max_q_range = future_avg*2 - max_q_min;
        max_q = max_q_min + ran.nextDouble() * max_q_range;

        //System.out.println("future-avg: " + future_avg + " max_q: " + max_q);

        return createQCfromBid(ti, max_q);
    }

}


