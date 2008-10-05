package hm;

/*
 * Controller_NONEUPD.java
 *
 * Created on 13 November 2005, 00:44
 * @author Huiming Qu
 */
public class Controller_NONEUPD extends Controller{

    public Controller_NONEUPD() {
    }

    public boolean admissionCtrl(TranItem ti, ReadyQueue readyQ){
        if(ti.isAccess())
            return true;
        else if(ti.isUpdate())
            return false;
        else{
            Config.Error_TranType("Controller.Admission Control");
            return false;
        }
    }
}