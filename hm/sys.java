package hm;

/*
 * sys.java
 *
 * Created on 27 September 2005, 00:07
 *
 * main system class
 *
 * @author Huiming Qu
 */
import java.io.*;
import java.util.*;

public class sys {

    public Controller controller;  //system controller
    public Monitor monitor;     //system monitor
    public Tran transactions;   //system transaction set: update and access
    public Data data;           //system data set
    public User users;

    public static int gClock;          //time tick
    public int max_time;        //total running time in main loop

    public boolean test = false;

    /*************************** function begin **********************************/
    public sys(String[] args) {
        init(args);     //init parameters
	loadInit(); //sort the transaction by arrival time
        //transactions.listAccessQ();
        //transactions.listUpdateQ();
        run();      //system start processing in real time
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
	sys system = new sys(args);
        //System.out.println("Total Running Time = " + (System.currentTimeMillis() - start)/6000 + " minute(s)\n\n");
    }

    /* main loop */
    public void run(){

	for(gClock = 0; gClock < max_time; gClock++)
	{
                //if(gClock<10)transactions.sysSnapshot(gClock);
                //if(gClock % 100 == 0) System.out.println(gClock);

		transactions.transArrivingFromUser(users); // put arrived transaction in submit queue

                transactions.admissionCtrl(); // decide if admit

 		transactions.execute(); // priority control and concurrency control

		//controller.LoadBalancing(); // adjust the tightness of admission control and shedding speed of updates.

                if(Config.ts){
                    transactions.readyQ.adjustTimeShare();
                    monitor.collectEveryTimeTick();
                }

                //users.act();

                if(test && sys.gClock > 1422 && sys.gClock < 1){
                    System.out.println("----------------------------2000");
                }

	}

        if(Config.testSystem){
            monitor.finalStat();
            //monitor.printOntimeStat();
	    //monitor.statTran();
            //monitor.detailStat();
        }

        users.performance();
        if(Config.testUser){
            System.out.println(users);
        }
    }

