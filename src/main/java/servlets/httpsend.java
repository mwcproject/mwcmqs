package servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import util.AsyncCompletion;

public class httpsend extends HttpServlet {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private AsyncCompletion acomp = null;
    
    private static Logger log = Logger.getLogger(httpsend.class);
    
    public void init()
    {
        acomp = listener.acomp;
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res)
    {
        String address = req.getParameter("address");
        if(address != null)
        {
            int end = address.indexOf('/');
            if(end>=0)
                address = address.substring(0, end);
        }

        PrintWriter pw = null;
        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new InputStreamReader(req.getInputStream()));
            String line;
            log.error("address="+address);

            pw = new PrintWriter(res.getOutputStream());

            if(address != null && address.contains("@"))
            {
                pw.println("{\n" + 
                        "  \"id\": 1,\n" + 
                        "  \"jsonrpc\": \"2.0\",\n" + 
                        "  \"result\": {\n" + 
                        "    \"Err\": {\n" + 
                        "      \"AddressFormatError\": \"Only local addresses supported.\",\n" + 
                        "    }\n" + 
                        "  }\n" +
                        "}");
                pw.flush();
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            while((line=buf.readLine()) != null)
                sb.append(line);

            JSONObject obj = new JSONObject(sb.toString());
            if("check_version".equalsIgnoreCase(obj.getString("method")))
            {
                log.error("got a checkversion");
                pw.println("{\n" + 
                        "  \"id\": 1,\n" + 
                        "  \"jsonrpc\": \"2.0\",\n" + 
                        "  \"result\": {\n" + 
                        "    \"Ok\": {\n" + 
                        "      \"foreign_api_version\": 2,\n" + 
                        "      \"supported_slate_versions\": [\n" + 
                        "        \"V3\",\n" + 
                        "        \"V2\"\n" + 
                        "      ]\n" + 
                        "    }\n" + 
                        "  }\n" +
                        "}");
                pw.flush();
            } else {
                log.error("got a non check");
                
                // we pass to another thread so start async
                AsyncContext ac = req.startAsync();
                ac.setTimeout(60*1000);
                
                acomp.send(address, sb.toString());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try { pw.close(); } catch(Exception ign) {}
            try { buf.close(); } catch(Exception ign) {}
        }
    }
}
