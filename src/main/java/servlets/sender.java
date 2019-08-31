package servlets;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
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
    public static final String DEFAULT_MWCMQS_DOMAIN = "mqs.mwc.mw";
    
    private AsyncCompletion acomp = null;
    private String domain = null;

    public void init()
    {
        acomp = listener.acomp;
        String suggest = getServletConfig().getInitParameter("mwcmqs_domain");
        if(suggest != null)
            domain = suggest;
        else
            domain = DEFAULT_MWCMQS_DOMAIN;
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
                //writing html in the stream
                pw.print("lastSeen: "+lastSeen);
            }
            else
            {
                // it's another server so we need to forward.
                // for now just do a synchronous connection.
                // Maybe move to thread later.
                
                String url = "https://" + target_domain + "/sender?address=" + address;
                URL obj = new URL(url);
                HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

                //add reuqest header
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", "mwcmqs 1.0");
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
                // Send post request
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(message);
                wr.flush();
                wr.close();

                int responseCode = con.getResponseCode();
                System.out.println("Resp code = " + responseCode);
                pw = res.getWriter();
                pw.print("LastSeen: Unknown");
            }

            log.log(Level.INFO, "sender");
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally
        {
            try { pw.close(); } catch(Exception ign) {}
        }
    }
}