    /* set up the system parameters either by defaults or user input.
     */
    public void init(String[] args){
        int argc = args.length;

        // echo the command line
        System.out.print("java sys");
        for(int i=0; i<argc; i++){
            System.out.print(" "+args[i]);
        }
        System.out.println();


        // get the must see 3 parameters: "period_file trace_file scheMode"
        // return error msg if not enough parameters inputted.
        if (argc < 4){
            System.out.println("java sys update_trace access_trace controlMode scheMode");
            System.out.println("[-win][-ups][-dos][-bank]");

            System.out.println("controlMode = 0:IMU, 5:UNIT");
            System.out.println("scheMode = hundreds tens ones");
            System.out.println("[1]: within queryQ, FIFO:0, V:1, VoD:2, VoR:3");
            System.out.println("[2]: within updateQ, FIFO:0, V:1, VoU:2");
            System.out.println("[3]: between queryQ and updateQ, FIFO:0, UH:1, QH:2, QUTS:3");
            System.out.println("000: FIFO");
            System.out.println("211: VoD-V-UH 212: VoD-V-QH 213: VoD-V-QUTS");
            System.out.println("221: VoD-VoU-UH 222: VoD-VoU-QH 223: VoD-VoU-QUTS");
            System.out.println("001: FIFO-FIFO-UH 002: FIFO-FIFO-QH 003: V-QUTS-VoD");

            System.exit(-1);
        }

        Config.update_file = args[0];
        Config.access_file = args[1];
        Config.ctrl_mode =  Integer.parseInt(args[2]); //IMU:0, ODU:1, QMF:2, USM:3, NON_UPD:4, UNIT:5
        Config.sched_mode = args[3];

        if(Config.sched_mode.length()<3){
            System.out.println("\nInvalid scheduling mode! " + Config.sched_mode);
            System.out.println("scheMode = [1][2][3]");
            System.out.println("[1]: within queryQ, FIFO:0, V:1, VoD:2, VoR:3");
            System.out.println("[2]: within updateQ, FIFO:0, V:1, VoU:2");
            System.out.println("[3]: between queryQ and updateQ, FIFO:0, UH:1, QH:2, QUTS:3");
            System.out.println("000: FIFO");
            System.out.println("211: VoD-V-UH 212: VoD-V-QH 213: VoD-V-QUTS");
            System.out.println("001: FIFO-FIFO-UH 002: FIFO-FIFO-QH 003: V-QUTS-VoD");
            System.exit(-1);
        }

        System.out.println(Config.info());

        // keep getting more parameters and theoratically they can reset all the parameters in config
        for(int index=0; index < argc; index++) {
            if (args[index].equals("-u")) {
                index++;
                Config.upd_adjust_workload = Double.valueOf(args[index]);
                Config.upd_adjust_workload_para = Config.upd_adjust_workload/Config.UPD_ORIGINAL_WORKLOAD;
                System.out.println("In Sys::Init, update: " + Config.UPD_ORIGINAL_WORKLOAD + "\t" + Config.upd_adjust_workload + "\t" + Config.upd_adjust_workload_para);
            }
            if (args[index].equals("-a")) {
                index++;
                Config.acc_adjust_workload = Double.valueOf(args[index]);
                Config.acc_adjust_workload_para = Config.acc_adjust_workload/Config.ACC_ORIGINAL_WORKLOAD;
                System.out.println("In Sys::Init, access: " + Config.ACC_ORIGINAL_WORKLOAD + "\t" + Config.acc_adjust_workload + "\t" + Config.acc_adjust_workload_para);
            }
            if (args[index].equals("-atom")) {
                index++;
                ReadyQueue.atomTime = Integer.parseInt(args[index]);
            }
            if (args[index].equals("-sp")){
                index++;
                ReadyQueue.samplingPeriod = Integer.parseInt(args[index]);
            }
            if (args[index].equals("-al")){
                index++;
                ReadyQueue.alpha = Double.parseDouble(args[index]);
            }
            if (args[index].equals("-be")){
                index++;
                ReadyQueue.beta = Double.parseDouble(args[index]);
            }
            if (args[index].equals("-mon_ontime")){
                Monitor.print_ontime_qosqod = true;
            }
            if (args[index].equals("-noTS")){
                Config.ts = false;
            }
            if (args[index].equals("-readRD")){
                Config.readRD = true;
            }
            if (args[index].equals("-linear")){
                Config.linearfunction = true;
            }
            if (args[index].equals("-onSp")){
                Monitor.print_on_samplingPeriod = true;
            }
            if (args[index].equals("-onTick")){
                Monitor.print_on_tick = true;
            }
            if (args[index].equals("-testUser")){
                Config.testUser = true;
            }
            if (args[index].equals("-testSystem")){
                Config.testSystem = true;
            }
            // testing UserAgent_Intel
            if (args[index].equals("-win")) {
                index++;
                Config.window = Integer.parseInt(args[index]);
            }
            if (args[index].equals("-ups")) {
                index++;
                Config.up_step = Double.parseDouble(args[index]);
            }
            if (args[index].equals("-dos")) {
                index++;
                Config.down_step = Double.parseDouble(args[index]);
            }
            if (args[index].equals("-bank")) {
                index++;
                Config.bank = Double.parseDouble(args[index]);
                Config.bankISset = true;
            }
            if (args[index].equals("-initBid")) {
                index++;
                Config.initBid = Double.parseDouble(args[index]);
                Config.initBidSet = true;
            }
            if (args[index].equals("-downperc")) {
                index++;
                Config.down_percent = Double.parseDouble(args[index]);
            }
            if (args[index].equals("-factor")) {
                index++;
                Config.futureavg_factor = Double.parseDouble(args[index]);
            }
        }

    }


    /* initialize the system, load the access and update
     */
    public void loadInit(){
        // in Tran, generate accessQ from trace_file
        String line, line_qos, line_rd;
        StringTokenizer tokenizer;
        Integer int_temp;

        switch (Config.ctrl_mode) {
            case 0:
                controller = new Controller_IMU();
                break;
            case 1:
                controller = new Controller_ODU();
                break;
            case 2:
                controller = new Controller_QMF();
                break;
            case 3:
                controller = new Controller_USM();
                break;
            case 4:
                controller = new Controller_NONEUPD();
                break;
            case 5:
                controller = new Controller_UNIT();
                break;
            default:
                System.out.println("Not a valid control mode!");
                break;
        }
        data = new Data();
        users = new User();
        transactions = new Tran(data, controller, users);


        load_update(Config.update_file);
        load_query(Config.access_file);

//        if(Config.testUser){
//            System.out.println(users);
//        }

        //initialize the data set
        data.finalizeInput();

        monitor = new Monitor(transactions, data);
        controller.link(monitor, data);

    }


