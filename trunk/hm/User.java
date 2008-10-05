package hm;

/*
 * User.java
 *
 * Created on 25 October 2005, 10:10
 *
 * User class and UserItem class are included.
 * static method in User class will be called in sys.run() on every time tick, to adjust the user behavior.
 *
 * NOTE: if creating workload,
 *          - the accessing data set should not exceed the current data set (which is initialized by update trace)
 *          - data access pattern should be skewed, most queries accessing a small number of data items
 *          - workload can be generated through calling transactions.addAccess()
 */

/*
 * @author Huiming Qu
 */

import java.util.LinkedList;


public class User{
    LinkedList<UserItem> users;

    public User(){
        users = new LinkedList<UserItem>();
    }

    public UserItem addUser(int userID){
        UserItem user = new UserItem(userID);
        users.add(user);
        return user;
    }

    public UserItem getUserByID(int userID){
        int index = users.indexOf(new UserItem(userID));
        if(index != -1)
            return users.get(index);
        else
            return null;
    }

    public LinkedList<UserItem> getUserList() {
        return users;
    }

    public UserItem getUser(int index){
        if(index < users.size())
            return users.get(index);
        else
            return null;
    }

    public boolean hasUser(int userID){
        if(users.contains(new UserItem(userID)))
            return true;
        else
            return false;
    }

    public int size(){
        return users.size();
    }

    public void performance(){
        for(int i=0; i<users.size(); i++){
            users.get(i).listFinished();
        }
    }

    public String toString(){
        String print = "\n-------- User Info : total is " + users.size() + " --------\n";
        for(int i=0; i<users.size(); i++){
            print += users.get(i);
        }
        return print;
    }
}

/* Each user has one kinds of queries, one agent and one initial quality contract
 */
class UserItem{

    public int userID;
    public LinkedList<TranItem> tranList;
    public LinkedList<TranItem> unsent;
    public LinkedList<TranItem> finished;
    public LinkedList<TranItem> sent;
    public UserAgent agent;
    public QualityContract qc;

    public double budget;
    public double bank;

    public UserItem(int id){
        userID = id;
        tranList = new LinkedList<TranItem>();
        unsent = new LinkedList<TranItem>();
        finished = new LinkedList<TranItem>();
        sent = new LinkedList<TranItem>();
        if(Config.bankISset){
            bank = Config.bank;
            budget = bank;
        }else{
            bank = 0.0;
            budget = 0.0;
        }
    }

    /* 0~99 fixed
     * 100~199 random
     * 200~299 random_adjust
     * 300+ intel
     */
    public void addTran(TranItem ti){

        tranList.add(ti);
        unsent.add(ti);
        if(!Config.bankISset){
            budget += ti.qc.getQMax();
            bank = budget;
        }

        if(qc == null){
            qc = ti.qc;
            if(userID < 100) {      //FIX 0~99
                agent = new UserAgent_Fixed(this);
            }else if(userID < 200){ //RAN 100~199
                agent = new UserAgent_Random(this);
            }else if(userID < 300){ //HYE 200~299
                agent = new UserAgent_Intel_E(this);//new UserAgent_RandomWatch(this);
            }else if(userID < 400){ //HYB 300-399
                agent = new UserAgent_Intel_pro(this);
            }else if(userID < 500){ //AD 400~499
                agent = new UserAgent_BudgetWatch(this);
            }else if(userID < 600){ //E  500~599
                agent = new UserAgent_E(this);
            }else //HYE     >= 600    always the most intelligent user take care
                agent = new UserAgent_Intel_E(this);
        }
    }

    public QualityContract getInitialQC(){
        return qc;
    }
    public double getBudget(){
        return budget;
    }
    public int getNumUnSent(){
        return unsent.size();
    }
    public boolean hasBudget(){
        return budget > 0;
    }
    /* if transaction arrTime is smaller or equal to sys.gClock,
     * the transaction if deleted from the unfinished list and returned
     */
    public TranItem tranArriving(){

        if(!hasBudget())
            return null;

        TranItem newTran = unsent.peek();
        if(newTran!=null && newTran.arrTime <= sys.gClock){//this transaction should be arriving already
            newTran = unsent.poll(); // newTran = unfinished.remove(); it may not be finished. do it when receive it.
            sent.add(newTran);
            // renew the quality contract with current one
            newTran.qc = agent.newQC(newTran);
            budget -= newTran.qc.getQMax();
            return newTran;
        }
        return null;
    }

    public boolean equals(Object o){
        UserItem u = (UserItem)o;
        if(this.userID == u.userID)
            return true;
        else
            return false;
    }

    public String toString(){
        String print = "---- user " + userID + " " + agent.type() + " has " + tranList.size() + " transactions ----\n";
        for(int i=0; i<tranList.size(); i++){
            print += tranList.get(i).toString();
        }
        return print;
    }



   /* action is taken upon each failure or rejection
    * fixed_user will not change it
    * random_user will change it randomly
    * intelligent user react differently to success, reject, or failure
    */
    public void receiveResult(TranItem ti){
        finished.add(ti);
        sent.remove(ti);
        agent.addHistory(ti);
        budget += ti.qc.getQMax() - ti.getQ(); // refund the left money not paid but saved for the tran
    }

    public void listFinished(){
        String print = "";
        int num_nonzero_profit = 0;
        TranItem ti;
        for(int i=0; i<finished.size(); i++){
            ti = finished.get(i);
            print += ti.toString();
            if((ti.getQos() + ti.getQod()) > 0){
                num_nonzero_profit++;
            }
        }
        System.out.println("User " + userID + " "+ agent.type() + " has successful queries: "
                + num_nonzero_profit + " out of " + tranList.size()
                + ", or " +Config.digit3.format((double)num_nonzero_profit/tranList.size())
                + " budget paid: " + Config.digit3.format(bank - budget) + " out of " + bank
                + ", or " +Config.digit3.format((double)(bank - budget)/bank)
                );
        //System.out.println(print);
    }

}//UserItem

