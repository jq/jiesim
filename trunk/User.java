import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.ArrayList;


public class User {
	static int UserNumber;
    String config = "userConfig.txt";
    static User[] getUsers() {
		User[] u = new User[User.UserNumber];
		for (int i = 0; i<UserNumber; ++i) {
			u[i] = new User();
		}
        return u;
    }
    public int pay(int responseTime, float dataFresh) {
    	return 0;
    }
    
    Vector<Integer> usrid = new Vector<Integer>();
    Vector<Integer> usrlist = new Vector<Integer>();

	/*
	 * @param: access, userprofile, 
	 * @return: user number
	 */
    public int addUser(Vector<Access> inputAccess, String inputUserProfile, String output){                                                                                                
        Random ran = new Random();                                                                                          
         
        usrid.clear();
        usrlist.clear();
        int usrIndex = -1, usrNum = -1, usrPos = -1;
        int usrID, queryNum;
        int maxQos, minQos, relDeadline, maxQod, minQod;
        double fresh;
        Vector<Profile> prof = new Vector();
                
        String line;                                                                                                                               
        StringTokenizer line_tokenizer; 
        
        //read inputUserProfile and put into an array, get usrNum, build vector of id and list;
        try
        {                                                                                                                                   
            FileReader fr_access = new FileReader (inputUserProfile);                                                                             
            BufferedReader br_access = new BufferedReader (fr_access);  
            
            line = br_access.readLine();                                                                                                    
            while (line != null) {                                                                                                                               
                line_tokenizer = new StringTokenizer (line);                                                                                
                try {                                                                                                                       
                	//100	1000	5	100	-0.0	5	1	-0.0
                	usrID = Integer.parseInt(line_tokenizer.nextToken());
                	queryNum = Integer.parseInt(line_tokenizer.nextToken());
					maxQos = Integer.parseInt(line_tokenizer.nextToken());
					relDeadline = Integer.parseInt(line_tokenizer.nextToken());
					minQos = (int)( Double.parseDouble(line_tokenizer.nextToken()));
					maxQod = Integer.parseInt(line_tokenizer.nextToken());
					fresh = Double.parseDouble(line_tokenizer.nextToken());
					minQod = (int)(Double.parseDouble(line_tokenizer.nextToken()));
                	
					//keep same index in usrid and prof
                	if (usrid.indexOf(usrID)==-1){
                	 usrid.add(new Integer(usrID));
                	 prof.add(new profile(relDeadline, fresh, maxQos, maxQod, minQos, minQod));  
                	}
                	for (int i=0; i<queryNum; i++){
						usrlist.add(new Integer(usrID));
					}
                }                                                                                                                           
                catch (NumberFormatException exception) {                                                                                    
                    System.out.println ("FormatException. While reading Line ignored:");                                                                  
                    System.out.println (line);                                                                                              
                    //System.exit(0);                                                                                                       
                }                                                                                                                           
                catch (NoSuchElementException exception) {                                                                                   
                    System.out.println ("NoSuchElementException. While reading Line ignored:");                                                           
                    System.out.println (line);                                                                                              
                    //System.exit(0);                                                                                                       
                }    
                line = br_access.readLine();     
            }    
            br_access.close();  
            usrNum = usrid.size();
            //System.out.println(usrlist.size());
            
            int total = inputAccess.size();            
            for (int i=0; i<total; i++){
            	usrIndex = ran.nextInt(total-i);
            	usrID = (usrlist.get(usrIndex)).intValue();
            	usrPos = usrid.indexOf(usrID); //position in user id list, index of profile
            	
            	inputAccess.elementAt(i).userID = usrID;
            	Profile p = prof.elementAt(usrPos);
            	inputAccess.elementAt(i).maxQos = p.maxQos;
            	inputAccess.elementAt(i).maxQod = p.maxQod;
            	inputAccess.elementAt(i).minQos = p.minQos;
            	inputAccess.elementAt(i).minQod = p.minQod;
            	inputAccess.elementAt(i).fresh = p.fresh;
            	inputAccess.elementAt(i).relDeadline = p.relDeadline;
            	usrlist.removeElementAt(usrIndex);
            }
            
/*
            FileOutputStream out = new FileOutputStream(output); ; 
            PrintStream out_p = new PrintStream( out );                                                            

            // dump output file
            for (int i=0; i<total; i++){
            	arrTime = inputAccess.elementAt(i).arrTime;
            	baseData = inputAccess.elementAt(i).baseData;
            	exeTime = inputAccess.elementAt(i).exeTime;
            	usrID = inputAccess.elementAt(i).usrID;
            	usrIndex = usrid.indexOf(usrID);
            	out_p.println (arrTime + "\t" + baseData + "\t" + exeTime + "\t" + usrID + "\t" + prof.elementAt(usrIndex) ); 
            }
            out_p.close();    
*/
        }        

        catch (FileNotFoundException exception){                                                                                            
            System.out.println ("The file " + inputUserProfile + " was not found.");      
            System.exit(0);                                                              
        }
        catch (IOException exception){                                                                                                      
            System.out.println (exception);     
            System.exit(0);                                                                                                  
        }                                                                                                                                   
        
        return usrNum;
    }

}

class Profile{
	
	int usrId;
	int maxQos;
	int relDeadline;
	int minQos;
	int maxQod;
	double fresh; 
	int minQod;
	
	public Profile(int reld, double f, int qos, int qod, int nqos, int nqod){
		relDeadline = reld;
		fresh = f;	
		maxQos = qos;
		maxQod = qod;
		minQos = nqos;
		minQod = nqod;
	}
	public String toString(){
		return relDeadline + "\t" + fresh + "\t" + maxQos + "\t" + maxQod + "\t" + minQos + "\t" + minQod;
		//return maxQos + "\t" + relDeadline + "\t" + maxQod + "\t" + maxUnappliedupdates;
	}

}                                                                                                                                     
