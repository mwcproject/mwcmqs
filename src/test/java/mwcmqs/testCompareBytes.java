package mwcmqs;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class testCompareBytes
{
    public static void run(String [] args) throws IOException
    {
        System.out.println("comparing bytes" + args[0] + " to " + args[1]);
        InputStream is1 = new FileInputStream(args[0]);
        InputStream is2 = new FileInputStream(args[1]);
        byte [] b1 = new byte[1024];
        byte [] b2 = new byte[1024];
        
        while(true)
        {
            int len1 = is1.read(b1);
            int len2 = is2.read(b2);
            
            if(len1 == len2 && len1 == 1024)
                for(int i=0; i<1024; i++)
                {
                    if(b1[i] != b2[i])
                    {
                        System.out.println("ne " + i + "b1="+(int)b1[i] + ",b2[i]="+b2[i]);
                        System.exit(0);
                    }
                }
            
            System.out.println("len1="+len1+",len2="+len2);
            if(len1 <= 0 || len2 <= 0)
                break;
        }
    }
    
    public static void main(String [] args)
    {
        try
        {
            run(args);
        } catch(Exception err) {
            err.printStackTrace();
        }
    }
}
