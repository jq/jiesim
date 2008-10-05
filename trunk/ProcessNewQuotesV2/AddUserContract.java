/*
 * Modified by Jie Xu @ Oct. 26, 2006
 */

/*
 * addUserContract.java
 *
 * Created on 17 November 2005, 22:47
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/*
 * @author Huiming Qu
 */
import java.io.*;                                                                                                                           
import java.util.*;  
public class AddUserContract {                                                                                                               
                                                                                                                                            
    public Vector<profile> prof = new Vector<profile>();
                                                                                                                                          
    public AddUserContract(Vector<Access> inputAccess, String inputUserProfile, String output) 
    {                                                                                                
        //Random ran = new Random();                                                                                          
                                                                                                                   
        int arrTime, baseData, exeTime, usrID;
        int maxQos, relDeadline, maxQod, maxUnappliedupdates;
        int minQos, minQod;
        int usrIndex = -1;
        Vector<Integer> usrid = new Vector<Integer>();
        String line;                                                                                                                               
        StringTokenizer line_tokenizer; 
        
        //read inputUserProfile and put into an array, get usrNum;
        try
        {                                                                                                                                   
            FileReader fr_access = new FileReader (inputUserProfile);                                                                             
            BufferedReader br_access = new BufferedReader (fr_access);    
			                                                           
            line = br_access.readLine();                                                                                                    
            while (line != null)                                                                                                            
            {                                                                                                                               
              
                line_tokenizer = new StringTokenizer (line);                                                                                
                                                                                             
                try                                                                                                                         
                {   
                	//100	1000	5	100	-0.0	5	1	-0.0
                	usrID = Integer.parseInt(line_tokenizer.nextToken());
                	Integer.parseInt(line_tokenizer.nextToken()); //pass query number;
					maxQos = Integer.parseInt(line_tokenizer.nextToken());
					relDeadline = Integer.parseInt(line_tokenizer.nextToken());
					minQos = (int)( Double.parseDouble(line_tokenizer.nextToken()));
					maxQod = Integer.parseInt(line_tokenizer.nextToken());
					maxUnappliedupdates = Integer.parseInt(line_tokenizer.nextToken());
					minQod = (int)(Double.parseDouble(line_tokenizer.nextToken()));
					
					//keep same index in usrid and prof
					usrid.add(new Integer(usrID));
					prof.add(new profile(relDeadline, maxUnappliedupdates, maxQos, maxQod, minQos, minQod));                                                                                               
                }                                                                                                                           
                catch (NumberFormatException exception)                                                                                     
                {                                                                                                                           
                    System.out.println ("FormatException. While reading Line ignored:");                                                                  
                    System.out.println (line);                                                                                              
                    //System.exit(0);                                                                                                       
                }                                                                                                                           
                catch (NoSuchElementException exception)                                                                                    
                {                                                                                                                           
                    System.out.println ("NoSuchElementException. While reading Line ignored:");                                                           
                    System.out.println (line);                                                                                              
                    //System.exit(0);                                                                                                       
                }    
                line = br_access.readLine();     
            }           
            //usrNum = usrid.size();                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         
            br_access.close();                                                                                                                                                                                                                                                 
        }
        catch (FileNotFoundException exception){                                                                                            
            System.out.println ("The file " + inputUserProfile + " was not found.");      
            System.exit(0);                                                              
        }                                                                                                                                   
        catch (IOException exception){                                                                                                      
            System.out.println (exception);  
            System.exit(0);                                                                                                     
        }   
//        // print the user profile info
//        System.out.println("----------profile read as follows----------total user is: " +usrNum);
//        for(int i=0; i<usrNum; i++){
//        	System.out.println(prof.elementAt(i));
//        	}
        // read in traceTime and output                                                                                    
        try                                                                                                                                 
        {                                                                                                                                   
            FileOutputStream out = new FileOutputStream(output); ; 
            PrintStream out_p = new PrintStream( out ); 
			
            // print the format of output final trace
            //out_p.println ("arrTime  baseData  estExeTime  usrId  relDeadline  maxUnappliedupdates  maxQos  maxQod  minQos  minQod");                                                             
 
            for (int i=0; i<inputAccess.size(); i++){
            	arrTime = inputAccess.elementAt(i).arrTime;
            	baseData = inputAccess.elementAt(i).baseData;
            	exeTime = inputAccess.elementAt(i).exeTime;
            	usrID = inputAccess.elementAt(i).usrID;
            	usrIndex = usrid.indexOf(usrID);
            	out_p.println (arrTime + "\t" + baseData + "\t" + exeTime + "\t" + usrID + "\t" + prof.elementAt(usrIndex) ); 
            }
            out_p.close();                                                                                                                                    
        }                                                                                                                                   
        catch (IOException exception){                                                                                                      
            System.out.println (exception);     
            System.exit(0);                                                                                                  
        }                                                                                                                                   
                                                                                                                                           
    }                                                                                                                                       
                                                                                                                                            
                                                                                                                                            
}                                                                                                                                           

class profile{
	
	int usrId;
	int maxQos;
	int relDeadline;
	int minQos;
	int maxQod;
	int maxUnappliedupdates; 
	int minQod;
	
	public profile(int reld, int upd, int qos, int qod, int nqos, int nqod){
		relDeadline = reld;
		maxUnappliedupdates = upd;	
		maxQos = qos;
		maxQod = qod;
		minQos = nqos;
		minQod = nqod;
	}
	public String toString(){
		return relDeadline + "\t" + maxUnappliedupdates + "\t" + maxQos + "\t" + maxQod + "\t" + minQos + "\t" + minQod;
		//return maxQos + "\t" + relDeadline + "\t" + maxQod + "\t" + maxUnappliedupdates;
	}

}                                                                                                                                     


