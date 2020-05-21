package servlets;

import java.io.BufferedReader;
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
    private String mwc713Script = null;
    
    private static Logger log = Logger.getLogger(httpsend.class);
    
    public void init()
    {
        acomp = listener.acomp;
        mwc713Script = getServletConfig().getInitParameter("mwc713Script");
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res)
    {
        String domain = sender.domain;
        String address = req.getParameter("address");
        String address_pre = address;

        if(!sender.DEFAULT_MWCMQS_DOMAIN.equals(domain))
        {
            address += "@" + domain;
        }
        
        if(address != null)
        {
            int end = address.indexOf('/');
            if(end>=0)
                address = address.substring(0, end);
        }

        if(!sender.DEFAULT_MWCMQS_DOMAIN.equals(domain))
            address += "@" + domain;


        PrintWriter pw = null;
        BufferedReader buf = null, buf2 = null;
        try {
            buf = new BufferedReader(new InputStreamReader(req.getInputStream()));
            String line;
            log.error("address="+address);

            pw = new PrintWriter(res.getOutputStream());

            if(address_pre != null && address_pre.contains("@"))
            {
                pw.println("{\n" + 
                        "  \"id\": 1,\n" + 
                        "  \"jsonrpc\": \"2.0\",\n" + 
                        "  \"result\": {\n" + 
                        "    \"Err\": {\n" + 
                        "      \"AddressFormatError\": \"Only local addresses supported.\"\n" + 
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
                // we pass to another thread so start async
                AsyncContext ac = req.startAsync();
                ac.setTimeout(60*1000);
                
                // get encrypted slate
                ProcessBuilder pb = new ProcessBuilder(
                        mwc713Script,
                        "'" + sb.toString() + "'",
                        address_pre);

                boolean success = false;

                Process proc = pb.start();
                buf2 = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()));

                while((line=buf2.readLine()) != null)
                {
                    System.out.println("line="+line);
                }
                
                acomp.send(address, sb.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try { buf.close(); } catch(Exception ign) {}
            try { buf2.close(); } catch(Exception ign) {}
        }
    }
}
