/*
 * File: AddUser.java
 * Auth: Jie Xu 
 * Created: Nov. 4, 2006
 */

import java.io.*;                                                                                                                           
import java.util.*;  
public class AddUser {                                                                                                               
                                                                                                                                            
    public Vector<Access> usrAccessVec = new Vector<Access>();

	public AddUser(Vector<Access> inputAccess, String inputUserProfile){                                                                                                
        Random ran = new Random();                                                                                          
                                                                                                                   

        int usrIndex = -1, usrNum = -1;
        int usrID, queryNum;
        Vector<Integer> usrid = new Vector<Integer>();
        Vector<Integer> usrlist = new Vector<Integer>();
        
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
                	queryNum = Integer.parseInt(line_tokenizer.nextToken());
                	
                	if (usrid.indexOf(usrID)==-1){
                	 usrid.add(new Integer(usrID));
                	}

                	for (int i=0; i<queryNum; i++){
						usrlist.add(new Integer(usrID));
					}
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
            usrNum = usrid.size();
            //System.out.println(usrlist.size());
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
            String userAccess = "userAccess_" + Integer.toString(usrNum) + "user.txt"; 
        	FileOutputStream out = new FileOutputStream(userAccess); 
            PrintStream out_p = new PrintStream( out ); 
			
            // print the format of output final trace
            out_p.println ("arrTime  baseData  exeTime	usrId");                                                             
 
            int total = inputAccess.size();
            int arrTime, baseData, exeTime;
            
            for (int i=0; i<total; i++){
            	arrTime = inputAccess.elementAt(i).arrTime;
            	baseData = inputAccess.elementAt(i).baseData;
            	exeTime = inputAccess.elementAt(i).exeTime;

            	usrIndex = ran.nextInt(total-i);
            	usrID = (usrlist.get(usrIndex)).intValue();
            	
            	inputAccess.elementAt(i).usrID = usrID;
            	usrlist.removeElementAt(usrIndex);
            	//to be fair, ran(total) everytime
            	/*while (usrID == -1){
            		usrIndex = ran.nextInt(total);
            		usrID = (usrlist.get(usrIndex)).intValue();
            		
            	}
            	inputAccess.elementAt(i).usrID = usrID;
            	usrlist.add(usrIndex, new Integer(-1));*/ 
            	
            	out_p.println (arrTime + "\t" + baseData + "\t" + exeTime + "\t" + usrID); 
            }
            out_p.close();
            usrAccessVec = inputAccess;
        }                                                                                                                                   
        catch (IOException exception){                                                                                                      
            System.out.println (exception);     
            System.exit(0);                                                                                                  
        }                                                                                                                                   
                                                                                                                                           
    }                                                                                                                                       
                                                                                                                                            
                                                                                                                                            
}                                                                                                                                           



