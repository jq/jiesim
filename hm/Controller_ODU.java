package hm;

/*
 * Controller_ODU.java
 *
 * Created on 28 September 2005, 00:45
 *
 * @author Huiming Qu
 */
public class Controller_ODU extends Controller{
    Data data;
    public Controller_ODU() {
    }
    public void link(Monitor mon, Data d){
        data = d;
    }
    public boolean onDemandUpdate(TranItem ti, int index){
        if ( !data.bData[index].isFresh() )
            return true;
        else
            return false;
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
