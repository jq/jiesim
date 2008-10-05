package hm;


/*
 * QC_step.java
 *
 * Created on 12 November 2005, 22:03
 *
 */

/*
 * @author Huiming Qu
 * | QoS
 * |--------- max_qos
 * |        |
 * |        | min_qos
 * |________|_______________
 * |        |------------
 * arr      deadline
 */
public class QC_step extends QualityContract{

    public int arr;
    public int deadline;
    public int max_unapplied_updates;

    public double min_qos;
    public double min_qod;

    public QC_step(int arr, int deadline, double max_qos, double min_qos, int max_unapplied_updates, double max_qod, double min_qod) {
        this.arr = arr;
        this.deadline = deadline;
        this.max_unapplied_updates = max_unapplied_updates;
        this.max_qod = max_qod;
        this.max_qos = max_qos;
        this.min_qod = min_qod;
        this.min_qos = min_qos;
    }

    public double getQos(int finishTime){
        if(finishTime <= deadline)
            return max_qos;
        else
            return min_qos;
    }

    public double getQod(int unapplied_updates){
        if(unapplied_updates <= max_unapplied_updates)
            return max_qod;
        else
            return min_qos;
    }

}


