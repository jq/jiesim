package hm;
/*
 * QC_stepPositive.java
 *
 * Created on 12 November 2005, 21:55
 *
 */

/*
 * @author Huiming Qu
 *
 * |QoS
 * |--------- max_qos
 * |        |
 * |        | 0
 * |________|______________
 * arr      relDeadline
 */
public class QC_stepPositive extends QualityContract{
    public int arr;
    public int relDeadline;
    public int max_unapplied_updates;

    public QC_stepPositive(){}

    public QC_stepPositive(int arr, double max_qos, double max_qod, int relDeadline, int max_unapplied_updates) {
        this.arr = arr;
        this.relDeadline = relDeadline;
        this.max_unapplied_updates = max_unapplied_updates;
        this.max_qod = max_qod;
        this.max_qos = max_qos;
    }

    public QC_stepPositive(int arr, QC_stepPositive qc) {
        this.arr = arr;
        this.relDeadline = qc.relDeadline;
        this.max_unapplied_updates = qc.max_unapplied_updates;
        this.max_qod = qc.max_qod;
        this.max_qos = qc.max_qos;
    }

    public double getQos(int time){
        //if(finishTime == TranItem.UNSTARTED || finishTime == TranItem.ABORTED)
        if( time > relDeadline )
            return 0.0;
        else
            return max_qos;
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
    public double getQod(int staleness){
        if (staleness >= max_unapplied_updates )
            return 0.0;
        else
            return max_qod;
    }

    public double getQMax(int time){
        if( time > relDeadline )
            return max_qod;
        else
            return max_qos + max_qod;
    }

    public double getQosFeasible(int time){
        return max_qos;
    }

    public int getUUMax(){
        return max_unapplied_updates;
    }
    public int getRDMax(){
        return relDeadline;
    }

}
