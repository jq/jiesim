import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.StringTokenizer;

class UserProfileGenerator {                                                                                                               
                                                                                                                                            
    //public String userProfile;
    
    public UserProfileGenerator(String userConfigFile, String userProfile) {                                                                                                

    	String outFile = userProfile;
    	
    	Random ran = new Random();                                                                                          
                                                                                                                                            
        int userId, userIdMin, userIdMax;
        int maxQos, maxQosMin, maxQosMax;
        int relDL, relDLMin, relDLMax;
        double negQos, minQos; 
        int maxQod, maxQodMin, maxQodMax;
        double maxF, maxFMin, maxFMax;
        double negQod, minQod;
        int userNum, queryNum;
        
        String user, qos, deadline, nqos, qod, fresh, nqod;
        String line;                                                                                                                        
        StringTokenizer line_tokenizer;  
        
        try                                                                                                                                 
        {                                                                                                                                   
            FileReader frConfig = new FileReader (userConfigFile);                                                                             
            BufferedReader brConfig = new BufferedReader (frConfig);    
            FileOutputStream out = new FileOutputStream(outFile); ; 
            PrintStream out_p = new PrintStream( out ); 
			
            line = brConfig.readLine();                                                                                                    
            
//          skip comment lines and empty lines
        	while (line.startsWith("#") || line.equals(""))
        	{
        		line = brConfig.readLine();
//            	continue;
        	}
        	
            while (line != null)                                                                                                            
            {                                                                                                                               
            	
            	try                                                                                                                         
                { 
            		//NEW CONFIG:  RAN	100-100	13334	0-119999	$5-5	100-100  -0%	$5-5	0.5-0.8	-0%	

            		line_tokenizer = new StringTokenizer (line);
	            	//remove class
	            	line_tokenizer.nextToken();
	            	
	            	user = line_tokenizer.nextToken();
	            	
	            	queryNum = Integer.parseInt(line_tokenizer.nextToken());
	            	//skip ID range
	            	line_tokenizer.nextToken();                                                                                                                      
                	                                                                             
                    qos = line_tokenizer.nextToken(); 
                    deadline = line_tokenizer.nextToken(); 
                    nqos = line_tokenizer.nextToken(); 
                    qod = line_tokenizer.nextToken(); 
                    fresh = line_tokenizer.nextToken(); 
                    nqod = line_tokenizer.nextToken(); 
                    
                    
                    /*
                     * all fields: assume range is (min, max)
                    */
                    
                    userIdMin = Integer.parseInt(user.substring( 0, user.indexOf('-')));
                    userIdMax = Integer.parseInt(user.substring(user.indexOf('-')+1, user.length()));
                    userNum = userIdMax - userIdMin + 1;
                    
                    maxQosMin = Integer.parseInt(qos.substring( 1, qos.indexOf('-')));
                    maxQosMax = Integer.parseInt(qos.substring(qos.indexOf('-')+1, qos.length()));
                    
                    relDLMin = Integer.parseInt(deadline.substring( 0, deadline.indexOf('-')));
                    relDLMax = Integer.parseInt(deadline.substring(deadline.indexOf('-')+1, deadline.length()));
                    
                    negQos = Double.parseDouble(nqos.substring( 1, nqos.length()-1))/100.0; //get positive part
                                                                                                                                        
                    maxQodMin = Integer.parseInt(qod.substring( 1, qod.indexOf('-')));
                    maxQodMax = Integer.parseInt(qod.substring(qod.indexOf('-')+1, qod.length()));

                    maxFMin = Double.parseDouble(fresh.substring( 0, fresh.indexOf('-')));
                    maxFMax = Double.parseDouble(fresh.substring(fresh.indexOf('-')+1, fresh.length()));
                    
                    negQod = Double.parseDouble(nqod.substring( 1, nqod.length()-1))/100.0;//get positive part
                    
                    queryNum = queryNum/userNum;
                    
                    for (int i=userIdMin; i<=userIdMax; i++){
                    	userId = i;
                    	
                    	if (maxQosMax == maxQosMin){
                    		maxQos = maxQosMin;
                    	}else
                    		maxQos = maxQosMin + ran.nextInt(maxQosMax - maxQosMin +1);
                    	
                    	if (relDLMin == relDLMax){
                    		relDL = relDLMin;
                    	}else
                    		relDL = relDLMin + ran.nextInt(relDLMax - relDLMin +1);
                    	
                    	minQos = maxQos * negQos; //negQos is positive, so as minQos
                    	minQos = (int)(minQos *100) / -100.0; //change back to negative
                    	
                    	if (maxQodMin == maxQodMax){
                    		maxQod = maxQodMin;
                    	}else
                    		maxQod = maxQodMin + ran.nextInt(maxQodMax - maxQodMin +1);
                    	
                    	if(maxFMin == maxFMax){
                    		maxF = maxFMin;
                    	}else {
                    		int range = (int)((maxFMax - maxFMin)*10 + 1);
                    		maxF = maxFMin + ran.nextInt(range)*0.1;
                    	}
                    	
                    	minQod = maxQod * negQod; //negQod is negative, so as minQod	
                    	minQod = (int)(minQod *100) / -100.0;
                    	
                    	
                    	out_p.println (userId + "\t" + queryNum + "\t" + maxQos + "\t" + relDL + "\t" + minQos + "\t" + maxQod + "\t" + maxF + "\t" + minQod);
                    }
                     
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
                line = brConfig.readLine();     
            }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    
            brConfig.close();                                                                                                              
            out_p.close();                                                                                                                                    
        }                                                                                                                                   
        catch (FileNotFoundException exception){                                                                                            
            System.out.println ("The file " + userConfigFile + " was not found.");                                                              
        }                                                                                                                                   
        catch (IOException exception){                                                                                                      
            System.out.println (exception);                                                                                                 
        }                                                                                                                                   
                                                                                                                                           
    }     
    
    public static void main(String[] args) throws IOException{
		new UserProfileGenerator("userConfig.txt", "userProfile.txt");
	}
}   