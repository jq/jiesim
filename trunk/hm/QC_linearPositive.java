package hm;

/*
 * QC_linearPositive.java
 *
 * Created on 1 July 2006, 1:35
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
 * arr      relDeadline
 */


public class QC_linearPositive extends QC_linear{

    public QC_linearPositive(int arr, double max_qos, int relDeadline, double max_qod, int max_unapplied_updates) {
        this.arr = arr;
        this.relDeadline = relDeadline;
        this.max_unapplied_updates = max_unapplied_updates;
        this.max_qod = max_qod;
        this.max_qos = max_qos;
        this.min_qod = 0;
        this.min_qos = 0;
        nslope_qos = max_qos/relDeadline;
        nslope_qod = max_qod/max_unapplied_updates;
    }


    /* maxqos/relDeadline = tg(a) = nslope_qos
     * qos/(relDeadline - response) = tg(a)
     * qos = nslope_qos * (relDeadline - response)
     */
    public double getQos(int time){
        double realqos = 0;
        if( time > relDeadline )
            return 0.0;
        else
            realqos = nslope_qos * (relDeadline - time);
            return realqos;
    }

    public double getQod(int staleness){
        if (staleness >= max_unapplied_updates )
            return 0.0;
        else
            return nslope_qod * (max_unapplied_updates - staleness);
    }

    public double getQMax(int time){
        if( time > relDeadline )
            return max_qod;
        else
            return nslope_qos * (relDeadline - time) + max_qod;
    }

    public double getQosFeasible(int time){
        if( time > relDeadline )
            return 0;
        else
            return nslope_qos * (relDeadline - time);
    }

}

