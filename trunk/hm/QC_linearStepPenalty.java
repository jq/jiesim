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
 * arr |     relDeadline
 * |   |_______________
 * |
 * MAX_INT
 *
 */

public class QC_linearStepPenalty extends QC_linear{

    public QC_linearStepPenalty(){}

    //QC with penalty limit
    public QC_linearStepPenalty(int arr, int relDeadline, int max_unapplied_updates, double max_qos, double max_qod, double min_qos, double min_qod){
        this.arr = arr;
        this.relDeadline = relDeadline;
        this.max_unapplied_updates = max_unapplied_updates;
        this.max_qod = max_qod;
        this.max_qos = max_qos;
        this.min_qod = min_qod;
        this.min_qos = min_qos;
        nslope_qos = max_qos/relDeadline;
        nslope_qod = max_qod/max_unapplied_updates;
    }

    //QC with penalty limit 0, linearPositive
    public QC_linearStepPenalty(int arr, int relDeadline, int max_unapplied_updates, double max_qos, double max_qod){
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

    //just to reset arrTime
    public QC_linearStepPenalty(int arr, QC_linearStepPenalty qc){
        this.arr = arr;
        this.relDeadline = qc.relDeadline;
        this.max_unapplied_updates = qc.max_unapplied_updates;
        this.max_qod = qc.max_qod;
        this.max_qos = qc.max_qos;
        this.min_qod = qc.min_qod;
        this.min_qos = qc.min_qod;
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
            realqos = min_qos;
        else
            realqos = nslope_qos * (relDeadline - time);
        return realqos;
    }

    public double getQod(int staleness){
        if (staleness >= max_unapplied_updates )
            return min_qod;
        else
            return nslope_qod * (max_unapplied_updates - staleness);
    }

    public double getQMax(int time){
        if( time > relDeadline )
            return min_qos + max_qod;
        else
            return nslope_qos * (relDeadline - time) + max_qod;
    }

    public double getQosFeasible(int time){
        if( time > relDeadline )
            return min_qos;
        else{
            double r = nslope_qos * (relDeadline - time);
            //System.out.println("--> "+  r + " slope = " + nslope_qos + " mqos = " + max_qos + " rd = "+relDeadline +" time = " + time);
            return r;
        }
    }

}
