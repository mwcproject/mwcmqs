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
        log.info("got a request: " + req);
        try
        {
            res.setContentType("text/html");//setting the content type

            // create an async request
            AsyncContext ac = req.startAsync();
            ac.setTimeout(1000*1000);
            String address = req.getParameter("address");
            
            int delCount = -1;
            try
            {
                Integer.parseInt(req.getParameter("delcount"));
            } catch(Exception err) {
                
            }

            acomp.add(new AsyncRequest(ac,
                    address,
                    res.getOutputStream(),
                    delCount));
        }
        catch(Exception err)
        {
            log.log(Level.SEVERE, "listener servet generated exception", err);
        }
    }
}
