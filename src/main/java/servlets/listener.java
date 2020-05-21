package servlets;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.AsyncCompletion;
import util.AsyncRequest;

public class listener extends HttpServlet
{
    public static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(listener.class);
    

    public static AsyncCompletion acomp = null;
    public static String mwc713Script = null;
    public static String mwc713DecryptScript = null;
    public static String httpSendAddress = null;

    
    public void init()
    {
        String line;
        BufferedReader buf = null;
        acomp = new AsyncCompletion();
        mwc713Script = getServletConfig().getInitParameter("mwc713Script");
        mwc713DecryptScript = getServletConfig().getInitParameter("mwc713decryptScript");

        
        ProcessBuilder pb = new ProcessBuilder(
                mwc713Script,
                "''",
                "");
log.error("Init listener");
        try
        {
            Process proc = pb.start();
            buf = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));

            String address = null;
            while((line=buf.readLine()) != null)
            {
                if(line.startsWith("Your mwcmqs address: "))
                    address = line.substring(line.indexOf(":") + 2);
            }
            httpSendAddress = address;
            log.error("Address="+address);
        } catch(Exception err)
        {
            log.error("exception", err);
        }
        finally
        {
            try { buf.close(); } catch(Exception ign) {}
        }
        
    }
    
    public void doGet(HttpServletRequest req, HttpServletResponse res)
    {
        log.info("listener: " + req);

        try
        {
            res.setContentType("text/html");//setting the content type

            String address = req.getParameter("address");
            acomp.updateLastSeenTime(address);
            
            long listenerTime = 0;
            
            try
            {
                listenerTime = Long.parseLong(req.getParameter("time_now"));
            } catch(Exception err) {
                // not specified
            }
            
            // note: we check current time because since they get time from us,
            // anyone posting a higher time than us is being malicious.
            if(!acomp.isMostRecentAndSet(address, listenerTime, acomp) ||
               listenerTime > System.currentTimeMillis())
            {
                // a newer client has logged in
                // tell this client to exit.
                OutputStream os = res.getOutputStream();
                String closeMessage = "closenewlogin";
                
                os.write(("messagelist: 1\n").getBytes());
                os.write(("message[" + closeMessage.length() + "]: " +
                            closeMessage + "\n").getBytes());
                os.flush();
            }
            else
            {
                // create an async request
                AsyncContext ac = req.startAsync();
                ac.setTimeout(1000*1000);
                
                int delCount = -1;
                try
                {
                    delCount = Integer.parseInt(req.getParameter("delcount"));
                } catch(Exception err) {
                }
                
                String delTo = req.getParameter("delTo");

                boolean first = "true".equalsIgnoreCase(req.getParameter("first"));
                
                acomp.add(new AsyncRequest(ac,
                        address,
                        res.getOutputStream(),
                        delCount,
                        delTo,
                        listenerTime,
                        first));
            }
        }
        catch(Exception err)
        {
            log.error("listener servet generated exception", err);
        }
    }
}
