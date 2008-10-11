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
	 * @return: 
	 */
    public void addUser(ArrayList<Access> inputAccess, String inputUserProfile, String output){                                                                                                
        Random ran = new Random();                                                                                          
         
        usrid.clear();
        usrlist.clear();
        int usrIndex = -1, usrNum = -1, usrPos = -1;
        int usrID, queryNum;
        int maxQos, minQos, relDeadline, maxQod, minQod;
        double fresh;
        ArrayList<Profile> prof = new ArrayList();
                
        String line;                                                                                                                               
        StringTokenizer line_tokenizer; 
        
        //read inputUserProfile and put into an array, get usrNum, build ArrayList of id and list;
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
                	 prof.add(new Profile(relDeadline, fresh, maxQos, maxQod, minQos, minQod));  
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
            Access a;
            Profile p;
            for (int i=0; i<total; i++){
            	usrIndex = ran.nextInt(total-i);
            	usrID = (usrlist.get(usrIndex)).intValue();
            	usrPos = usrid.indexOf(usrID); //position in user id list, index of profile
            	
            	p = prof.get(usrPos);
            	a = inputAccess.get(i);
            	a.userID = usrID;
            	a.maxQos = p.maxQos;
            	a.maxQod = p.maxQod;
            	a.minQos = p.minQos;
            	a.minQod = p.minQod;
            	a.fresh = p.fresh;
            	a.relDeadline = p.relDeadline;
            	usrlist.remove(usrIndex);
            }
            
            FileOutputStream out = new FileOutputStream(output); ; 
            PrintStream out_p = new PrintStream( out );                                                            

            // dump output file
            int len;
            StringBuilder b = new StringBuilder(1024);
            for (int i=0; i<total; i++){
            	a = inputAccess.get(i);
            	//output format: queryID|data1,data2,...,datan|arrTime|userID|maxQos|relDeadline|maxQod|fresh
            	b.append(a.queryID); b.append('|');
            	len = a.d.length;
            	b.append(a.d[0]);
            	for (int j=1; j<len; j++) {
            		b.append(',');
            		b.append(a.d[j]);
            	}
            	b.append('|');
            	b.append(a.timestamp); b.append('|');
            	b.append(a.userID); b.append('|');
            	b.append(a.maxQos); b.append('|');
            	b.append(a.relDeadline); b.append('|');
            	b.append(a.maxQod); b.append('|');
            	b.append(a.fresh); b.append('\n');
            	
            	out_p.print (b.toString()); 
            	b.setLength(0);
            }
            out_p.close();    

        }        

        catch (FileNotFoundException exception){                                                                                            
            System.out.println ("The file " + inputUserProfile + " was not found.");      
            System.exit(0);                                                              
        }
        catch (IOException exception){                                                                                                      
            System.out.println (exception);     
            System.exit(0);                                                                                                  
        }                                                                                                                                   
        
        //return usrNum;
    }
    
    public static void main(String[] args) throws IOException{
		//new UserProfileGenerator("userConfig.txt", "userProfile.txt");
		//AddUser(a, "userProfile.txt", "query.txt");
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
		//return relDeadline + "\t" + fresh + "\t" + maxQos + "\t" + maxQod + "\t" + minQos + "\t" + minQod;
		return maxQos + "\t" + relDeadline + "\t" + maxQod + "\t" + fresh;
	}

}                                                                                                                                     
