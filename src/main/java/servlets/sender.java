package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.AsyncCompletion;

public class sender extends HttpServlet
{
    /**
     * 
     */
    public static final long serialVersionUID = 1L;
    
    private AsyncCompletion acomp = null;
    
    public void init()
    {
        acomp = listener.acomp;
    }
    
    private static Logger log = Logger.getLogger("mwcmq2");

    public void doPost(HttpServletRequest req, HttpServletResponse res)
    {
        StringBuilder jb = new StringBuilder();
        String line = null;
        try {
          BufferedReader reader = req.getReader();
          while ((line = reader.readLine()) != null)
            jb.append(line);
        } catch (Exception e) { /*report an error*/ }

        res.setContentType("text/html");//setting the content type
        PrintWriter pw = null;
        try
        {
            String address = req.getParameter("address");
            String message = jb.toString();
            System.out.println("message: " + message);
            acomp.send(address, message);
            pw = res.getWriter();

            //writing html in the stream
            pw.print("sender complete");
            
            log.log(Level.INFO, "sender");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            try { pw.close(); } catch(Exception ign) {}
        }
    }
}