    public void load_query(String queryfilename){
        // in Tran, generate accessQ from trace_file
        String line, line_qos, line_rd;
        StringTokenizer tokenizer;
        Integer int_temp;

        int userID;
        UserItem user;
        int arrTime = 0, baseData, estExeTime;
        int relDeadline, nUnapplied;
        int qos_max = 0, qod_max = 0, qos_min = 0, qod_min = 0;
        try
        {
            FileReader fr_access = new FileReader (queryfilename);
            BufferedReader br_access = new BufferedReader (fr_access);

            //line = br_access.readLine();    //ignore the first line notes
            line = br_access.readLine();

            while (line != null)
            {
                tokenizer = new StringTokenizer (line);
                try
                {   // arrTime  baseData  estExeTime  usrId  relDeadline  maxUnappliedupdates  maxQos  maxQod  minQos  minQod
                    arrTime = Integer.parseInt(tokenizer.nextToken());
                    baseData = Integer.parseInt(tokenizer.nextToken());
                    estExeTime = (int)(Integer.parseInt(tokenizer.nextToken()) * Config.acc_adjust_workload_para);

                    userID = Integer.parseInt(tokenizer.nextToken());
                    user = users.getUserByID(userID);
                    if(user == null){
                        user = users.addUser(userID);
                    }

                    relDeadline = Integer.parseInt(tokenizer.nextToken());
                    nUnapplied = Integer.parseInt(tokenizer.nextToken());

                    qos_max = Integer.parseInt(tokenizer.nextToken());
                    qod_max = Integer.parseInt(tokenizer.nextToken());
                    qos_min = Integer.parseInt(tokenizer.nextToken());
                    qod_min = Integer.parseInt(tokenizer.nextToken());


                    data.addDataItem(baseData);

                    // construct access transaction's data set, it has one data currently
                    Vector<Integer> baseDataSet = new Vector<Integer>();
                    int_temp = new Integer(baseData);
                    baseDataSet.add(int_temp);

                    // construct new transaction with the line info
                    transactions.addAccess(arrTime, baseDataSet, estExeTime, user, relDeadline, nUnapplied, qos_max, qod_max, qos_min, qod_min);
                }
                catch (NumberFormatException exception)
                {
                    System.out.println ("In file " + queryfilename + " Error in input. Line ignored:");
                    System.out.println (line);
                }
                catch (NoSuchElementException exception)
                {
                    System.out.println ("In file " + queryfilename + " NoSuchElementException. Line ignored:");
                    System.out.println (line);
                    //System.exit(0);
                }
                line = br_access.readLine();

            }
            Config.MAX_TIME = arrTime + Config.MAX_AFTERTIME;
            max_time = Config.MAX_TIME;
            br_access.close();

        }
        catch (FileNotFoundException exception)
        {
            System.out.println ("The file " + queryfilename + " or reDeadline file " + " was not found.");
        }
        catch (IOException exception)
        {
            System.out.println (exception);
        }

//        print to check if file is correctly put in transactions.
//	transactions.listAccessQ();
//         System.out.println (users);
    }

    public void load_update(String updatefilename){
        String line;
        StringTokenizer tokenizer;

        int arrTime=0, baseData, estExeTime;
        try
        {
            FileReader fr_update = new FileReader (updatefilename);
            BufferedReader br_update = new BufferedReader (fr_update);

            int period;
            //line = br_update.readLine(); //ignore the first line notes
            line = br_update.readLine();
            while (line != null)
            {
                tokenizer = new StringTokenizer (line);
                try
                {
                    arrTime = Integer.parseInt(tokenizer.nextToken());
                    baseData = Integer.parseInt(tokenizer.nextToken());
                    //estExeTime = (int)(Integer.parseInt(tokenizer.nextToken()) * Config.upd_adjust_workload_para);
                    estExeTime = Integer.parseInt(tokenizer.nextToken());
                    //System.out.print(Config.upd_adjust_workload_para + "\t" + estExeTime + "\t");
                    estExeTime = (int)(estExeTime * Config.upd_adjust_workload_para);
                   // System.out.println(estExeTime * Config.upd_adjust_workload_para);
                    // construct new transaction with the line info
                    data.addDataItem(baseData);
                    transactions.addUpdate(arrTime, baseData, estExeTime);

                }
                catch (NumberFormatException exception)
                {
                   System.out.println ("In file " + updatefilename + " Error in input. Line ignored:");
                   System.out.println (line);

                }
                catch (NoSuchElementException exception)
                {
                    System.out.println ("In file " + updatefilename + " NoSuchElementException. Line ignored:");
                    System.out.println (line);
//                    System.exit(0);
                }
                line = br_update.readLine();

            }
            br_update.close();
        }
        catch (FileNotFoundException exception)
        {
            System.out.println ("The file " + updatefilename + " was not found.");
        }
        catch (IOException exception)
        {
            System.out.println (exception);
        }

    }


}
