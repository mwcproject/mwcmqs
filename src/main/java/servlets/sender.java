package servlets;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import util.AsyncCompletion;

public class sender extends HttpServlet
{
    /**
     * 
     */
    public static final long serialVersionUID = 1L;
    public static final String DEFAULT_MWCMQS_DOMAIN = "mqs.mwc.mw";
    
    private AsyncCompletion acomp = null;
    public static String domain = null;
    
    private class HttpForwardRequest 
    {
        HttpForwardRequest(String url,
                           String message,
                           OutputStream os,
                           AsyncContext ac)
        {
            this.url = url;
            this.message = message;
            this.os = os;
            this.ac = ac;
        }

        private String url = null;
        private String message = null;
        private OutputStream os = null;
        private AsyncContext ac = null;
    }
    
    private List <HttpForwardRequest> queue = new LinkedList<HttpForwardRequest>();
    
    private class HttpForwarder extends Thread
    {
        HttpForwarder()
        {
            setDaemon(true);
            setName("Httpforwarder Thread");
        }
        
        public void run()
        {
            List <HttpForwardRequest> localList =
                    new LinkedList<HttpForwardRequest>();
            while(true)
            {
                synchronized(queue)
                {
                    while(queue.isEmpty())
                        try { queue.wait(); } catch(Exception err) {}

                    localList.clear();
                    localList.addAll(queue);
                    queue.removeAll(queue);
                }
                
                for(Iterator<HttpForwardRequest> itt=localList.iterator();
                    itt.hasNext();)
                {
                    HttpForwardRequest hfr = itt.next();
                    
                    HttpsURLConnection con = null;
                    DataOutputStream wr = null;
                    PrintWriter pw = null;
                    BufferedReader buf = null;
                    String line = null;
                    AsyncContext ac = hfr.ac;
                    
                    try {

                        URL obj = new URL(hfr.url);
                        con = (HttpsURLConnection) obj.openConnection();
                        con.setConnectTimeout(3000);
                        con.setReadTimeout(3000);

                        con.setRequestMethod("POST");
                        con.setRequestProperty("User-Agent", "mwcmqs 1.0");
                        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                        con.setDoOutput(true);
                        wr = new DataOutputStream(con.getOutputStream());
                        wr.writeBytes(hfr.message);
                        wr.flush();
                        wr.close();

                        buf = new BufferedReader(new InputStreamReader(
                                con.getInputStream()));

                        StringBuilder resp = new StringBuilder();
                        while((line=buf.readLine()) != null)
                            resp.append(line);

                        pw = new PrintWriter(hfr.os);
                        pw.print(resp.toString());

                    }
                    catch(Exception err)
                    {
                        err.printStackTrace();
                    }
                    finally
                    {
                        try { pw.close(); } catch(Exception ign) {}
                        try { wr.close(); } catch(Exception ign) {}
                        try { buf.close(); } catch(Exception ign) {}
                        try { con.disconnect(); } catch(Exception ign) {}
                        try { ac.complete(); } catch(Exception ign) {}
                    }
                }
            }
        }
    }

    public void init()
    {
        acomp = listener.acomp;
        String suggest = getServletConfig().getInitParameter("mwcmqs_domain");
        if(suggest != null)
            domain = suggest;
        else
            domain = DEFAULT_MWCMQS_DOMAIN;
        
        // start async thread.
        new HttpForwarder().start();
    }

    private static Logger log = LoggerFactory.getLogger(sender.class);

    public void doPost(HttpServletRequest req, HttpServletResponse res)
    {
        log.info("sender: " + req);

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
            
            log.error("message="+message);
            
            boolean is_target_domain = true;
            String target_domain = DEFAULT_MWCMQS_DOMAIN;
            if(address.indexOf("@")>=0) {
                target_domain = address.substring(address.indexOf("@")+1);
                if(!target_domain.equalsIgnoreCase(domain))
                    is_target_domain = false;
            }
            else if(!domain.equalsIgnoreCase(DEFAULT_MWCMQS_DOMAIN))
            {
                is_target_domain = false;
            }

            if(is_target_domain)
            {
                long lastSeen = System.currentTimeMillis() -
                        acomp.getLastSeenTime(address);
                acomp.send(address, message);
                pw = res.getWriter();
                //writing lastSeen in the stream
                pw.print("lastSeen: "+lastSeen);
            }
            else
            {
                
                // we pass to another thread so start async
                AsyncContext ac = req.startAsync();
                ac.setTimeout(1000*1000);
                
                String url = "https://" +
                             target_domain +
                             "/sender?address=" +
                             address.replace("@", "%40");
                
                HttpForwardRequest hfr = new HttpForwardRequest(url,
                                                     message,
                                                     res.getOutputStream(),
                                                     ac);

                synchronized(queue)
                {
                    queue.add(hfr);
                    queue.notify();
                }
                
                log.info("Forwarding: " + url);
            }

        }
        catch (Exception err)
        {
            log.error("sender servet generated exception", err);
        }
        finally
        {
            try { pw.close(); } catch(Exception ign) {}
        }
    }
}
