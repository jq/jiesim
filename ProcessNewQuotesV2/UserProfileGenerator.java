/*                                                                                                                                          
 * File: UserProfileGenerator.java                                                                                                                      
 * Author: Jie Xu
 * Desc: generate user profile from a user config file
 * Created: Oct. 24, 2006
 *                                                       
 */                                                                                                                                         
                                                                                                                                            
import java.io.*;                                                                                                                           
import java.util.*;                                                                                                                       
                                                                                                       
                                                                                                                                            
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
        int maxUU, maxUUMin, maxUUMax;
        double negQod, minQod;
        int userNum, queryNum;
        
        String user, qos, deadline, nqos, qod, uu, nqod;
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
        	while (line.startsWith("#") || (line == ""))
        	{
        		line = brConfig.readLine();
//            	continue;
        	}
        	
            while (line != null)                                                                                                            
            {                                                                                                                               
            	
            	try                                                                                                                         
                { 
	            	//SAMPLE LINE: class1  1-4     $20-30  100-200 -10%    $10-15  0-4     -0%	1000
            		//NEW CONFIG:  RAN	100-100	13334	0-119999	$5-5	100-100  -0%	$5-5	1-1	-0%	

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
                    uu = line_tokenizer.nextToken(); 
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

                    maxUUMin = Integer.parseInt(uu.substring( 0, uu.indexOf('-')));
                    maxUUMax = Integer.parseInt(uu.substring(uu.indexOf('-')+1, uu.length()));
                    
                    negQod = Double.parseDouble(nqod.substring( 1, nqod.length()-1))/100.0;//get positive part
                    
                    queryNum = queryNum/userNum;
                    
                    for (int i=userIdMin; i<=userIdMax; i++){
                    	userId = i;
                    	
                    	if (maxQosMax == maxQosMin){
                    		maxQos = maxQosMin;
                    	}else
                    		maxQos = maxQosMin + ran.nextInt(maxQosMax - maxQosMin);
                    	
                    	if (relDLMin == relDLMax){
                    		relDL = relDLMin;
                    	}else
                    		relDL = relDLMin + ran.nextInt(relDLMax - relDLMin);
                    	
                    	minQos = maxQos * negQos; //negQos is positive, so as minQos
                    	minQos = (int)(minQos *100) / -100.0; //change back to negative
                    	
                    	if (maxQodMin == maxQodMax){
                    		maxQod = maxQodMin;
                    	}else
                    		maxQod = maxQodMin + ran.nextInt(maxQodMax - maxQodMin);
                    	
                    	if(maxUUMin == maxUUMax){
                    		maxUU = maxUUMin;
                    	}else
                    		maxUU = maxUUMin + ran.nextInt(maxUUMax - maxUUMin);
                    	
                    	minQod = maxQod * negQod; //negQod is negative, so as minQod	
                    	minQod = (int)(minQod *100) / -100.0;
                    	
                    	
                    	out_p.println (userId + "\t" + queryNum + "\t" + maxQos + "\t" + relDL + "\t" + minQos + "\t" + maxQod + "\t" + maxUU + "\t" + minQod);
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
}                                                                                                                                           
                                                                                                                                     
