package hm;

/*
 * QualityContract.java
 *
 * Created on 12 October 2005, 17:44
 *
 * define the QoD and QoS "gain" as well as "panelty".
 */

/*
 * @author Huiming Qu
 */
public class QualityContract{
    public double cost_reject;
    public double cost_fmiss;
    public double cost_fstale;

    public double fresh_bar;

    public double max_qos;
    public double max_qod;

    public QualityContract(){
        cost_reject = 0;
        cost_fmiss = 0;
        cost_fstale = 0;

        fresh_bar = 0.0;
    }

    public void setCost(double cR, double cDM, double cDS){
        cost_reject = cR;
        cost_fmiss = cDM;
        cost_fstale = cDS;
    }

    public boolean isFresh(double fr){
        return fr >= fresh_bar;
    }
    public double getQos(int time){
        return 0.0;
    }
    public double getQod(int staleness){
        return 0.0;
    }
    public double getQodMax(){
        return max_qod;
    }
    public double getQosMax(){
        return max_qos;
    }
    public int getUUMax(){
        return 0;
    }
    public int getRDMax(){
        return 0;
    }
    public double getQMax(){
        return max_qos + max_qod;
    }
    public double getQMax(int time){
        return 0.0;
    }
    public double getQosFeasible(int time){
        return max_qos;
    }

    public double getQoSMin(){
        return 0.0;
    }
    public double getQoDMin(){
        return 0.0;
    }

    public void setQosMax(double qos){
        max_qos = qos;
    }
    public void setQodMax(double qod){
        max_qod = qod;
    }
    public void setArrTime(int arr){
    }
}
