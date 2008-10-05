package hm;

/*
 * QC_linear.java
 *
 * Created on 30 October 2006, 1:35
 *
 */

/*
 * @author Huiming Qu
 *
 * |QoS
 * |\ max_qos
 * | \
 * |  \
 * |___\_______________
 * arr  \    relDeadline
 * |     \
 * |      \
 * MAX_INT
 *
 */

public class QC_linear extends QualityContract{
    public int arr;
    public int relDeadline;
    public int max_unapplied_updates;

    public double min_qos;
    public double min_qod;
    public double nslope_qos; //negative slope of qos decline
    public double nslope_qod; //negative slope of qod decline

    public QC_linear(){}

    public QC_linear(int arr, int relDeadline, int max_unapplied_updates, double max_qos, double max_qod){
        this.arr = arr;
        this.relDeadline = relDeadline;
        this.max_unapplied_updates = max_unapplied_updates;
        this.max_qod = max_qod;
        this.max_qos = max_qos;
        this.min_qod = 0 - Config.MAX_INT;
        this.min_qos = 0 - Config.MAX_INT;
        nslope_qos = max_qos/relDeadline;
        nslope_qod = max_qod/max_unapplied_updates;
    }

    /* maxqos/relDeadline = tg(a) = nslope_qos
     * qos/(relDeadline - response) = tg(a)
     * qos = nslope_qos * (relDeadline - response)
     */

    public double getQos(int time){
        return nslope_qos * (relDeadline - time);
    }
    public double getQod(int staleness){
        return nslope_qod * (max_unapplied_updates - staleness);
    }
    public double getQosMax(){
        return max_qos;
    }
    public double getQodMax(){
        return max_qod;
    }
    public double getQMax(){
        return max_qos + max_qod;
    }
    public double getQMax(int time){
        return nslope_qos * (relDeadline - time) + max_qod;
    }

    public double getQosFeasible(int time){
        return nslope_qos * (relDeadline - time);
    }
    public int getUUMax(){
        return max_unapplied_updates;
    }
    public int getRDMax(){
        return relDeadline;
    }
    public double getQoSMin(){
        return min_qos;
    }
    public double getQoDMin(){
        return min_qos;
    }

    public void setQosMax(double qos){
        max_qos = qos;
    }
    public void setQodMax(double qod){
        max_qod = qod;
    }
    public void setArrTime(int arrTime){
        arr = arr;
    }
}
