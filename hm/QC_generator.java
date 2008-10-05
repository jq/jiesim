package hm;

/*
 * QC_generator.java
 *
 * Created on 13 November 2005, 19:10
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

/*
 * @author Huiming Qu
 */
import java.io.*;
import java.util.Random;
public class QC_generator {

    public QC_generator() {
    }
    public static void main(String[] args) {
                FileOutputStream out; // declare a file output object
                PrintStream p; // declare a print stream object

                int min = 1;
                int max = 1000;
                int qos, i=0;
                int num_trans = 44501;
                String filename = new String("qc.trace_"+ max);
                Random ran = new Random();
                try
                {
                    out = new FileOutputStream(filename);
                    p = new PrintStream( out );

                    p.println ("max_qos");
                    while(i < num_trans){
                        qos = ran.nextInt(max) + 1; // the generated random number ranging from 1 to max. (1, max-1) + 1
                        p.println (qos);
                        i++;
                    }
                    p.close();
                }
                catch (Exception e)
                {
                        System.err.println ("Error writing to file");
                }

    }

}
