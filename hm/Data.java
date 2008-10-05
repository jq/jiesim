package hm;

/*
 * Data.java
 *
 * Created on 27 September 2005, 13:18
 *
 * system data set info.
 */

/*
 * @author Huiming Qu
 */
import java.util.Vector;

public class Data {
    public DataItem[] bData;
    public Vector<DataItem> vecData;
    int num_data;

    /*************************** function begin **********************************/
    public Data(){
        num_data = 0;
        vecData = new Vector<DataItem>();
    }

    //actually to calculate the total number of data item with the help of vector
    public void addDataItem(int index){
        if (index >= num_data)
            num_data = index + 1;
//        DataItem di = new DataItem(num_data);
//        if(!vecData.contains(di)){
//            vecData.add(di);
//        }
    }

    //initialize the dataset with the total num_data, assuming the data number are all consecutive
    public void finalizeInput(){
        Config.MAX_NUM_BASE_DATA = num_data;
        bData = new DataItem[num_data];
        for(int i=0; i<num_data; i++){
            bData[i] = new DataItem(i);
        }

    }

    public void listUPDinfo(){
        for(int i=0; i<num_data; i++){
            System.out.println(bData[i].p_update);
        }
    }

    public void setUpdateOnCall(int index){
        bData[index].updateOnCall = true;
    }

    public void undoUpdateOnCall(int index){
        bData[index].updateOnCall = false;
    }

    public boolean isUpdateOnCall(int index){
        return bData[index].updateOnCall;
    }

    public int size(){
        return num_data;
    }

}//class Data

