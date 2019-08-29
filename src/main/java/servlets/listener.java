package servlets;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.AsyncCompletion;
import util.AsyncRequest;

public class listener extends HttpServlet
{
    public static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger("mwcmq2");

    public static AsyncCompletion acomp = null;
    
    public void init()
    {
        acomp = new AsyncCompletion(); 
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse res)
    {
System.out.println("got a req: " + req);
        try
        {
            res.setContentType("text/html");//setting the content type

            // create an async request
            AsyncContext ac = req.startAsync();
            ac.setTimeout(1000*1000);
            String address = req.getParameter("address");
            acomp.add(new AsyncRequest(ac,
                    address,
                    res.getOutputStream()));
            log.log(Level.INFO, "listener");
        }
        catch(Exception err)
        {
            log.log(Level.SEVERE, "listener servet generated exception", err);
        }
    }
}
