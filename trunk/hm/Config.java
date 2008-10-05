package hm;

/*
 * Config.java
 *
 * Created on 27 September 2005, 00:32
 *
 * Class Config includes all the system parameter default value, if any is
 * changed by user input, it will be changed in init() in sys.
 */

/**
 * @author Huiming Qu
 */
import java.text.DecimalFormat;

public class Config {

    //constants
    public static final int EDF = 0;
    public static final int HiV = 1;
    public static final int VoD = 2;
    public static final int VRD = 3;
    public static final int LST = 4;
    public static final int PET = 5;
    public static final int FIFO = 6;

    public static final int IMU = 0;
    public static final int ODU = 1;
    public static final int QMF = 2;
    public static final int USM = 3;
    public static final int NO_UPD = 4;

    public static final int LOW_UPDATE_EXE_TIME = 1;
    public static final int HIGH_UPDATE_EXE_TIME = 65;  // DEFFERENCE NEEDS TO BE LOG2(N)
    public static final double FRESHNESS_BAR = 0.9;
    public static final int Q_CAPACITY = 44501;         // for ready queue
    public static final int MAX_INT = 2147483647;

    //static variants with default values
    public static double cost_reject = 0;
    public static double cost_fmiss = 0;
    public static double cost_fstale = 0;

    public static int MAX_TIME = 1800000;
    public static int MAX_NUM_BASE_DATA = 1024;
    public static int MAX_AFTERTIME = 1000000; //1 SECOND

    public static int ctrl_mode;
    public static String sched_mode;

    public static boolean ts = true;
    public static boolean readRD = false;
    public static boolean linearfunction = false;
    public static String  access_file = new String();
    public static String  update_file = new String();

    public static final double UPD_ORIGINAL_WORKLOAD = 0.8103712043445147; // when using ./trace/update_fiftyPercent.trace
    public static final double ACC_ORIGINAL_WORKLOAD = 0.8192820954336236; // when using ./trace/access_onePercent.trace
    public static double upd_adjust_workload;
    public static double acc_adjust_workload;
    public static double upd_adjust_workload_para = 1;
    public static double acc_adjust_workload_para = 1;

    //switches
    public static boolean testUser = false;
    public static boolean testSystem = false;

    //decimal format
    public static DecimalFormat digit3 = new DecimalFormat(".###");
    public static DecimalFormat digit2 = new DecimalFormat(".##");
    public static DecimalFormat digit1 = new DecimalFormat(".#");

    //testing UserAgent_Intel
    public static int window = 10000;
    public static double futureavg_factor = 1.0;
    public static double up_step = 0.1;
    public static double down_step = 0.1;
    public static double down_percent = 0.05;
    public static double bank = 10000;
    public static boolean bankISset = false;
    public static double initBid = 0.0;
    public static boolean initBidSet = false;

    /*************************** function begin **********************************/
    public Config(String u_file, String a_file, int c_mode) {
        update_file = u_file;
        access_file = a_file;
        ctrl_mode = c_mode;
    }

    public static String info(){
        String con = "";
        String controller = " = ";
        String sched = " = ";

        switch (ctrl_mode) {
            case 0:
                controller += "IMU";
                break;
            case 1:
                controller += "ODU";
                break;
            case 2:
                controller += "QMF";
                break;
            case 3:
                controller += "USM";
                break;
            case 4:
                controller += "NONEUPD";
                break;
            case 5:
                controller += "UNIT";
                break;
            default:
                controller += "Not a valid control mode!";
                break;
        }

        switch (sched_mode.charAt(0)) { //query
            case '0':
                sched += "FIFO";
                break;
            case '1':
                sched += "V";
                break;
            case '2':
                sched += "VoD";
                break;
            case '3':
                sched += "VoR";
                break;
            default:
                sched += "Not a valid query scheduling mode!";
                break;
        }
        sched += '-';
        switch (sched_mode.charAt(1)) { //update
            case '0':
                sched += "FIFO";
                break;
            case '1':
                sched += "V";
                break;
            case '2':
                sched += "VoU";
                break;
            default:
                sched += "Not a valid update scheduling mode!";
                break;
        }
        sched += '-';
        switch ( sched_mode.charAt(2)) { //update
            case '0':
                sched += "FIFO";
                break;
            case '1':
                sched += "UH";
                break;
            case '2':
                sched += "QH";
                break;
            case '3':
                sched += "QUTS";
                break;
            default:
                sched += "Not a valid update scheduling mode!";
                break;
        }
        con =  "----------Config Info-----------\n" +
                "access_file: " + access_file + "\n" +
                "update_file: " + update_file + "\n" +
                "ctrl_mode:   " + ctrl_mode + controller + "\n" +
                "sched_mode:  " + sched_mode + sched + "\n" +
                "MAX_TIME:    " + MAX_TIME + "\n"
                ;
        return con;
   }

    public static void Error_TranType(String stackinfo){
        System.exit(0);
        //System.out.println("Tran.reject()::Wrong type of trans!");
        System.out.println(stackinfo + "::Wrong type of trans!");
    }

    public static void ErrorAt(String stackinfo){
        System.out.println("Error at " + stackinfo);
        System.exit(0);
    }

}
