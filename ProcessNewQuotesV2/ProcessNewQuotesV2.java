/*
 * Jie Xu @ Nov. 9, 2006
 */
import java.util.StringTokenizer;                                                                                                           
import java.util.Vector;
import java.io.*;                                                                                                                           
import java.util.*; 
                                                                                                                                            

public class ProcessNewQuotesV2 {                                                                                                               
                                                                                                                                                                                                                                                                                       
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {                                                                                                
		int argc = args.length;
        if (argc < 3){
            System.out.println("java ProcessNewQuotesV2 inter_access userConfig output");          
            System.exit(-1);     
        }     
        
        String inFile = args[0] ;
        String outFile = args[2];
        String userConfig = args[1];
        
        int arrTime, baseData = -1, exeTime;  
        String line;                                                                                                                        
        StringTokenizer line_tokenizer;                                                                                     
        
        Vector<Access> accessVec = new Vector<Access>();
        
        try                                                                                                                                 
        {                                                                                                                                   
            FileReader fr_access = new FileReader (inFile);                                                                             
            BufferedReader br_access = new BufferedReader (fr_access);  
            //FileOutputStream out = new FileOutputStream(outFile); ; 
			//PrintStream out_p = new PrintStream( out );                                                                     
            //line = br_access.readLine();    //ignore the first line notes                                                                   
            
            line = br_access.readLine();              
                                                                                                  
            while (line != null)                                                                                                            
            {                                                                                                                               
                //arrTime baseData exeTime
                                                                                
                line_tokenizer = new StringTokenizer (line);                                                                                
                                                                                          
                try                                                                                                                         
                {                                                                                                                           
                	arrTime = Integer.parseInt(line_tokenizer.nextToken());
                	baseData = Integer.parseInt(line_tokenizer.nextToken());
                	exeTime = Integer.parseInt(line_tokenizer.nextToken());
                	
                    accessVec.add(new Access(arrTime, baseData, exeTime)); 
                                                                               
                }                                                                                                                           
                catch (NumberFormatException exception)                                                                                     
                {                                                                                                                           
                    System.out.println ("FormatException. Line ignored:");                                                                  
                    System.out.println (line);                                                                                              
                    //System.exit(0);                                                                                                       
                }                                                                                                                           
                catch (NoSuchElementException exception)                                                                                    
                {                                                                                                                           
                    System.out.println ("NoSuchElementException. Line ignored:");                                                           
                    System.out.println (line);                                                                                              
                    //System.exit(0);                                                                                                       
                }                                                                                                                           
                line = br_access.readLine();
                //System.out.println(stock.size());
                                                                                                                                            
            }                                                                                                                               
                                                                                                                                            
            br_access.close();                                                                                                              
//            out_p.close();
            
            
            //generate user profile from parser.userConfig
            String userProfile = "userProfile.txt";
            new UserProfileGenerator(userConfig, userProfile);
            
            /*
             * add user contract and execution time to each access record, print final output file
             * 
             * AddUserContract(Vector<Access> inputAccess, String inputUserProfile, 
             * String output, int exeTimeDist, int minExeTime, int maxExeTime)
             *
             */
            AddUser userAcc = new AddUser(accessVec, userProfile);
            
            new AddUserContract( userAcc.usrAccessVec, userProfile, outFile);

        }                                                                                                                                   
        catch (FileNotFoundException exception){                                                                                            
            System.out.println ("The file " + inFile + " was not found.");                                                              
        }                                                                                                                                   
        catch (IOException exception){                                                                                                      
            System.out.println (exception);                                                                                                 
        } 
        
                                                                                                                                           
    }                                                                                                                                       
                                                                                                                                                                                                                                                                                        
}

class Access{
	public int arrTime;
	public int baseData;
	public int usrID = -1;
	public int exeTime;
	
	public Access( int arrT, int baseD, int exeT){
		arrTime = arrT;
		baseData = baseD;
		exeTime = exeT;
	}
}

class ArrTimeComparator implements java.util.Comparator {
    public int compare(Object o1, Object o2) {
         Access le1 = (Access) o1;
         Access le2 = (Access) o2;
         return le1.arrTime - le2.arrTime;
    }
}

                                                                                                                                     